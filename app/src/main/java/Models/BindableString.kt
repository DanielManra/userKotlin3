package Models

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.example.userkotlin.BR
import java.util.*

class BindableString : BaseObservable() {
    private var value: String? = null
    /*
    * BaseObservable que puede ampliar. La clase de datos es responsable de notificar
    * cuando cambian las propiedades. Esto se hace asignando una anotación @Bindable
    * al captador y notificandolo en el definidor. Este oyente se invoca en cada actu-
    * alización y actualiza a las visitas correspondientes.
    */
    @Bindable
    fun getValue(): String {
        return if (value != null) value!! else ""
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun setValue(value: String){
        if(!Objects.equals(this.value, value)){
            this.value = value
            notifyPropertyChanged(BR.value)
        }
    }

}