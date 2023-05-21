package com.example.mybookapp.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.mybookapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //init progress bar while creating
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, not have account, go to register screen

        binding.noAccountTv.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        //handle click, begin login
        binding.loginBtn.setOnClickListener {
            /*
            1) Input data
            2) Validate Data
            3) Login- firebase Auth
            4) Check user type -firebase auth
            If User mote to user dashboard
             */

            validateData()
        }

        binding.forgotTv.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private var email = ""
    private var password = ""

    private fun validateData() {
        // Input data
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        //Check validate
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format" , Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
        } else {
            loginUser()
        }
    }

    private fun loginUser() {
        progressDialog.setMessage("Logging in...")
        progressDialog.show()

       try {
           firebaseAuth.signInWithEmailAndPassword(email, password)
               .addOnSuccessListener {
                   checkUser()
               }
               .addOnFailureListener { e ->
                   progressDialog.dismiss()
                   Toast.makeText(this, "Login failed due to ${e.message}", Toast.LENGTH_SHORT).show()
               }
       } catch (e: Exception) {
           Log.d("Login", "${e.message}")
       }

//            .addOnSuccessListener {
//                checkUser()
//            }
//            .addOnFailureListener { e ->
//                progressDialog.dismiss()
//                Toast.makeText(this, "Login failed due to ${e.message}", Toast.LENGTH_SHORT).show()
//            }
    }

    private fun checkUser() {
//        check user or admin
        progressDialog.setMessage("Checking User...")
        val  firebaseUser = firebaseAuth.currentUser!!

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()
                    // get user type
                    val userType = snapshot.child("userType").value
                    if(userType == "user") {
                        // user's Dashboard
                        startActivity(Intent(this@LoginActivity, DashboardUserActivity::class.java))
                        finish()
                    } else if(userType == "admin") {
                        // admin's Dashboard
                        startActivity(Intent(this@LoginActivity, DashboardAdminActivity::class.java))
                        finish()
                    }
                }
            })
    }
}