import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {region as regionV1, EventContext as EventContextV1}
  from "firebase-functions/v1";


admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

const USER_STATUS_COLLECTION = "user_status";
const USERS_COLLECTION = "users";
const FRIENDSHIPS_COLLECTION = "friendships";
const FRIENDSHIP_STATUS_ACCEPTED = "ACCEPTED";

export const notifyFriendsOnUserStatusChange = regionV1("europe-west1")
  .firestore.document(`${USER_STATUS_COLLECTION}/{statusDocUserId}`)
  .onWrite(
    async (
      change: functions.Change<functions.firestore.DocumentSnapshot>,
      context: EventContextV1,
    ) => {
      const statusDocUserId = context.params.statusDocUserId;
      const newStatusData = change.after.data();
      const oldStatusData = change.before.data();

      if (!change.after.exists || !newStatusData) {
        console.log(
          `Status document for user ${statusDocUserId} deleted ` +
            "or has no data. No action.",
        );
        return null;
      }

      if (
        oldStatusData &&
        newStatusData.currentLocationName ===
          oldStatusData.currentLocationName &&
        newStatusData.currentBssid === oldStatusData.currentBssid &&
        newStatusData.isOnlineAtLocation ===
          oldStatusData.isOnlineAtLocation &&
        newStatusData.currentIconId === oldStatusData.currentIconId &&
        newStatusData.currentColorHex === oldStatusData.currentColorHex
      ) {
        console.log(
          `User status for ${statusDocUserId} relevant content ` +
            "did not change. No notification.",
        );
        return null;
      }

      console.log(
        `Status changed/written for user: ${statusDocUserId}, ` + "new data:",
        newStatusData,
      );

      const friendsIds: string[] = [];
      try {
        const friendshipsQuery = db
          .collection(FRIENDSHIPS_COLLECTION)
          .where("participants", "array-contains", statusDocUserId)
          .where("status", "==", FRIENDSHIP_STATUS_ACCEPTED);

        const querySnapshot = await friendshipsQuery.get();
        const processedFriendshipIds = new Set<string>();

        querySnapshot.forEach((doc) => {
          if (processedFriendshipIds.has(doc.id)) return;
          const friendship = doc.data();
          if (
            friendship &&
            friendship.participants &&
            Array.isArray(friendship.participants)
          ) {
            const otherParticipant = (
              friendship.participants as string[]
            ).find((pId) => pId !== statusDocUserId);
            if (otherParticipant) {
              friendsIds.push(otherParticipant);
              processedFriendshipIds.add(doc.id);
            }
          }
        });

        if (friendsIds.length === 0) {
          console.log(
            `User ${statusDocUserId} has no accepted ` +
              "friendships to notify.",
          );
          return null;
        }
        console.log(
          `User ${statusDocUserId} has accepted friends: ` +
            `${friendsIds.join(", ")}`,
        );
      } catch (error) {
        console.error(
          `Error fetching friendships for user ${statusDocUserId}:`,
          error,
        );
        return null;
      }

      const tokensPromises = friendsIds.map(async (friendId) => {
        try {
          const friendUserDoc = await db
            .collection(USERS_COLLECTION)
            .doc(friendId)
            .get();
          const friendUserData = friendUserDoc.data();
          if (friendUserData && friendUserData.fcmToken) {
            return friendUserData.fcmToken as string;
          }
          console.warn(`No FCM token found for friend: ${friendId}`);
          return null;
        } catch (error) {
          console.error(
            `Error fetching FCM token for friend ${friendId}:`,
            error,
          );
          return null;
        }
      });

      const tokens = (await Promise.all(tokensPromises)).filter(
        (token): token is string => token !== null && token !== "",
      );

      if (tokens.length === 0) {
        console.log("No valid FCM tokens found for any friends.");
        return null;
      }

      const payload = {
        data: {
          type: "FRIEND_STATUS_UPDATE",
          updatedUserId: statusDocUserId,
          locationName:
            newStatusData.currentLocationName || "Unknown Location",
          bssid: newStatusData.currentBssid || "N/A",
          isOnline:
            newStatusData.isOnlineAtLocation?.toString() || "false",
          iconId: newStatusData.currentIconId || "default_icon",
          colorHex: newStatusData.currentColorHex || "#CCCCCC",
          timestamp:
            newStatusData.timestamp?.toDate().toISOString() ||
            new Date().toISOString(),
        },
        tokens: tokens,
      };

      try {
        console.log(
          "Sending FCM data message. Payload data:",
          payload.data,
          "To tokens count:",
          tokens.length,
        );
        const response = await messaging.sendEachForMulticast(payload);
        console.log(
          "Successfully sent message to:",
          response.successCount,
          "tokens",
        );

        if (response.failureCount > 0) {
          response.responses.forEach((resp, idx) => {
            if (!resp.success) {
              console.error(
                "Failed to send to token associated with friend " +
                  `index ${idx} (token: ${tokens[idx]}):`,
                resp.error,
              );
              if (
                resp.error &&
                resp.error.code ===
                  "messaging/registration-token-not-registered"
              ) {
                const failedToken = tokens[idx];
                console.log(
                  `Token ${failedToken} is not registered. ` +
                    `Consider removing it for user ${friendsIds[idx]}.`,
                );
              }
            }
          });
        }
        return null;
      } catch (error) {
        console.error(
          "Error sending FCM message via sendEachForMulticast:",
          error,
        );
        return null;
      }
    },
  );

