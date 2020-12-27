package Library

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class Networks (activity: Activity) : AppCompatActivity(){
    private var _activity: Activity? = null
    init {
        _activity = activity
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun verificarNerworks () : Boolean{
        var valor = false
        var cm = _activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm != null){
            val nc = cm.getNetworkCapabilities(cm.activeNetwork)
            if (nc != null){
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                    valor = true
                }else if(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    valor = true
                }
            }else{
                valor = false
            }
        }else{
            valor = false
        }
        return valor
    }
}