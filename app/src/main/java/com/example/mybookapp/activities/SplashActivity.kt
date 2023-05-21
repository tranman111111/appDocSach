package com.example.mybookapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.example.mybookapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        firebaseAuth = FirebaseAuth.getInstance()

        Handler().postDelayed(Runnable {
            checkUser()
        }, 2000)
    }

    private fun checkUser() {
        // get current user, if logged in or not
        val firebaseUser = firebaseAuth.currentUser
        val TAG = "CHECK USER"
//        startActivity(Intent(this@SplashActivity, DashboardUserActivity::class.java))
//        finish()
            if(firebaseUser == null) {
                //User not logged in, goto main screen
                Log.d(TAG, "SUCCEESS")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val ref = FirebaseDatabase.getInstance().getReference("Users")
                Log.d(TAG, "SUCCEESS1")
                Log.d(TAG, "${firebaseUser.uid}")
                ref.child(firebaseUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // get user type
                            val userType = snapshot.child("userType").value
                            Log.d(TAG, "SUCCEESS2")
                            if(userType == "user") {
                                // user's Dashboard
                                startActivity(Intent(this@SplashActivity, DashboardUserActivity::class.java))
                                finish()
                            }
                            else if(userType == "admin") {
                                // admin's Dashboard
                                startActivity(Intent(this@SplashActivity, DashboardAdminActivity::class.java))
                                finish()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })
            }
    }
}