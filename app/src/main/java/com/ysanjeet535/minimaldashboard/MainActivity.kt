package com.ysanjeet535.minimaldashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ysanjeet535.minimaldashboard.ui.theme.MinimalDashboardTheme
import java.util.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinimalDashboardTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    val i = Intent(Intent.ACTION_MAIN, null)
                    i.addCategory(Intent.CATEGORY_LAUNCHER)
                    val list = packageManager.queryIntentActivities(i, 0)
                    
                    Column(modifier = Modifier.padding(32.dp)) {
                        Text(text = "CUSTOM TATTI LAUNCHER", color = Color.White)
                        
                        Spacer(modifier = Modifier.height(64.dp))
                        
                        
                        Text(text = "${Calendar.getInstance().time}", color = Color.White)


                        Spacer(modifier = Modifier.height(64.dp))
                        
                        


                        LazyColumn(
                            modifier = Modifier
                                .padding(16.dp)
                                .height(240.dp)
                                .fillMaxWidth()
                        ) {
                            items(list) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(8.dp)
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
                                        modifier = Modifier.clickable {
                                            startActivity(
                                                packageManager.getLaunchIntentForPackage(item.activityInfo.packageName)
                                            )
                                        },
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(240.dp))

                        IconButton(onClick = {  }) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.background(Color.White))
                        }
                        
                    }



                }

            }
        }
    }
}

@Composable
fun Greeting(name: String, onClick: () -> Unit) {
    Text(text = "Hello $name!", modifier = Modifier.clickable { onClick() }, color = Color.White)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MinimalDashboardTheme {
        Greeting("Android") {}
    }
}