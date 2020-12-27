package com.example.userkotlin

import Library.MemoryData
import ViewModels.LoginViewModels
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.userkotlin.databinding.VerifyEmailBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class VerifyEmail : AppCompatActivity() {
    private var memoryData: MemoryData? = null
    private val RC_SIGN_IN = 9001
    private var mAuth: FirebaseAuth? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var _bindingEmail: VerifyEmailBinding? = null
    private var _login: LoginViewModels? = null



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.verify_email)

        memoryData = MemoryData.getInstance(this)
        if (memoryData!!.getData("user")== ""){
            _bindingEmail = DataBindingUtil.setContentView(this,R.layout.verify_email)
            //_bindingEmail!!.emailModel = LoginViewModels(this, _bindingEmail,null)
            _login = LoginViewModels(this, _bindingEmail, null)
            _bindingEmail!!.emailModel = _login
            val window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = this.resources.getColor(R.color.colorBlue, null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
            mAuth = FirebaseAuth.getInstance()
            _bindingEmail!!.googleSingInButton.setOnClickListener{v -> signIn()}
        }else{
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

    }
    private fun signIn(){
        val signInIntent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN){
            val task =
            GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account =
                    task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            }catch (e: ApiException){
                val data2 = e.message
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?){
        val credential = GoogleAuthProvider.getCredential(acct!!.idToken, null)
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(
                this
            ){task: Task<AuthResult> ->
                if (task.isSuccessful){
                    val user = mAuth!!.currentUser
                  //  memoryData!!.saveData("user", user!!.email!!)
                //access()
                    if (! _login!!.RegisterUser(user!!.email!!)){
                        mGoogleSignInClient!!.signOut()
                        mGoogleSignInClient!!.revokeAccess()
                    }
                }else{
                    Snackbar.make(
                        _bindingEmail!!.googleSingInButton,
                        "Authentication Failed",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener(
                this
            ){ task: Exception ->
                val data2 = task.message
            }
    }
    private fun access(){
        startActivity(
            Intent(this, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}