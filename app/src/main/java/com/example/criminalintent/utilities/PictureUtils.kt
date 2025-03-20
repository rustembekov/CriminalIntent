package com.example.criminalintent.utilities

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.view.WindowInsets

fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int): Bitmap {

    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(path, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()
    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth) {
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth
        inSampleSize = if (heightScale > widthScale) heightScale.toInt() else widthScale.toInt()
    }
    options.inJustDecodeBounds = false
    options.inSampleSize = inSampleSize
    return BitmapFactory.decodeFile(path, options)
}


fun getScaledBitmap(path: String, activity: Activity): Bitmap {
    val size = Point()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = activity.windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        size.x = windowMetrics.bounds.width() - insets.left - insets.right
        size.y = windowMetrics.bounds.height() - insets.top - insets.bottom
    } else {
        val displayMetrics = activity.resources.displayMetrics
        size.x = displayMetrics.widthPixels
        size.y = displayMetrics.heightPixels
    }

    return getScaledBitmap(path, size.x, size.y)
}


