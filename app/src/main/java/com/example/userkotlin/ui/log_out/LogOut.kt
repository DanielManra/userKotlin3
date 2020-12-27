package com.example.userkotlin.ui.log_out

import Library.MemoryData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.userkotlin.R
import com.example.userkotlin.VerifyEmail
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class LogOut : Fragment(){
    private var memoryData: MemoryData? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedIntanceState: Bundle?
    ): View?{
        FirebaseAuth.getInstance().signOut()

        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this.requireContext(), gso)
        mGoogleSignInClient!!.signOut()
        mGoogleSignInClient!!.revokeAccess()


        memoryData = MemoryData.getInstance(this.requireContext())
        memoryData!!.saveData("user", "")
        startActivity(
                Intent(requireContext(),VerifyEmail::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return null
    }
}