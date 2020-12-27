package Library

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class Permissions (activity: Activity){
    private var _activity: Activity? = null

    init {
        _activity = activity
    }

    fun STORAGE(): Boolean{
        return if (ContextCompat.checkSelfPermission(
                _activity!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                _activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(
                _activity!!, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),1
            )
            false
        }else{
            true
        }
    }
    fun CAMERA(): Boolean{
        return if (ContextCompat.checkSelfPermission(
                _activity!!,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions(
                _activity!!,arrayOf(
                    Manifest.permission.CAMERA
                ),2
            )
            false
        }else{
            true
        }
    }
}