package com.example.mybookapp

import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.w3c.dom.Text
import java.io.File
import java.io.InputStream
import java.text.DateFormat
import java.util.*
import android.graphics.pdf.*
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import android.widget.Toast
import com.example.mybookapp.activities.PdfDetailActivity
import com.github.barteksc.pdfviewer.PDFView
import com.google.firebase.auth.FirebaseAuth
import java.io.FileOutputStream
import kotlin.collections.HashMap

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        // create a static method to convert timestamp to proper date format
        fun formatTimeStamp(timestamp: Long): String {
            val cal = Calendar.getInstance(Locale.ENGLISH)
            cal.timeInMillis = timestamp
            return android.text.format.DateFormat.format("dd/MM/yyyy", cal).toString()
        }

        fun loadPdfSize(pdfUrl: String, pdfTitle: String, sizeTv: TextView) {
            val TAG = "PDF_SIZE_TAG"

            //using url we can get file and its medata from firebase storage
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
            ref.metadata
                .addOnSuccessListener { storageMetadata ->
                    val bytes = storageMetadata.sizeBytes.toDouble()
                    //convert bytes to KB/MB
                    val kb = bytes / 1024
                    val mb = kb / 1024
                    if (mb >= 1) {
                        sizeTv.text = "${String.format("%.2f", mb)} MB"
                    } else if (kb >= 1) {
                        sizeTv.text = "${String.format("%.2f", kb)} KB"
                    } else {
                        sizeTv.text = "${String.format("%.2f", bytes)} bytes"
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "LoadPdfSize: Failed to get metada due to${e.message}")
                }

        }

        fun loadPdfFromUrlSinglePage(
            pdfUrl: String,
            pdfTile: String,
            pdfView: ImageView,
            progressBar: ProgressBar,
            pagesTv: TextView?
        ) {
            //using ur; we can get file and its metadata from firebase
            val TAG = "PDF_THUMBNAIL_TAG"
//            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
//
//            ref.getStream().addOnSuccessListener { streamTask ->
//                val stream = streamTask.stream
//                pdfView.fromStream(stream)
//                    .pages(0)
//                    .spacing(0)
//                    .swipeHorizontal(false)
//                    .enableSwipe(false)
//                    .onError { t ->
//                        progressBar.visibility = View.INVISIBLE
//                        Log.d(TAG, "${t.message}")
//                    }
//                    .onPageError { page, t ->
//                        progressBar.visibility = View.INVISIBLE
//                    }
//                    .onLoad { nbPages ->
//                        progressBar.visibility = View.INVISIBLE
//                        if (pagesTv != null) {
//                            pagesTv.text = "$nbPages"
//                        }
//                    }
//                    .load()
//
//            }.addOnFailureListener {
//                // Handle failure
//            }
//            val pdfFile = File(pdfUrl)
//            pdfView.fromFile(pdfFile)
//                .pages(0)
//                .spacing(0)
//                .swipeHorizontal(false)
//                .enableSwipe(false)
//                .onError { t ->
//                    progressBar.visibility = View.VISIBLE
//                    Log.d(TAG, "${t.message}")
//                }
//                .onPageError { page, t ->
//                    progressBar.visibility = View.INVISIBLE
//                }
//                .onLoad { nbPages ->
//                    progressBar.visibility = View.INVISIBLE
//                    if (pagesTv != null) {
//                        pagesTv.text = "$nbPages"
//                    }
//                }
//                .load()
//            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
//            ref.getStream()
//                .addOnSuccessListener{ stream  ->
//                pdfView.fromStream(stream)
//                    .pages(0)
//                    .spacing(0)
//                    .swipeHorizontal(false)
//                    .enableSwipe(false)
//                    .onError { t ->
//                        progressBar.visibility = View.VISIBLE
//                        Log.d(TAG, "${t.message}")
//                    }
//                    .onPageError { page, t ->
//                        progressBar.visibility = View.INVISIBLE
//                    }
//                    .onLoad { nbPages ->
//                        progressBar.visibility = View.INVISIBLE
//                        if (pagesTv != null) {
//                            pagesTv.text = "$nbPages"
//                        }
//                    }
//                    .load()
//            }.addOnFailureListener {
//                // Handle failure
//            }
//            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
//            ref.getBytes(Constants.MAX_BYTES_PDF)
//                .addOnSuccessListener { bytes ->
//                    //convert bytes to KB/MB
//                    //set to pdf view
//
//                    pdfView.fromBytes(bytes)
//                        .pages(0)//show first page only
//                        .spacing(0)
//                        .swipeHorizontal(false)
//                        .enableSwipe(false)
//                        .onError { t ->
//                            progressBar.visibility = View.INVISIBLE
//                            Log.d(TAG, "${t.message}")
//                        }
//                        .onPageError { page, t ->
//                            progressBar.visibility = View.INVISIBLE
//                            Log.d(TAG, "${t.message}")
//
//                        }
//                        .onLoad { nbPages ->
//                            //pdf loaded, we can set page count, pdf thumbnail
//                            progressBar.visibility = View.INVISIBLE
//                            //if(pagesTv is not null then set page number
//                            if (pagesTv != null) {
//                                pagesTv.text = "$nbPages"
//                            }
//                        }
//                        .load()
//                }
//                .addOnFailureListener { e ->
//                    Log.d(TAG, "LoadPdfSize: Failed to get metada due to${e.message}")
//                }

            val storage = FirebaseStorage.getInstance()
            val pdfRef = storage.getReferenceFromUrl(pdfUrl)
            pdfRef.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener { bytes ->
                    // Use PdfRenderer to display the PDF using the bytes
                    val file = File.createTempFile("pdf", "pdf")
                    file.deleteOnExit()
                    val fos = FileOutputStream(file)
                    fos.write(bytes)
                    fos.flush()
                    fos.close()
                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(fileDescriptor)
                    val currentPage = pdfRenderer.openPage(0)
                    val bitmap = Bitmap.createBitmap(currentPage.width, currentPage.height, Bitmap.Config.ARGB_8888)
                    currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    progressBar.visibility = View.INVISIBLE
                    if (pagesTv != null) {
                        pagesTv.text = "${pdfRenderer.pageCount}"
                    }
                    pdfView.setImageBitmap(bitmap)
                    currentPage.close()
                    pdfRenderer.close()
                    fileDescriptor.close()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.INVISIBLE
                    Log.d(TAG, "LoadPdfSize: Failed to get metada due to${e.message}")
                }
        }

        fun loadCategory(categoryId: String, categoryTv: TextView) {
            //loadcategory using category id from firebase
            val ref = FirebaseDatabase.getInstance().getReference("Categories")
            ref.child(categoryId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get category
                        val category: String = "${snapshot.child("category").value}"
                        // set category
                        categoryTv.text = category
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })
        }

        fun deleteBook(context: Context, bookId: String, bookUrl: String, bookTitle: String) {
            //para details
            //1) context used when require for progress dialog, toast
            //2)book Id to delete book from fb
            //3)book Url, delete book from firebase storage
            //4)booktitle, show in dialog etc
            val TAG = "Delete_BOOK TAG"
            Log.d(TAG, "delete Book: deleting")
            //progress dialog
            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Please wait")
            progressDialog.setMessage("Deleting ${bookTitle}")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            Log.d(TAG, "Delete Book: Deleting from storage")
            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
            storageReference.delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Delete book: Deleted from storage")
                    Log.d(TAG, "Delete book: deleting from db now")
                    val ref = FirebaseDatabase.getInstance().getReference("Books")
                    ref.child(bookId)
                        .removeValue()
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            Toast.makeText(context, "Success to delete", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e->
                            progressDialog.dismiss()
                            Toast.makeText(context, "failed to delete", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                   progressDialog.dismiss()
                    Toast.makeText(context, "failed to delete", Toast.LENGTH_SHORT).show()
                }

        }

        fun increamentBookViewCount(bookId: String) {
            //get current book views count
            val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //get views count
                        var viewsCount = "${snapshot.child("viewsCount").value}"
                        if(viewsCount == "" || viewsCount == "null") {
                            viewsCount = "0"
                        }
                        // increment views count
                        val newViewsCount = viewsCount.toLong() + 1
                        //set up data to update in db
                        val hashMap = HashMap<String, Any>()

                        hashMap["viewsCount"] = newViewsCount
                        //set to db
                        val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                        dbRef.child(bookId)
                            .updateChildren(hashMap)
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
        }

         fun removeFromFavorite(context: Context, bookId: String) {
            val TAG = "REMOVE_FAV_TAG"
            val firebaseAuth = FirebaseAuth.getInstance()
            Log.d(TAG, "removefromfavorite: Removinging from fav")
            //database ref
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
                .removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "removefromfavorite: Removinging from fav")
                    Toast.makeText(context, " Removing from fav", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "removefromfavorite: Failded Removinging from fav ${e.message}")
                    Toast.makeText(context, "Faild to remove from fav due to ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


}