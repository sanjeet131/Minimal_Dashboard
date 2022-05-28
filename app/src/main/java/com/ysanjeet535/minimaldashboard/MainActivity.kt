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
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ysanjeet535.minimaldashboard.ui.theme.MinimalDashboardTheme
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private var usageStats: List<UsageStats>? = null

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        .background(MaterialTheme.colors.surface)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            keyboardController?.hide()
                            focusManager.clearFocus(true)
                        }
                ) {
                    val launchIntent = Intent(Intent.ACTION_MAIN, null)
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
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
    val list = packageManager.queryIntentActivities(intent, 0)
    val listState = rememberLazyListState()
    val undiscoveredList by remember {
        derivedStateOf {
            list.subList(listState.firstVisibleItemIndex + 8, list.lastIndex)
        }
    }
    var query by remember {
        mutableStateOf(TextFieldValue(""))
    }

    val filteredList = list.filter {
        it.loadLabel(packageManager).contains(query.text)
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
    LazyColumn(
        state = listState,
        modifier = Modifier
            .padding(16.dp)
            .height(440.dp)
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
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Color.White,
            disabledBorderColor = Color.Transparent
        )
    )
}


@Composable
fun InvisibleAppItemsTray(
    appList: List<ResolveInfo>,
    packageManager: PackageManager,
    onAppItemClicked: (Intent?) -> Unit
) {
    LazyRow {
        items(appList) { app ->
            Image(
                rememberDrawablePainter(app.loadIcon(packageManager)),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
                    .clickable {
                        onAppItemClicked(packageManager.getLaunchIntentForPackage(app.activityInfo.packageName))
                    }
            )
        }
    }

}

@OptIn(ExperimentalAnimationApi::class)
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