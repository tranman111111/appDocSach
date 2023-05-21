package com.example.mybookapp.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.mybookapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding:ActivityRegisterBinding

    //firebase auth
    private lateinit var firebaseAuth:FirebaseAuth

    private lateinit var progressDialog:ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //init progress bar while creating
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //Handle back button click

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //Hand Click, begin register

        binding.registerBtn.setOnClickListener {
              /* Step
              1) Input data
              2) Validate data
              3) Create account - firebase auth
              4) Save user info - firebase realtime database
               */
            validateData()
        }
    }

    private var name = ""
    private var email = ""
    private var password = ""

    private fun validateData() {
//        input data
        name = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        val cPassword = binding.cPasswordEt.text.toString().trim()

//        Validate Data
        if(name.isEmpty()) {
            Toast.makeText(this, "Enter your name" ,Toast.LENGTH_SHORT).show()
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //Invalid email
            Toast.makeText(this, "Invalid Email" ,Toast.LENGTH_SHORT).show()
        }
        else if(password.isEmpty()) {
            Toast.makeText(this, "Enter your password" ,Toast.LENGTH_SHORT).show()
        }
        else if(cPassword.isEmpty()) {
            Toast.makeText(this, "Enter Confirm password" ,Toast.LENGTH_SHORT).show()
        }
        else if(password != cPassword) {
            Toast.makeText(this, "Password doesn't match" ,Toast.LENGTH_SHORT).show()
        }
        else {
            createUserAccount()
        }
    }

    private fun createUserAccount() {
        // create account fire base auth
        progressDialog.setMessage("Creating account...")
        progressDialog.show()
        //crete user in firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                updateUserInfo()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed creating account due to ${e.message}" ,Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserInfo() {
        progressDialog.setMessage("Saving user info")
        val timestamp = System.currentTimeMillis()
        // get current user uid, since user is registered
        val uid = firebaseAuth.uid

        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = name
        hashMap["profileImage"] = "" // will do in profile edit
        hashMap["userType"] = "user" // possible values are user/admin
        hashMap["timestamp"] = timestamp

        //set Data to db

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener{
                // User info saved, open user dashboard
                progressDialog.dismiss()
                Toast.makeText(this, "Account created...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardUserActivity::class.java))
                finish()

            }
            .addOnFailureListener { e ->
                //failed adding data to db
                progressDialog.dismiss()
                Toast.makeText(this, "Failed saving user info due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}