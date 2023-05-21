package com.example.mybookapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.mybookapp.databinding.ActivityPdfEditBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PdfEditActivity : AppCompatActivity() {
    //view binding

    private lateinit var binding: ActivityPdfEditBinding

    private companion object {
        private const val TAG = "PDF_EDIT"
    }

    private var bookId = ""

    private lateinit var progressDialog: ProgressDialog

    private lateinit var categoryTitleArrayList:ArrayList<String>

    private lateinit var categoryIdArrayList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra("bookId")!!

        //set up progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        loadCategories()
        loadBookInfo()

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.categoryTv.setOnClickListener {
            categoryDialog()
        }

        binding.submitBtn.setOnClickListener {
            validateData()
        }

    }

    private fun loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Loading book info")
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book info
                    selectedCategoryId = snapshot.child("categoryId").value.toString()
                    val description = snapshot.child("description").value.toString()
                    val title = snapshot.child("title").value.toString()
                    //set to views
                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)
                    //load book category using category Id
                    Log.d(TAG, "Ondatachange: loading book category Info")
                    val refBookCategory = FirebaseDatabase.getInstance().getReference("Categories")
                    refBookCategory.child(selectedCategoryId)
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                            override fun onDataChange(snapshot: DataSnapshot) {
                               //get category
                                val category = snapshot.child("category").value
                               //set to textview
                                binding.categoryTv.text = category.toString()
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }

                        })

                }

                override fun onCancelled(error: DatabaseError) {
                }


            })
    }

    private var title = ""
    private var description = ""

    private fun validateData() {
        //get data
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()

        //validate data
        if(title.isEmpty()) {
            Toast.makeText(this, "Enter title",Toast.LENGTH_SHORT).show()
        } else if(description.isEmpty()) {
            Toast.makeText(this, "Enter description", Toast.LENGTH_SHORT).show()
        } else if(selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Pick category", Toast.LENGTH_SHORT).show()
        } else {
            updatePdf()
        }
    }

    private fun updatePdf() {
        Log.d(TAG,"Update Pdf: Starting updateing pdf info")
        //show progress
        progressDialog.setMessage("Updating book info")
        progressDialog.show()
        //set up data to update to db
        val hashMap =  HashMap<String, Any>()
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["categoryId"] = "$selectedCategoryId"

        //start updating
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
               Log.d(TAG, "updatePdf: Updated successfully")
                Toast.makeText(this, "Success to update", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {
               progressDialog.dismiss()
                Toast.makeText(this, "failded to update due to", Toast.LENGTH_SHORT).show()
            }

    }


    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""

    private fun categoryDialog() {
//        Show dialog to pick
        val categoriesArray = arrayOfNulls<String>(categoryTitleArrayList.size)
        for (i in categoryTitleArrayList.indices) {
            categoriesArray[i] = categoryTitleArrayList[i]
        }
        //alert dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Category")
            .setItems(categoriesArray) {dialog, position ->
                //handle click, save clicked
                selectedCategoryId = categoryIdArrayList[position]
                selectedCategoryTitle = categoryTitleArrayList[position]
                //set to textview
                binding.categoryTv.text = selectedCategoryTitle
            }
            .show() // show dialog
    }

    private fun loadCategories() {
        Log.d(TAG, "loadCategories: Loading categories")
        categoryTitleArrayList = ArrayList()
        categoryIdArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                 categoryIdArrayList.clear()
                categoryTitleArrayList.clear()
                for (ds in snapshot.children) {
                    val id = "${ds.child("id").value}"
                    val category = "${ds.child("category").value}"

                    categoryIdArrayList.add(id)
                    categoryTitleArrayList.add(category)

                    Log.d(TAG, "onDataChange: Category ID $id")

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }
}