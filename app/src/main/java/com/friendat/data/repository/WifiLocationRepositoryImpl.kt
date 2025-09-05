package com.friendat.data.repository

import com.friendat.data.model.WifiLocation
import com.friendat.data.sources.local.dao.WifiLocationDao // <-- DAO importieren
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log // Für Logging

private const val WIFI_LOCATIONS_COLLECTION = "userWifiLocations"

@Singleton
class WifiLocationRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val wifiLocationDao: WifiLocationDao // <-- DAO hier injizieren
) : WifiLocationRepository {

    override suspend fun addWifiLocation(wifiLocation: WifiLocation): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User nicht angemeldet."))
            }

            // Firestore erwartet oft, dass das 'id'-Feld nicht Teil des Objekts beim HINZUFÜGEN ist,
            // da Firestore die Dokument-ID selbst generiert.
            // Room hingegen benötigt die ID (oder 0 für autogenerate).
            // Wir müssen hier etwas aufpassen.

            val userId = currentUser.uid
            val locationForFirestore = wifiLocation.copy(
                userId = userId,
                id = "" // Firestore generiert die ID, also setzen wir sie hier vorerst leer
                // oder wir generieren sie clientseitig und verwenden .document(id).set()
            )

            // In Firestore speichern und die generierte ID holen
            val documentReference = firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .add(locationForFirestore) // .add() generiert eine neue ID
                .await()
            Log.d("WifiLocationRepo", "Location added to Firestore with ID: ${documentReference.id}")

            // Die von Firestore generierte ID im WifiLocation-Objekt speichern,
            // falls du sie in Room als Referenz oder Primärschlüssel verwenden möchtest.
            // ODER: Du könntest auch eine eigene UUID clientseitig generieren und
            // diese sowohl für Firestore Document ID als auch für Room ID verwenden.
            // Für den Moment nehmen wir an, Room hat eine eigene AutoGenerate ID.
            val locationForRoom = wifiLocation.copy(
                userId = userId,
                // Wenn Room 'id' als AutoGenerate hat, muss es 0 sein oder nicht gesetzt.
                // Wenn 'id' in WifiLocation von Room auto-generiert wird:
                id = "" // oder 0, je nach deiner WifiLocation @Entity Definition für Room
                // und ob 'id' nullbar ist und einen Defaultwert hat.
                // Sicherer ist, eine Kopie ohne ID zu erstellen, wenn Room autogeneriert.
                // Oder wir müssen die Firestore ID in einem anderen Feld speichern.

                // ANNAHME: Deine WifiLocation-Klasse für Room hat eine auto-generierte id.
                // Wir erstellen eine Kopie für Room und setzen die ID explizit auf 0,
                // damit Room eine neue generiert, oder wir nutzen die Firestore ID.
                // Für jetzt machen wir es einfach und speichern die gleiche Struktur,
                // aber die ID-Handhabung muss klar sein.
                // Idealerweise hat deine WifiLocation-Data-Klasse ein Feld für die Firestore-ID
                // und ein separates für die Room-Primärschlüssel-ID (auto-generiert).
                // Bsp.: firestoreDocumentId: String? = documentReference.id
            )

            // In Room speichern
            // Angenommen, deine `id` in WifiLocation ist der Primärschlüssel für Room und
            // du willst die von Firestore generierte ID NICHT als Primärschlüssel in Room verwenden,
            // sondern Room soll seine eigene ID generieren.
            // Dann muss das `id`-Feld von `locationForRoom` für Room entweder 0 sein (wenn Int/Long)
            // oder leer/null (wenn String und Room es so interpretiert für Auto-Generierung,
            // was bei String-PKs unüblich ist - meist sind String-PKs nicht auto-generiert).

            // EINFACHERE VARIANTE FÜR JETZT:
            // Wir gehen davon aus, dass WifiLocation.id als String in Firestore und Room dient.
            // Wir verwenden die von Firestore generierte ID.
            val finalLocationToSave = wifiLocation.copy(
                userId = userId,
                id = documentReference.id // Firestore ID verwenden
            )
            wifiLocationDao.insert(finalLocationToSave)
            Log.d("WifiLocationRepo", "Location added to Room with ID: ${finalLocationToSave.id}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WifiLocationRepo", "Error in addWifiLocation", e)
            Result.failure(e)
        }
    }

    // getWifiLocationsForCurrentUser() kann erstmal so bleiben,
    // da das ViewModel es für die Anzeige der Liste aus Firestore verwendet.
    // Der Worker verwendet direkt wifiLocationDao.
    override fun getWifiLocationsForCurrentUser(): Flow<Result<List<WifiLocation>>> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("User nicht angemeldet.")))
        }



        val query = firestore.collection(WIFI_LOCATIONS_COLLECTION)
            .whereEqualTo("userId", currentUser.uid)
            // .orderBy("createdAt", Query.Direction.DESCENDING) // Stelle sicher, dass createdAt in WifiLocation existiert
            // Wenn nicht, nach 'name' oder einem anderen Feld sortieren oder Sortierung weglassen
            .orderBy("name", Query.Direction.ASCENDING)


        return query.snapshots().mapNotNull { snapshot ->
            try {
                val locations = snapshot.documents.mapNotNull { document ->
                    // Wichtig: Die Dokument-ID von Firestore in das WifiLocation-Objekt übernehmen
                    document.toObject<WifiLocation>()?.copy(id = document.id)
                }
                Result.success(locations)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getWifiLocationsForCurrentUserFromRoom(): Flow<List<WifiLocation>> {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            // User nicht angemeldet, leeren Flow oder Fehler-Flow zurückgeben
            // Für einen UI-Flow ist ein leerer Flow oft besser als ein Fehler, der die App crasht
            Log.w("WifiLocationRepo", "getWifiLocationsForCurrentUserFromRoom: User not logged in, returning empty flow.")
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val userId = currentUser.uid
        Log.d("WifiLocationRepo", "getWifiLocationsForCurrentUserFromRoom: Fetching from DAO for user $userId")
        return wifiLocationDao.getAllLocationsForUserFlow(userId)
    }

    override suspend fun updateWifiLocation(wifiLocation: WifiLocation): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null || currentUser.uid != wifiLocation.userId) {
                return Result.failure(Exception("User nicht eingeloggt oder falscher User"))
            }
            if (wifiLocation.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Location ID ist leer für Update"))
            }

            // In Firestore aktualisieren
            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .document(wifiLocation.id) // Verwende die übergebene ID
                .set(wifiLocation) // .set() überschreibt das Dokument
                .await()
            Log.d("WifiLocationRepo", "Location updated in Firestore: ${wifiLocation.id}")

            // In Room aktualisieren
            wifiLocationDao.update(wifiLocation) // Annahme: Dao hat eine update-Methode
            Log.d("WifiLocationRepo", "Location updated in Room: ${wifiLocation.id}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WifiLocationRepo", "Error in updateWifiLocation", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteWifiLocation(locationId: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User nicht eingeloggt"))
            }
            if (locationId.isBlank()) {
                return Result.failure(IllegalArgumentException("Location ID ist leer für Delete"))
            }

            // Aus Firestore löschen
            firestore.collection(WIFI_LOCATIONS_COLLECTION)
                .document(locationId)
                .delete()
                .await()
            Log.d("WifiLocationRepo", "Location deleted from Firestore: $locationId")

            // Aus Room löschen
            // Du brauchst eine Methode im DAO, die nach ID löscht, oder du übergibst das Objekt.
            // Wenn du nur die ID hast, brauchst du eventuell das Objekt erst aus Room zu laden, um es zu löschen,
            // oder eine Dao-Methode `deleteById(id: String)`
            // Annahme: Du hast eine delete-Methode im DAO, die ein WifiLocation-Objekt erwartet.
            // Da wir nur die ID haben, ist es besser, eine deleteById-Methode im DAO zu haben.
            // Alternativ, wenn delete das Objekt braucht und die ID der Primärschlüssel ist:
            // Du könntest ein WifiLocation-Objekt nur mit der ID erstellen, wenn das für Room @Delete reicht.
            // Sicherer ist eine spezifische deleteById-Methode.
            // Für jetzt gehen wir davon aus, es gibt eine:
            val locationToDeleteFromRoom = WifiLocation(id = locationId, userId = currentUser.uid, name = "", bssid = "", iconId = "", colorHex = "") // Dummy-Objekt nur mit ID für @Delete
            wifiLocationDao.delete(locationToDeleteFromRoom) // Oder besser: wifiLocationDao.deleteById(locationId, currentUser.uid)
            Log.d("WifiLocationRepo", "Location deleted from Room: $locationId")


            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WifiLocationRepo", "Error in deleteWifiLocation", e)
            Result.failure(e)
        }
    }
}

