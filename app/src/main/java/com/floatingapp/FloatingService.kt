package com.floatingapp

import android.app.ActivityOptions
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 100

        windowManager.addView(floatingView, params)

        // Close button logic
        floatingView.findViewById<Button>(R.id.btn_close).setOnClickListener {
            stopSelf()
        }

        setupDragging(params)
        loadInstalledApps()
    }

    private fun setupDragging(params: WindowManager.LayoutParams) {
        val header = floatingView.findViewById<View>(R.id.drag_header)
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadInstalledApps() {
        val container = floatingView.findViewById<LinearLayout>(R.id.app_list_container)
        val pm = packageManager
        
        // Find all launchable apps
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)

        for (app in apps) {
            // Filter out this app itself
            if (app.activityInfo.packageName == packageName) continue

            // Create a horizontal layout for Icon + Name
            val itemLayout = LinearLayout(this)
            itemLayout.orientation = LinearLayout.HORIZONTAL
            itemLayout.gravity = Gravity.CENTER_VERTICAL
            itemLayout.setPadding(8, 16, 8, 16)

            // App Icon
            val iconView = ImageView(this)
            iconView.setImageDrawable(app.activityInfo.loadIcon(pm))
            iconView.layoutParams = LinearLayout.LayoutParams(100, 100)
            
            // App Name
            val nameView = TextView(this)
            nameView.text = app.activityInfo.loadLabel(pm)
            nameView.setPadding(20, 0, 0, 0)
            nameView.textSize = 16f
            nameView.setTextColor(android.graphics.Color.BLACK)

            itemLayout.addView(iconView)
            itemLayout.addView(nameView)
            container.addView(itemLayout)

            // Make the app item clickable
            itemLayout.setOnClickListener {
                launchAppInMiniScreen(app.activityInfo.packageName)
            }
        }
    }

    private fun launchAppInMiniScreen(pkgName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            
            try {
                // This tells Android to open the app in a mini box!
                val options = ActivityOptions.makeBasic()
                // Define where the mini app should appear: Left, Top, Right, Bottom
                val bounds = Rect(100, 200, 800, 1200) 
                options.launchBounds = bounds
                
                startActivity(launchIntent, options.toBundle())
            } catch (e: Exception) {
                // Fallback if OS blocks window mode
                startActivity(launchIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
