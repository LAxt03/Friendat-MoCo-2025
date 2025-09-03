package com.friendat.ui.composables


import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.friendat.ui.theme.Primary
import com.friendat.ui.theme.Sekundary


@Composable
fun TopBar(periodLabels: MutableList<String>, functions: MutableList<()->Unit>) {
    var periodIndex by remember {
        mutableStateOf(0)
    }


    TabRow(
        selectedTabIndex = periodIndex,
        indicator = { tabPositions ->
            if (periodIndex < tabPositions.size) {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[periodIndex]),
                    shape = RoundedCornerShape(
                        topStart = 3.dp,
                        topEnd = 3.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 0.dp
                    ),
                    color = Sekundary
                )
            }
        },
        containerColor = Primary
    ) {
        periodLabels.forEachIndexed { index, title ->
            Tab(
                selected = periodIndex == index,
                onClick = {
                    periodIndex = index
                },
                text = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun TopBarPrev(){
    TopBar(mutableListOf<String>("Locations","Friends"),mutableListOf<()->Unit>({},{}))
}