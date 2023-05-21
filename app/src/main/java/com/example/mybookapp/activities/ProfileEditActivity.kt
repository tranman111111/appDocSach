package com.example.mybookapp.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.mybookapp.MyApplication
import com.example.mybookapp.R
import com.example.mybookapp.databinding.ActivityProfileEditBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ProfileEditActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityProfileEditBinding
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //image url (which we will pick
    private var imageUri: Uri? = null

    //progress dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        loadUserInfo()

        //handle click, go back

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, pick image from camera/galerry
        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }

        binding.updateBtn.setOnClickListener {
            validateData()
        }

    }

    private var name = ""

    private fun validateData() {
        //get data
        name = binding.nameEt.text.toString().trim()

        //validate
        if(name.isEmpty()) {
            // name not entered
            Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show()
        } else {
            //name is entered
            if(imageUri == null) {
                //update withoud page
                updateProfile("")
            } else {
                //need to update with image
                uploadImage()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.setMessage("Uploading profile image")
        progressDialog.show()
        //image path and name use uid to replace previous
        val filePathAndName = "ProfileImages/"+firebaseAuth.uid

        //storage reference
        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"

                updateProfile(uploadedImageUrl)

            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failded to upload image due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Updating profile")
        //set up info to update to db
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["name"] = "$name"
        if(imageUri != null) {
            hashMap["profileImage"] = uploadedImageUrl
        }
        //upload to db
        val refer = FirebaseDatabase.getInstance().getReference("Users")
        refer.child(firebaseAuth.uid!!)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failded to upload profile due to ${e.message}", Toast.LENGTH_SHORT).show()

            }
    }

    private fun loadUserInfo() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"

                    //set data
                    binding.nameEt.setText(name)

                    //set image
                    try {
                        Glide.with(this@ProfileEditActivity).load(profileImage)
                            .placeholder(R.drawable.ic_baseline_person_24).into(binding.profileIv)
                    } catch (e: Exception) {

                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun showImageAttachMenu() {
//        Show popup menu with options camera, gallery to pick image
        //set up popup menu
        val popupMenu = PopupMenu(this, binding.profileIv)
        popupMenu.menu.add(Menu.NONE, 0, 0, "Camera")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Gallery")
        popupMenu.show()

        //handle popup menu item click

        popupMenu.setOnMenuItemClickListener { item ->
            //get id from click item
            val id = item.itemId
            if(id == 0) {
                // Camera click
                pickImageCamera()
            } else if(id == 1) {
                // Gallery click
                pickImageGallery()
            }

            true
        }
    }

    private fun pickImageCamera() {
        //intent to pick image from camera
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGallery() {
       //intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {result ->
            if(result.resultCode == Activity.RESULT_OK) {
                val data = result.data
//                imageUri = data!!.data no need we already have image in imageurl in camera case
                //set to imageview
                binding.profileIv.setImageURI(imageUri)
            } else {
                //canceled
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )
    //used to handle result of camera intent(new way in replacement of start activity for result
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if(result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data!!.data
                //set to imageview
                binding.profileIv.setImageURI(imageUri)
            } else {
                //canceled
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )


}