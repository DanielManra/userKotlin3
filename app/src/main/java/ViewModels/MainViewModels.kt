package ViewModels

import Library.MemoryData
import Library.Networks
import Models.Collections
import Models.Pojo.User
import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.userkotlin.R
import com.example.userkotlin.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.common.reflect.TypeToken
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import java.lang.reflect.Type

class MainViewModels {
    private var _activity: Activity? = null
    private var _headerView: View? = null
    private var _memoryData: MemoryData? = null
    private var _navigationView: NavigationView? = null
    private var _db: FirebaseFirestore? = null
    private var _storage: FirebaseStorage? = null
    private var _storageRef: StorageReference? = null
    private var _imageViewUser: ImageView? = null
    private var _textViewName: TextView? = null
    private var _textViewLastName: TextView? = null
    private var _textViewEmail: TextView? = null
    private var typeItem: Type = object : TypeToken<User?>() {}.type
    private var gson = Gson()
    private var data: User? = null

    @RequiresApi(Build.VERSION_CODES.M)
    constructor(activity: Activity, binding: ActivityMainBinding) {
        _activity = activity
        _navigationView = binding.navView
        _headerView = _navigationView!!.getHeaderView(0)
        _memoryData = MemoryData.getInstance(_activity!!)
        _db = FirebaseFirestore.getInstance()
        _storage = FirebaseStorage.getInstance()
        _storageRef = _storage!!.reference
        _textViewName = _headerView!!.findViewById(R.id.textViewName)
        _textViewLastName = _headerView!!.findViewById(R.id.textViewLastName)
        _textViewEmail = _headerView!!.findViewById(R.id.textViewEmail)
        _imageViewUser = _headerView!!.findViewById(R.id.imageViewUser)
        GetUser()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun GetUser() {
        val ONE_MEGABYTE = 1024 * 1024.toLong()
        if (Networks(_activity!!).verificarNerworks()) {
            data = gson.fromJson<User>(_memoryData!!.getData("user"), typeItem)
            _textViewName!!.text = data?.name
            _textViewLastName!!.text = data?.lastName
            _textViewEmail!!.text = data?.email
            _storageRef!!.child(Collections.User.USERS + "/" + data?.email)
                .getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes: ByteArray ->
                    val _selectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    _imageViewUser!!.setImageBitmap(_selectedImage)
                    _imageViewUser!!.scaleType = ImageView.ScaleType.CENTER_CROP
                }
        } else {
            Snackbar.make(_navigationView!!, R.string.networks, Snackbar.LENGTH_LONG).show()
        }
    }
}