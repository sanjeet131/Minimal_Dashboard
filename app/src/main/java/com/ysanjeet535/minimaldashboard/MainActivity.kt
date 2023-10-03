package com.ysanjeet535.minimaldashboard

import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ysanjeet535.minimaldashboard.bubbles.ControllerService
import com.ysanjeet535.minimaldashboard.ui.theme.MinimalDashboardTheme
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private var usageStats: List<UsageStats>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1)
        }
        startService(Intent(this, ControllerService::class.java))
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        }
        this.onBackPressedDispatcher.addCallback(this, callback)

        setContent {
            MinimalDashboardTheme {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusManager = LocalFocusManager.current
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            keyboardController?.hide()
                            focusManager.clearFocus(true)
                        }
                ) {
                    val launchIntent = remember {
                        Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    usageStats = remember { fetchUsageStats() }
                    AppListContent(
                        packageManager = packageManager,
                        usageStats = usageStats,
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

    private fun fetchUsageStats(): List<UsageStats>? {
        return if (checkForPermission(context = this)) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, -1)
            val usageStatsManager =
                this.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                System.currentTimeMillis()
            )
        } else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            null
        }
    }

    private fun checkForPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == MODE_ALLOWED
    }

}


@Composable
fun AppListContent(
    packageManager: PackageManager,
    usageStats: List<UsageStats>?,
    intent: Intent,
    onAppItemClicked: (Intent?) -> Unit
) {
    val list = remember {
        packageManager.queryIntentActivities(intent, 0)
    }
    val listState = rememberLazyListState()
    //todo : Fix this undiscovered list should be calculated from last visible item
    val undiscoveredList by remember {
        derivedStateOf {
            val startIndex =
                if ((listState.firstVisibleItemIndex + 9) <= list.lastIndex) listState.firstVisibleItemIndex + 9 else list.lastIndex
            list.subList(startIndex, list.lastIndex)
        }
    }
    var query by remember {
        mutableStateOf(TextFieldValue(""))
    }

    val filteredList = remember {
        list.filter {
            it.loadLabel(packageManager).contains(query.text)
        }
    }

    val appColumnList by remember {
        derivedStateOf {
            if (query.text.isNotBlank()) filteredList else list
        }
    }

    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchField(query = query) {
            query = it
        }

        MainAppColumn(
            listState = listState,
            appList = appColumnList,
            usageStats = usageStats,
            packageManager = packageManager,
            filterQuery = query.text,
            onAppItemClicked = onAppItemClicked
        )
        InvisibleAppItemsTray(
            appList = undiscoveredList,
            packageManager = packageManager,
            verticalListState = listState,
            onAppItemClicked = onAppItemClicked
        )
    }
}

@Composable
fun MainAppColumn(
    listState: LazyListState,
    appList: List<ResolveInfo>,
    usageStats: List<UsageStats>?,
    packageManager: PackageManager,
    filterQuery: String,
    onAppItemClicked: (Intent?) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .height(440.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(16.dp)
                .height(400.dp)
                .fillMaxWidth()
        ) {
            items(items = appList.filter {
                it.loadLabel(packageManager).toString().contains(filterQuery, ignoreCase = true)
            }) { item ->
                AppListItem(
                    item = item,
                    usageStats = usageStats,
                    packageManager = packageManager,
                    onAppItemClicked = onAppItemClicked
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )

    }

}


@Composable
fun SearchField(query: TextFieldValue, onQueryChanged: (TextFieldValue) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White)
        },
        colors = TextFieldDefaults.colors()
    )
}

@Composable
fun InvisibleAppItemsTray(
    appList: List<ResolveInfo>,
    packageManager: PackageManager,
    verticalListState: LazyListState,
    onAppItemClicked: (Intent?) -> Unit
) {
    val lazyRowState = rememberLazyListState()
    LazyRow(state = lazyRowState) {
        items(
            appList,
            key = { it.iconResource.toString() + it.activityInfo.packageName + it.activityInfo.hashCode() }) { app ->
            InvisibleAppItemsTrayItem(
                app = app,
                packageManager = packageManager,
                scope = this,
                onAppItemClicked = onAppItemClicked
            )
        }
    }

    /**
     * This gives smooth animation on swiping up, need to activate it
     * with with condition of only scrolling up
     */
    val isVerticalScrollingUp = verticalListState.isScrollingUp()

    if (appList.isNotEmpty() and (appList.size > 7)) {
        LaunchedEffect(appList) {
            if (isVerticalScrollingUp)
                lazyRowState.animateScrollToItem(0)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InvisibleAppItemsTrayItem(
    app: ResolveInfo,
    packageManager: PackageManager,
    scope: LazyItemScope,
    onAppItemClicked: (Intent?) -> Unit
) {
    val icon = remember {
        app.loadIcon(packageManager)
    }

    val launchIntent = remember {
        packageManager.getLaunchIntentForPackage(app.activityInfo.packageName)
    }
    scope.apply {
        Image(
            rememberDrawablePainter(icon),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(8.dp)
                .clickable {
                    onAppItemClicked(launchIntent)
                }
                .animateItemPlacement(
                    animationSpec = tween(
                        durationMillis = 50,
                        easing = FastOutSlowInEasing,
                    )
                )
        )
    }
}

@Composable
fun AppListItem(
    item: ResolveInfo,
    usageStats: List<UsageStats>?,
    packageManager: PackageManager,
    onAppItemClicked: (Intent?) -> Unit
) {
    val usageStat = remember {
        getUsageStatForThePackage(item, usageStats)
    }


    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val icon = remember {
        item.loadIcon(packageManager)
    }

    val appName = remember {
        item.loadLabel(packageManager).toString()
    }

    val launchIntent = remember {
        packageManager.getLaunchIntentForPackage(item.activityInfo.packageName)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(8.dp)
                    .clickable {
                        onAppItemClicked(launchIntent)
                    }
            ) {

                Image(
                    rememberDrawablePainter(drawable = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .background(color = Color.Transparent)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    text = appName,
                    modifier = Modifier,
                    color = Color.White
                )
            }

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            AppMetaData(usageStat)
        }
    }
}

@Composable
fun AppMetaData(stats: UsageStats?) {

    Column {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MetaDataItem(
                "Screen time today : ",
                TimeUnit.MILLISECONDS.toMinutes(stats?.totalTimeVisible ?: 0)
                    .toString() + " minutes"
            )
            MetaDataItem(
                "Last Used  : ",
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - stats?.lastTimeStamp!!)
                    .toString() + " seconds ago"
            )

        }
    }
}

@Composable
fun MetaDataItem(dataType: String, value: String) {
    Row {
        Text(text = dataType, color = Color.White)
        Text(text = value, color = Color.White)
    }
}

private fun getUsageStatForThePackage(
    item: ResolveInfo,
    usageStats: List<UsageStats>?
): UsageStats? {
    var value: UsageStats? = null
    usageStats?.forEach {
        if ((item.activityInfo.packageName).contains(it.packageName))
            value = it
    }
    return value
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MinimalDashboardTheme {

    }
}


//Utils
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}
