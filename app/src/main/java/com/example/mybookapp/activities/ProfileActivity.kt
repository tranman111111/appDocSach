package com.example.mybookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Display.Mode
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.mybookapp.MyApplication
import com.example.mybookapp.R
import com.example.mybookapp.adapters.AdapterPdfFavorite
import com.example.mybookapp.databinding.ActivityProfileBinding
import com.example.mybookapp.models.ModelPdf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //arraylist to hold books

    private lateinit var firebaseUser: FirebaseUser

    private lateinit var booksArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfFavorite : AdapterPdfFavorite

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //reset to default values
        binding.accountTypeTv.text = "N/A"
        binding.memberDateTv.text = "N/A"
        binding.favoriteBookCountTv.text = "N/A"
        binding.accountStatusTv.text = "N/A"

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        loadUserInfo()
        loadFavoriteBooks()

        //handle click, goback
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //hanle click, open edit profile
        binding.profileEditBtn.setOnClickListener {
           startActivity(Intent(this, ProfileEditActivity::class.java))
        }

        //handle click, verify user if not
        binding.accountStatusTv.setOnClickListener {
            if(firebaseUser.isEmailVerified) {
                //User is verified
                Toast.makeText(this, "Already verified...!", Toast.LENGTH_SHORT).show()
            } else {
                //User isn't verified
                //show confirmatiojn dialog before verification
                emailVerificationDialog()
            }
        }

    }

    private fun emailVerificationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify Email")
            .setMessage("Are you sure you want to send email verification instructions to your email ${firebaseUser.email}")
            .setPositiveButton("Send") {d, e ->
                sendEmailVerification()
            }
            .setNegativeButton("Cancel") {d, e ->
                d.dismiss()
            }
            .show()


    }

    private fun sendEmailVerification() {
        //show progress dialog
        progressDialog.setMessage("Sending email verification instruction to email ${firebaseUser.email}")
        progressDialog.show()

        //send instructions
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener {
                //successfully
                progressDialog.dismiss()
                Toast.makeText(this, "Instruction sent check your email ${firebaseUser.email}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to send due to ${e.message}", Toast.LENGTH_SHORT).show()
                //failed to send
            }
    }

    private fun loadUserInfo() {
        //check if user is verified or not, changes may effect after login when you verify  email
        if(firebaseUser.isEmailVerified) {
            binding.accountStatusTv.text = "Verified"
        } else {
            binding.accountStatusTv.text = "Not Verified"
        }

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    val uid = "${snapshot.child("uid").value}"
                    val userType = "${snapshot.child("userType").value}"

                    //convert timestamp to peroper date format
                    val formattedDate = MyApplication.formatTimeStamp(timestamp.toLong())
                    //set data
                    binding.nameTv.text = name
                    binding.emailTv.text = email
                    binding.memberDateTv.text = formattedDate
                    binding.accountTypeTv.text = userType
                    //set image
                    try {
                        Glide.with(this@ProfileActivity).load(profileImage)
                            .placeholder(R.drawable.ic_baseline_person_24).into(binding.profileIv)
                    } catch (e: Exception) {

                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun loadFavoriteBooks() {
        //inti arrayList
        booksArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites")
                .addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                            //clear arraylist, before starting adding data
                        booksArrayList.clear()
                        for (ds in snapshot.children) {
                            //get only if of the book , rest of the info wahve loaded in adapter class
                            val bookId = "${ds.child("bookId").value}"
                            //set to adapter
                            val modelPdf = ModelPdf()
                            modelPdf.id = bookId

                            booksArrayList.add(modelPdf)
                        }
                        //set number of favorite books
                        binding.favoriteBookCountTv.text = "${booksArrayList.size}"
                        adapterPdfFavorite = AdapterPdfFavorite(this@ProfileActivity, booksArrayList)
                        binding.favoriteRv.adapter = adapterPdfFavorite
                    }
                    override fun onCancelled(error: DatabaseError) {

                    }
                })

    }
}