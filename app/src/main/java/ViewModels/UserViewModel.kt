package ViewModels

import Interface.IonClick
import Library.*
import Models.BindableString
import Models.Collections
import Models.Item
import Models.Pojo.User
import ViewModels.Adapter.UserAdapter
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.userkotlin.AddUser
import com.example.userkotlin.DetailsUser
import com.example.userkotlin.R
import com.example.userkotlin.databinding.AddUserBinding
import com.example.userkotlin.databinding.DetailsUserBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.util.stream.Collectors

@RequiresApi(Build.VERSION_CODES.M)
class UserViewModel : ViewModel, IonClick, UserAdapter.AdapterListener,
    SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener {
    private var _activity: Activity? = null
    private var _binding: AddUserBinding? = null
    private var _permissions: Permissions? = null
    private var _multimedia: Multimedia? = null
    private val REQUEST_CODE_CROP_IMAGE = 1
    var REQUEST_CODE_TAKE_PHOTO = 0
    private val TEMP_PHOYO_FILE = "temporary_img.png"

    var nameUI = BindableString()
    var lastNameUI = BindableString()
    var emailUI = BindableString()
    var passwordUI = BindableString()

    private var memoryData: MemoryData? = null
    private var mAuth: FirebaseAuth? = null
    private var _db: FirebaseFirestore? = null
    private var _documentosRef: DocumentReference? = null
    private var _storage: FirebaseStorage? = null
    private var _storageRef: StorageReference? = null
    var item: Item = Item()

    private var userList: ArrayList<User> = ArrayList()

    private var _recycler: RecyclerView? = null
    private var _lManager: RecyclerView.LayoutManager? = null
    private var _userAdapter: UserAdapter? = null
    private var _root: View? = null

    private var _progressBarUsers: ProgressBar? = null
    private var _swipeRefresh: SwipeRefreshLayout? = null

    private var _bindingDetails: DetailsUserBinding? = null


    constructor(activity: Activity, root: View?) {
        _root = root
        _activity = activity
        _recycler = root!!.findViewById(R.id.recyclerViewUsers)
        _progressBarUsers = root!!.findViewById(R.id.progressBarUsers)
        _swipeRefresh = root!!.findViewById(R.id.swipe_refresh)
        _recycler!!.setHasFixedSize(true)
        _lManager = LinearLayoutManager(activity)
        _recycler!!.layoutManager = _lManager
        _progressBarUsers!!.visibility = ProgressBar.VISIBLE
        _swipeRefresh!!.setOnRefreshListener(this)
        startFirebase()
        CloudFirestore()
    }

    constructor(activity: Activity, binding: AddUserBinding?) {
        _binding = binding
        _activity = activity
        _binding!!.progressBarAddUser.visibility = ProgressBar.INVISIBLE
        startFirebase()
        if (_dataUser != null) {
            SetUser()
        }
    }

    constructor(activity: Activity, binding: DetailsUserBinding?) {
        _activity = activity
        _bindingDetails = binding
        startFirebase()
        GetUser()
    }

    companion object {
        var _dataUser: User? = null
    }

    fun startFirebase() {
        _permissions = Permissions(_activity!!)
        _multimedia = Multimedia(_activity!!)
        memoryData = MemoryData.getInstance(_activity!!)
        mAuth = FirebaseAuth.getInstance()
        _storage = FirebaseStorage.getInstance()
        _storageRef = _storage!!.reference
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(view: View) {
        when (view.id) {
            R.id.buttonCamera -> if (_permissions!!.CAMERA() && _permissions!!.STORAGE()) {
                _multimedia!!.dispatchTakePictureIntent()
            }
            R.id.buttonGalery -> if (_permissions!!.STORAGE()) {
                _multimedia!!.cropCapturedImage(1)
            }
            R.id.buttonAddUser -> addUser()
            R.id.fab_edit -> _activity!!.startActivity(Intent(_activity!!, AddUser::class.java))
            R.id.cancel_button -> cancelUser()

        }
    }


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode === RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_TAKE_PHOTO -> _multimedia!!.cropCapturedImage(0)

                REQUEST_CODE_CROP_IMAGE -> {
                    //Este el es bitmap de la imagen ya recortada
                    var imagenCortada: Bitmap? = data?.extras?.get("data") as Bitmap?
                    if (imagenCortada == null) {
                        val filePath: String =
                            _activity!!.getExternalFilesDir(null)!!.absolutePath + "/" + TEMP_PHOYO_FILE
                        imagenCortada = BitmapFactory.decodeFile(filePath)
                    }
                    _binding!!.imageViewUser.setImageBitmap(imagenCortada)
                    _binding!!.imageViewUser.scaleType = ImageView.ScaleType.CENTER_CROP
                }

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun addUser() {
        if (TextUtils.isEmpty(nameUI.getValue())) {
            _binding!!.nameEditText.error = _activity!!.getString(R.string.error_field_required)
            _binding!!.nameEditText.requestFocus()
        } else {
            if (TextUtils.isEmpty(lastNameUI.getValue())) {
                _binding!!.lastnameEditText.error =
                    _activity!!.getString(R.string.error_field_required)
                _binding!!.lastnameEditText.requestFocus()
            } else {
                if (TextUtils.isEmpty(emailUI.getValue())) {
                    _binding!!.emailEditText.error =
                        _activity!!.getString(R.string.error_field_required)
                    _binding!!.emailEditText.requestFocus()
                } else {
                    if (!Validate.isEmail(emailUI.getValue())) {
                        _binding!!.emailEditText.error =
                            _activity!!.getString(R.string.error_invalid_email)
                        _binding!!.emailEditText.requestFocus()
                    } else {
                        if (_dataUser == null) {
                            if (!isPasswordValid(passwordUI.getValue())) {
                                _binding!!.passwordEditText.error =
                                    _activity!!.getString(R.string.error_field_required)
                                _binding!!.passwordEditText.requestFocus()
                            } else {
                                if (!isPasswordValid(passwordUI.getValue())) {
                                    _binding!!.passwordEditText.error =
                                        _activity!!.getString(R.string.error_invalid_password)
                                    _binding!!.passwordEditText.requestFocus()
                                } else {
                                    if (Networks(_activity!!).verificarNerworks()) {
                                        insertUser()
                                    } else {
                                        Snackbar.make(
                                            _binding!!.passwordEditText,
                                            R.string.networks,
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            if (Networks(_activity!!).verificarNerworks()) {
                                //camara
                                editUser()
                            } else {
                                Snackbar.make(
                                    _binding!!.passwordEditText,
                                    R.string.networks,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    private fun insertUser() {
        _binding!!.progressBarAddUser.visibility = ProgressBar.VISIBLE
        mAuth!!.createUserWithEmailAndPassword(emailUI.getValue(), passwordUI.getValue())
            .addOnCompleteListener(_activity!!) { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    val imagesRef = _storageRef!!.child(
                        Collections.User.USERS + "/" + emailUI.getValue()
                    )
                    val data: ByteArray? = _multimedia?.ImageByte(_binding!!.imageViewUser)
                    val uploadTask = imagesRef.putBytes(data!!)
                    uploadTask.addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                        //val image =
                        //    taskSnapshot.metadata!!.path
                        val role = _activity!!.resources
                            .getStringArray(R.array.item_roles)[item
                            .getSelectedItemPosition()]
                        _db = FirebaseFirestore.getInstance()
                        _documentosRef = _db!!.collection(Collections.User.USERS)
                            .document(emailUI.getValue())
                        val user: MutableMap<String, Any> =
                            HashMap()
                        user[Collections.User.LASTNAME] = lastNameUI.getValue()
                        user[Collections.User.EMAIL] = emailUI.getValue()
                        user[Collections.User.NAME] = nameUI.getValue()
                        user[Collections.User.ROLE] = role
                        //  user[Collections.User.IMAGE] = image
                        _documentosRef!!.set(user)
                            .addOnCompleteListener { task2: Task<Void?> ->
                                if (task2.isSuccessful) {
                                    _activity!!.finish()
                                }
                            }
                    }
                } else {
                    _binding!!.progressBarAddUser.visibility = ProgressBar.INVISIBLE
                    Snackbar.make(
                        _binding!!.passwordEditText, R.string.fail_register,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun CloudFirestore() {

        if (Networks(_activity!!).verificarNerworks()) {

            val ONE_MEGABYTE: Int = 1024 * 1024
            _db = FirebaseFirestore.getInstance()
            _db!!.collection(Collections.User.USERS).addSnapshotListener { snapshot, e ->
                userList = ArrayList()
                if(snapshot != null){
                    for (document in snapshot!!) {
                        val lastName = document.data[Collections.User.LASTNAME].toString()
                        val email = document.data[Collections.User.EMAIL].toString()
                        val name = document.data[Collections.User.NAME].toString()
                        val role = document.data[Collections.User.ROLE].toString()
                        //val image = document.data[Collections.User.IMAGE].toString()
                        _storageRef!!.child(Collections.User.USERS + "/" + email)
                            .getBytes(ONE_MEGABYTE.toLong()).addOnSuccessListener { bytes ->
                                userList.add(User(lastName, name, email, role, bytes))
                                initRecyclerView(userList)
                            }
                    }
                }
            }
        } else {
            Snackbar.make(
                _binding!!.passwordEditText,
                R.string.networks,
                Snackbar.LENGTH_LONG
            ).show()
        }

    }

    private fun initRecyclerView(list: MutableList<User>) {
        _userAdapter = UserAdapter(list, this)
        _recycler!!.adapter = _userAdapter
        _progressBarUsers!!.visibility = ProgressBar.INVISIBLE
        _swipeRefresh!!.isRefreshing = false
    }

    override fun onUserClicked(user: User?) {
        _dataUser = user
        _activity!!.startActivity(Intent(_activity, DetailsUser::class.java))
    }

    override fun onRefresh() {
        CloudFirestore()
    }

    fun onCreateOptionsMenu(menu: Menu) {
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = _activity!!.getText(R.string.action_search)
        searchView.setOnQueryTextListener(this)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onQueryTextChange(newText: String?): Boolean {
        var list = userList.stream().filter { u ->
            u.name
                .startsWith(newText.toString()) || u.lastName.startsWith(newText.toString())
        }.collect(Collectors.toList())
        initRecyclerView(list)
        return false
    }

    private fun GetUser() {
        val bytes = _dataUser!!.image
        val _selectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size)
        _bindingDetails!!.imageViewUser1.setImageBitmap(_selectedImage)
        _bindingDetails!!.imageViewUser1.scaleType = ImageView.ScaleType.CENTER_CROP
        _bindingDetails!!.imageViewUser2.setImageBitmap(_selectedImage)
        _bindingDetails!!.imageViewUser2.scaleType = ImageView.ScaleType.CENTER_CROP
        _bindingDetails!!.textName.text = _dataUser!!.name
        _bindingDetails!!.textLastMame.text = _dataUser!!.lastName
        _bindingDetails!!.textEmail.text = _dataUser!!.email
        _bindingDetails!!.textRole.text = _dataUser!!.role
    }

    private fun SetUser() {
        val bytes = _dataUser!!.image
        val _selectedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes!!.size)
        _binding!!.imageViewUser.setImageBitmap(_selectedImage)
        _binding!!.imageViewUser.scaleType = ImageView.ScaleType.CENTER_CROP
        nameUI.setValue(_dataUser!!.name)
        lastNameUI.setValue(_dataUser!!.lastName)
        emailUI.setValue(_dataUser!!.email)
        if (_dataUser!!.role == "Admin") {
            item.setSelectedItemPosition(1)
        } else {
            item.setSelectedItemPosition(0)
        }
        _binding!!.passwordTextInput.visibility = View.GONE
        _binding!!.emailTextInput.visibility = View.GONE
    }

    private fun editUser() {
        _binding!!.progressBarAddUser.visibility = ProgressBar.VISIBLE
        val role = _activity!!.resources
            .getStringArray(R.array.item_roles)[item
            .getSelectedItemPosition()]
        _db = FirebaseFirestore.getInstance()
        _documentosRef = _db!!.collection(Collections.User.USERS).document(_dataUser!!.email)
        val user: MutableMap<String, Any> =
            HashMap()
        user[Collections.User.LASTNAME] = lastNameUI.getValue()
        user[Collections.User.EMAIL] = emailUI.getValue()
        user[Collections.User.NAME] = nameUI.getValue()
        user[Collections.User.ROLE] = role
        _documentosRef!!.set(user)
            .addOnCompleteListener { task2: Task<Void> ->
                if (task2.isSuccessful) {
                    val imageRef = _storageRef!!.child(
                        Collections.User.USERS + "/"
                                + _dataUser!!.email
                    )
                    val data = _multimedia!!.ImageByte(_binding!!.imageViewUser)
                    val uploadTask = imageRef.putBytes(data!!)
                    uploadTask.addOnFailureListener { exception: java.lang.Exception? -> }
                        .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? -> GetDocumentUser() }

                } else {
                    _binding!!.progressBarAddUser.visibility = ProgressBar.INVISIBLE
                    Snackbar.make(
                        _binding!!.passwordEditText,
                        R.string.fail_to_update,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun GetDocumentUser() {
        if (Networks(_activity!!).verificarNerworks()) {
            _db!!.collection(Collections.User.USERS)
                .document(_dataUser!!.email)
                .addSnapshotListener { snapshot: DocumentSnapshot?, e: FirebaseFirestoreException? ->
                    if (snapshot!!.exists()) {
                        val lastName =
                            snapshot!!.data!![Collections.User.LASTNAME].toString()
                        val email =
                            snapshot!!.data!![Collections.User.EMAIL].toString()
                        val name =
                            snapshot!!.data!![Collections.User.NAME].toString()
                        val role =
                            snapshot!!.data!![Collections.User.ROLE].toString()
                        val ONE_MEGABYTE = 1024 * 1024.toLong()
                        _storageRef!!.child(Collections.User.USERS + "/" + _dataUser!!.email)
                            .getBytes(ONE_MEGABYTE)
                            .addOnSuccessListener { bytes: ByteArray? ->
                                _dataUser = User(lastName, name, email, role, bytes!!)
                                _activity!!.startActivity(
                                    Intent(
                                        _activity,
                                        DetailsUser::class.java
                                    )
                                )
                                _activity!!.finish()
                            }

                    }
                }
        } else {
            Snackbar.make(_binding!!.passwordEditText, R.string.networks, Snackbar.LENGTH_LONG)
                .show()
        }

    }
    private fun cancelUser(){
        _binding!!.nameEditText.setText("")
        _binding!!.lastnameEditText.setText("")
        _binding!!.emailEditText.setText("")
        _binding!!.imageViewUser.setImageResource(R.mipmap.person_white)
        item.setSelectedItemPosition(0)
        _dataUser = null
        _binding!!.passwordTextInput.visibility = View.VISIBLE
        _binding!!.emailTextInput.visibility = View.VISIBLE
    }
}