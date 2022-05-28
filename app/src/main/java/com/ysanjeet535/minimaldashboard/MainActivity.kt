package com.ysanjeet535.minimaldashboard

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ysanjeet535.minimaldashboard.ui.theme.MinimalDashboardTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                //doing nothing
            }
        }
        this.onBackPressedDispatcher.addCallback(
            this,  // LifecycleOwner
            callback
        )


        setContent {
            MinimalDashboardTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.surface)
                ) {
                    val launchIntent = Intent(Intent.ACTION_MAIN, null)
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                    AppListContent(
                        packageManager = packageManager,
                        intent = launchIntent
                    ) { intent ->
                        if (intent != null) {
                            startActivity(intent)
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun AppListContent(
    packageManager: PackageManager,
    intent: Intent,
    onAppItemClicked: (Intent?) -> Unit
) {
    val list = packageManager.queryIntentActivities(intent, 0)

    Column(modifier = Modifier.padding(32.dp)) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(16.dp)
                .height(440.dp)
                .fillMaxWidth()
        ) {
            itemsIndexed(list) { index, item ->
                AppListItem(
                    item = item,
                    packageManager = packageManager,
                    onAppItemClicked = onAppItemClicked
                )
            }
        }
        val undiscoveredList =
            list.subList(listState.layoutInfo.visibleItemsInfo.size, list.size - 1)
        InvisibleAppItemsTray(appList = undiscoveredList, packageManager = packageManager)
    }
}


@Composable
fun InvisibleAppItemsTray(
    appList: List<ResolveInfo>,
    packageManager: PackageManager
) {
    LazyRow {
        items(appList) { app ->
            Image(
                rememberDrawablePainter(app.loadIcon(packageManager)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp)
            )
        }
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppListItem(
    item: ResolveInfo,
    packageManager: PackageManager,
    onAppItemClicked: (Intent?) -> Unit
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp)
                    .clickable {
                        onAppItemClicked(packageManager.getLaunchIntentForPackage(item.activityInfo.packageName))
                    }
            ) {
                val icon = item.loadIcon(packageManager)
                Image(
                    rememberDrawablePainter(drawable = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .background(color = Color.Transparent)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    text = item.loadLabel(packageManager).toString(),
                    modifier = Modifier,
                    color = Color.White
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(0.9f),
                    text = "When you scroll in LazyColumn, the composables that are no longer visible get removed from the composition tree and when you scroll back to them, they are composed again from scratch. That is why expanded is initialized to false again",
                    color = Color.White
                )
            }
        }
        IconButton(onClick = { isExpanded = !isExpanded }) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White
            )
        }
    }

}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MinimalDashboardTheme {

    }
}