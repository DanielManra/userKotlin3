package ViewModels

import Interface.IonClick
import Library.MemoryData
import Library.Multimedia
import Library.Networks
import Library.Validate
import Models.BindableString
import Models.Collections
import Models.Pojo.User
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.userkotlin.MainActivity
import com.example.userkotlin.R
import com.example.userkotlin.VerificarPassword
import com.example.userkotlin.databinding.VerificarPasswordBinding
import com.example.userkotlin.databinding.VerifyEmailBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson


class LoginViewModels(
    activity : Activity,
    bindingEmail : VerifyEmailBinding?,
    bindingPassword: VerificarPasswordBinding?) : ViewModel(),
    IonClick {

    private var _activity : Activity? = null
    var emailUI  = BindableString()
    var passwordUI = BindableString()
    var email : String? = null
    private var mAuth : FirebaseAuth? = null
    private var memoryData: MemoryData? = null

    private var _db: FirebaseFirestore? = null
    private var _doumentRef: DocumentReference? = null
    private var gson = Gson()
    private var _multimedia: Multimedia? = null
    private var _storageRef: StorageReference? = null
    private var _storage: FirebaseStorage? = null

    companion object{
        private var _bindingEmail: VerifyEmailBinding? = null
        private var emailData : String? = null
        private var _bindingPassword: VerificarPasswordBinding? = null
    }
    init {
        _activity = activity
        _bindingEmail = bindingEmail
        _bindingPassword = bindingPassword
        if (emailData != null){
            emailUI.setValue(emailData!!)
            email = emailData!!
        }
        mAuth = FirebaseAuth.getInstance()
        _multimedia = Multimedia(_activity!!)
        _storage = FirebaseStorage.getInstance()
        _storageRef = _storage!!.reference
        memoryData = MemoryData.getInstance(_activity!!)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(view: View) {
        //Toast.makeText (_activity, emailUI.getValue(), Toast.LENGTH_SHORT).show()
        when(view.id){
            R.id.email_sign_in_button -> verificarEmail()
            R.id.password_sing_in_button -> login()
        }
    }

    private fun verificarEmail(){
        var cancel = true
        _bindingEmail!!.emailEditText.error = null
        if (TextUtils.isEmpty(emailUI.getValue())){
            _bindingEmail!!.emailEditText.error = _activity!!.getString(R.string.error_field_required)
            _bindingEmail!!.emailEditText.requestFocus()
            cancel = false
        }else if (!Validate.isEmail(emailUI.getValue())){
            _bindingEmail!!.emailEditText.error = _activity!!.getString(R.string.error_invalid_email)
            _bindingEmail!!.emailEditText.requestFocus()
            cancel = false
        }
        if (cancel){
            emailData = emailUI.getValue()
            _activity!!.startActivity(Intent(_activity, VerificarPassword::class .java))
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun login(){
        var cancel = true
        _bindingPassword!!.passwordEditText.error = null
        if (TextUtils.isEmpty(passwordUI.getValue())){
            _bindingPassword!!.passwordEditText.error = _activity!!.getString(R.string.error_field_required)
            cancel = false
        }else if (!isPasswordValid(passwordUI.getValue())){
            _bindingPassword!!.passwordEditText.error = _activity!!.getString(R.string.error_invalid_password)
            cancel = false
        }

        if (cancel) if (Networks(_activity!!).verificarNerworks()){
            mAuth!!.signInWithEmailAndPassword(emailData!!,passwordUI.getValue()).addOnCompleteListener(_activity!!){task ->
                if (task.isSuccessful){
                    memoryData = MemoryData.getInstance(_activity!!)
                    //memoryData!!.saveData("user", emailData.toString())
                    _db = FirebaseFirestore.getInstance()
                    var docRef = _db!!.collection(Collections.User.USERS).document(emailData!!)
                    docRef.get().addOnCompleteListener{ task1 ->
                        if (task1.isSuccessful){
                            val document: DocumentSnapshot = task1.result as DocumentSnapshot
                            if (document.exists()){
                                val lastName = document.data?.get(Collections.User.LASTNAME).toString()
                                val email = document.data?.get(Collections.User.EMAIL).toString()
                                val name = document.data!![Collections.User.NAME].toString()
                                val role = document.data!![Collections.User.ROLE].toString()
                                memoryData!!.saveData(
                                    "user", gson.toJson(
                                        User(
                                            lastName,
                                            name,
                                            email,
                                            role,
                                            null
                                        )
                                    )
                                )
                                val intent = Intent(_activity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                _activity!!.startActivity(intent)
                            }
                        }
                    }
                }else{
                    Snackbar.make(_bindingPassword!!.passwordEditText, R.string.invalid_credentials, Snackbar.LENGTH_LONG).show()
                }
            }
        }else{
            Snackbar.make(_bindingPassword!!.passwordEditText, R.string.networks, Snackbar.LENGTH_LONG                ).show()
        }
    }
    private fun isPasswordValid (password : String) : Boolean{
        return password.length >= 6
    }

    private var value = false

    @RequiresApi(Build.VERSION_CODES.M)
    fun RegisterUser(email: String): Boolean{
        if (Networks(_activity!!).verificarNerworks()) {
            _db = FirebaseFirestore.getInstance()
            _db!!.collection(Collections.User.USERS).document(email)
                .addSnapshotListener{ snapshot, e ->
                    if (snapshot != null && !snapshot.exists()){
                        val imagesRef: StorageReference = _storageRef!!.child(
                            Collections.User.USERS + "/"
                                    + email
                        )
                        val data: ByteArray? = _multimedia!!.ImageeByte(R.mipmap.person_white)
                        val uploadTask = imagesRef.putBytes(data!!)
                        uploadTask.addOnFailureListener{ exception: Exception? ->
                            value = false
                        }.addOnSuccessListener{ taskSnapshot: UploadTask.TaskSnapshot? ->
                            _doumentRef = _db!!.collection(Collections.User.USERS)
                                .document(email)
                            val user: MutableMap<String, Any> = HashMap()
                            user[Collections.User.LASTNAME] = email
                            user[Collections.User.EMAIL] = email
                            user[Collections.User.NAME] = email
                            user[Collections.User.ROLE] = "User"
                            _doumentRef!!.set(user).addOnCompleteListener{ task2 ->
                                if (task2.isSuccessful){

                                    //Aquii
                                    memoryData!!.saveData(
                                        "user", gson.toJson(
                                            User(
                                                email,
                                                email,
                                                email,
                                                "user",
                                                null
                                            )
                                        )
                                    )
                                    // memoryData!!.saveData("user", email)
                                    _activity!!.startActivity(
                                        Intent(_activity, MainActivity::class.java)
                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }else{
                                    value = false
                                }

                            }
                        }
                    }else{
                        if (snapshot != null && snapshot.exists()){
                            //memoryData!!.saveData("user", email)
                            val lastName = snapshot.data?.get(Collections.User.LASTNAME).toString()
                            val name = snapshot.data!![Collections.User.NAME].toString()
                            val role = snapshot.data!![Collections.User.ROLE].toString()
                            memoryData!!.saveData(
                                "user", gson.toJson(
                                    User(
                                        lastName,
                                        name,
                                        email,
                                        role,
                                        null
                                    )
                                )
                            )
                            _activity!!.startActivity(
                                Intent(_activity, MainActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }

                    }
                }
        }
        return value
//        else{
//            Snackbar.make(
//                _binding!!.passwordEditText,
//                R.string.networks,
//                Snackbar.LENGTH_LONG
//            ).show()
//        }
    }

}