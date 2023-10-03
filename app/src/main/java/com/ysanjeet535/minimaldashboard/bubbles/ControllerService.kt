package com.ysanjeet535.minimaldashboard.bubbles

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ysanjeet535.minimaldashboard.MainActivity
import com.ysanjeet535.minimaldashboard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class ControllerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private var mWindowManager: WindowManager? = null
    private var mFloatingView: View? = null

    private var params: WindowManager.LayoutParams? = null


    @OptIn(ExperimentalComposeUiApi::class)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        startMyOwnForeground()
        //Inflate the floating view layout we created
//        mFloatingView = View
//            .inflate(this, R.layout.controller_layout, null)
        val LAYOUT_FLAG: Int =
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Specify the view position
        params!!.gravity =
            Gravity.CENTER_VERTICAL or Gravity.START //Initially view will be added to top-left corner
        params!!.x = 0
        params!!.y = 100
        //Add the view to the window
        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val composeView = ComposeView(this)
        composeView.setContent {
            Text(text = "Hello", color = androidx.compose.ui.graphics.Color.Black, fontSize = 50.sp,
                modifier = Modifier.wrapContentSize().background(androidx.compose.ui.graphics.Color.Green))
        }

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val runRecomposeScope = CoroutineScope(coroutineContext)
        val recomposer = Recomposer(coroutineContext)
        composeView.compositionContext = recomposer
        mFloatingView = composeView
        runRecomposeScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }



        mWindowManager!!.addView(composeView, params)
        //Set the close button


//        val controllerHandle = mFloatingView?.findViewById(R.id.controller) as ComposeView

        composeView.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                Log.d("move", "on touch called")

                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val Xdiff = (event.rawX - initialTouchX).toInt()
                        val Ydiff = (event.rawY - initialTouchY).toInt()
                        if (Xdiff < 10 && Ydiff < 10) {
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            intent.putExtra("fromwhere", "ser")
                            startActivity(intent)
                        }
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        //Update the layout with new X & Y coordinate
                        mWindowManager!!.updateViewLayout(composeView, params)
                        return true
                    }
                }
                return false
            }
        })
//        controllerHandle.createLifecycleAwareWindowRecomposer()
//        controllerHandle.setContent {
//            Box(modifier = Modifier.fillMaxSize()) {
//                Box(
//                    modifier = Modifier.background(
//                        androidx.compose.ui.graphics.Color.Yellow,
//                        shape = RectangleShape
//                    )
//                )
//            }
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mFloatingView != null) mWindowManager!!.removeView(mFloatingView)
    }

    private fun startMyOwnForeground() {
        val NOTIFICATION_CHANNEL_ID = "com.example.simpleapp"
        val channelName = "My Background Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("App is running in background")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }
}
