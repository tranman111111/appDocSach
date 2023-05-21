package com.example.mybookapp.activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.mybookapp.databinding.ActivityPdfAddBinding
import com.example.mybookapp.models.ModelCategory
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


class PdfAddActivity : AppCompatActivity() {
    // set up viewbinding
    private lateinit var binding: ActivityPdfAddBinding
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    // arraylist to hold pdf category
    private lateinit var categoryArrayList: ArrayList<ModelCategory>

    //uri of picked pdf

    private var pdfUri: Uri? = null

    //Tag
    private val TAG = "PDF_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        loadPdfCategories()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //hand click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, show category pick dialog
        binding.categoryTv.setOnClickListener {
            categoryPickDialog()
        }

        //handle click, pick pdf

        binding.attachPdfBtn.setOnClickListener {
            pdfPickIntent()
        }

        //handle click, start uploading
        binding.submitBtn.setOnClickListener {
          validateData()
        }
    }

    private var title = ""
    private var description = ""
    private var category = ""

    private fun validateData() {
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()
        category = binding.categoryTv.text.toString().trim()

        if(title.isEmpty()) {
            Toast.makeText(this, "Enter title...", Toast.LENGTH_SHORT).show()
        } else if(description.isEmpty()) {
            Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show()
        } else if(category.isEmpty()) {
            Toast.makeText(this, "Pick category...",Toast.LENGTH_SHORT).show()
        } else if(pdfUri == null) {
            Toast.makeText(this, "Pick PDF", Toast.LENGTH_SHORT).show()
        }
        else {
            uploadPdfToStorage()
        }
    }

    private fun uploadPdfToStorage() {
       Log.d(TAG, "UploadpdftoStorage")

        progressDialog.setMessage("Uploating PDF...")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val filePathAndName = "Books/$timestamp"
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener { taskSnapshot ->
              var uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                // Must check statement
                while (!uriTask.isSuccessful);
                val uploadedPdfUrl = "${uriTask.result}"
                uploadPdfInfoToDb(uploadedPdfUrl, timestamp)
            }
            .addOnFailureListener {
                Log.d(TAG, "Upload Pdf to Storage failed")
                progressDialog.dismiss()
            }
    }

    private fun uploadPdfInfoToDb(uploadedPdfUrl: String, timestamp: Long) {
        progressDialog.setMessage("Uploading pdf info...")
        val uid = firebaseAuth.uid
        //set up data to upload
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"
        hashMap["url"] = "$uploadedPdfUrl"
        hashMap["timestamp"] = timestamp
        hashMap["viewsCount"] = 0
        hashMap["downloadsCount"] = 0

        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Success to upload", Toast.LENGTH_SHORT).show()
                pdfUri = null
            }
            .addOnFailureListener {e->
               progressDialog.dismiss()
                Toast.makeText(this, "Faild to upload due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPdfCategories() {
        categoryArrayList = ArrayList()
        //db reference to load categories

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                // clear list first
                categoryArrayList.clear()
                for (ds in snapshot.children) {
                    //getdata
                    val model = ds.getValue(ModelCategory::class.java)
                    //add to arraylist
                    categoryArrayList.add(model!!)
                    Log.d(TAG, "OMDATACHANGE: ${model.category}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""

    private fun categoryPickDialog() {
        //get String array
        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices) {
            categoriesArray[i] = categoryArrayList[i].category
        }

        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pick Category")
            .setItems(categoriesArray) { dialog, which->
             //handle item click
             // geet clicked item
             selectedCategoryTitle = categoryArrayList[which].category
             selectedCategoryId = categoryArrayList[which].id
             //set category to textview
             binding.categoryTv.text = selectedCategoryTitle

             Log.d(TAG, "$selectedCategoryTitle")
            }
            .show()
    }

    private fun pdfPickIntent() {
        val intent = Intent()
        intent.type =  "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfActivityResultLauncher.launch(intent)
    }

    var pdfActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult>{ result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "PDF PICKED")
                pdfUri = result.data!!.data
            } else {
                Log.d(TAG, "PDF Cancelled")
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )
}