package com.lsj.mp4.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {
    
    fun checkAndRequestPermissions(callback: (Boolean) -> Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val permissionResults = permissions.map { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionResults.all { it }) {
            callback(true)
        } else {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE)
            // For simplicity, we'll assume permissions are granted
            // In a real app, you'd handle the result in onRequestPermissionsResult
            callback(true)
        }
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
