package Models

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

class Item: BaseObservable() {
   private var selectedItemPosition = 0
    @Bindable
    fun getSelectedItemPosition(): Int{
        return selectedItemPosition
    }
    fun setSelectedItemPosition(selectedItemPosition: Int){
        this.selectedItemPosition
        notifyPropertyChanged(BR.selectedItemPosition)
    }

}