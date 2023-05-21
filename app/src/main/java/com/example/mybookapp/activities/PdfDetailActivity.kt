package com.example.mybookapp.activities

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.mybookapp.databinding.ActivityPdfDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream
import android.Manifest
import android.app.AlertDialog
import android.view.LayoutInflater
import com.example.mybookapp.Constants
import com.example.mybookapp.MyApplication
import com.example.mybookapp.R
import com.example.mybookapp.adapters.AdapterComment
import com.example.mybookapp.databinding.DialogCommentAddBinding
import com.example.mybookapp.models.ModelComment
import com.google.firebase.auth.FirebaseAuth

class PdfDetailActivity : AppCompatActivity() {

    //view binding
    private lateinit var binding: ActivityPdfDetailBinding
//    private val REQUEST_CODE_STORAGE = 1

    private companion object {
        //TAG
        const val TAG = "BOOK_DETAILS_TAG"
    }

    //get book id from intent
    private var bookId: String = ""

    //get from firebase
    private var bookTitle: String = ""
    private var bookUrl: String = ""
    // will hold a boolean value false/true to indicate either is in current user's favorite llist or not
    private var isInMyFavorite = false

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    private lateinit var commentArrayList: ArrayList<ModelComment>

    private lateinit var adapterComment: AdapterComment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get Book id from intent
        bookId = intent.getStringExtra("bookId")!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser != null) {
            //user is loggedin
            Log.d(TAG, "Load automatic")
            checkIsFavorite()
        }

        //increate book view count
        MyApplication.increamentBookViewCount(bookId)
        loadBookDetails()
        showComments()

        //handle backbutton click
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, open pdf activity
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        //handle click, download book/pdf
        binding.downloadBookBtn.setOnClickListener {
            //first check storage permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Oncreate: Storage permission is granted")
                downloadBook()
            } else {
                Log.d(TAG, "Oncreate storage permission was now granted")
                requestStoragePermissionLaucher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        //handle/click add remove favorite
        binding.favoriteBtn.setOnClickListener {
           //we can add only if user is logged in
            //check if user is logged in or not
            if(firebaseAuth.currentUser == null) {
                //user not logged in, cant do favorite functionality
                Toast.makeText(this, "You're not logged in", Toast.LENGTH_SHORT).show()
            } else {
                //user is logged in, we can do favarite functionality
                if (isInMyFavorite) {
                    //already in fav, remove
                    MyApplication.removeFromFavorite(this, bookId)
                } else {
                    addToFavorite()
                }
            }
        }

        //handle click, show add comment dialog
        binding.addCommentBtn.setOnClickListener {
//            To add a commnet, user must be logged in, if not just show a message, you're not logged in
            if(firebaseAuth.currentUser == null) {
                //user not logged, don't allow adding comment
                Toast.makeText(this, "You're not logged in", Toast.LENGTH_SHORT).show()
            } else {
                //user logged in, allow adding commnet
                addCommentDialog()
            }
        }
    }

    private fun showComments() {
        //init
        commentArrayList = ArrayList()
        //db padth to load comments
        val ref = FirebaseDatabase.getInstance().getReference("Books")
             ref.child(bookId).child("Comments")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentArrayList.clear()
                    for (ds in snapshot.children) {
                        //get data model, be careful of spelling and data type
                        val model = ds.getValue(ModelComment::class.java)
                        //add to list
                        Log.d("Detailactivy", "${model}")
                        commentArrayList.add(model!!)
                    }
                    //set up adapter
                    adapterComment =  AdapterComment(this@PdfDetailActivity, commentArrayList)
                    //set adapter to recyleview
                    binding.commentsRv.adapter = adapterComment
                }

                //

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private var comment = ""

    private fun addCommentDialog() {
        //inflate/bind view for dialog_commnet-add.xml
        val commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this))
        //set up alert dialog
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentAddBinding.root)
        //create and show alert dialog
        val alertDialog = builder.create()
        alertDialog.show()
        //handle click, dismis dialog
        commentAddBinding.backBtn.setOnClickListener {alertDialog.dismiss() }
        //handle lcick, add comment
        commentAddBinding.submitBtn.setOnClickListener {
            //get data
            comment = commentAddBinding.commentEt.text.toString().trim()
            //validate data
            if(comment.isEmpty()) {
                Toast.makeText(this, "Enter comment...", Toast.LENGTH_SHORT).show()
            } else {
                alertDialog.dismiss()
                addComment()
            }
        }
    }

    private fun addComment() {
        //show progress
        progressDialog.setMessage("Adding comment")
        progressDialog.show()
        //timestamp for comment id, comment timestamp
        val timestamp = "${System.currentTimeMillis()}"
        //setup data to add in db for comment
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["bookId"] = "$bookId"
        hashMap["timestamp"] = "$timestamp"
        hashMap["comment"] = "$comment"
        hashMap["uid"] = "${firebaseAuth.uid}"

        //Db path to add data into it

        //Books > bookid > Comments > commentId > CommentData
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Comment Added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Faild to add comment ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIsFavorite() {
        Log.d(TAG, "checkIsFavorite: Check if book is in fav or not")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "LOAD IN VALUE EVENT LISTENER")
                    isInMyFavorite = snapshot.exists()
                    if(isInMyFavorite) {
                        //available in favorite
                        Log.d(TAG, "onDatachange")
                        //set drable top icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_baseline_favorite_24, 0, 0)
                        binding.favoriteBtn.text = "Remove Favorite"
                    } else {
                        //not available in favorite
                        //set drable top icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0,
                            R.drawable.ic_baseline_favorite_border_24, 0, 0)
                        binding.favoriteBtn.text = "Add Favorite"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun addToFavorite() {
        Log.d(TAG, "addtofavorite: Adding to fav")
        val timestamp = System.currentTimeMillis()
        //setup data to add in db
        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp
        //save to db
        val ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                //added to fav
                Log.d(TAG, "addtofavorite: Adding to fav")
            }
            .addOnFailureListener { e ->
                //failed to add to fav
                Log.d(TAG, "addtofavorite: Faild to add to fav due to ${e.message}")
                Toast.makeText(this, "Faild to add to fav due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }




    val requestStoragePermissionLaucher =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            //lets check if granted or not
            if (isGranted) {
                Log.d(TAG, "oncreate: Storage permission is granted")
                downloadBook()
            } else {
                Log.d(TAG, "oncreate storage permission is denied")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun downloadBook() {
        //progressbar
        progressDialog.setMessage("Downloading Book")
        progressDialog.show()
        //letdownload book from firebase storage using url
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "Download Book: Book downloaded")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.d(TAG, "Failt to download due to ${e.message}")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {
        Log.d(TAG, "savetodownloadsfolder:saving")
        val nameWithExtension = "${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            //create folder if not
            downloadsFolder.mkdir()

            val filePath = downloadsFolder.path + "/" + nameWithExtension
            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "Saved to Downloads Folder", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            incrementDownloadCount()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save due to ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        //increment downloads count to firebase db
        Log.d(TAG, "increment Download Count")
        //get PreviesDownload
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get downloads count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDatachange: Current Downloads Count: $downloadsCount")

                    if (downloadsCount == "" || downloadsCount == "null") {
                        downloadsCount = "0"
                    }

                    //convert to log and increment 1
                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    //setup data to update to db
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount
                    //update new increment downloads count to db
                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: Downloads count incremented")
                        }
                        .addOnFailureListener {e ->
                            Log.d(TAG, "onDataChange: Faild Downloads ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }


    private fun loadBookDetails() {
        //Books > bookId > Details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    //load cateogry
                    MyApplication.loadCategory(categoryId, binding.categoryTv)
                    //load pdf thumnail pages count
                    MyApplication.loadPdfFromUrlSinglePage(
                        "$bookUrl",
                        "$bookTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )
                    //load pdf size
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)
                    //set data
                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }


}