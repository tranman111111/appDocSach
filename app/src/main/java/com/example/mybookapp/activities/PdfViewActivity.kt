package com.example.mybookapp.activities

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.mybookapp.Constants
import com.example.mybookapp.databinding.ActivityPdfViewBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream

class PdfViewActivity : AppCompatActivity() {

    private var currentPageNumber = 0
    private var pageCountSum = 0
    private lateinit var currentPage: PdfRenderer.Page
    private lateinit var binding: ActivityPdfViewBinding

    //Book id
    var bookId = ""

    //TAG
    private companion object {
        const val TAG = "PDF_VIEW_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get book id from intent
        bookId = intent.getStringExtra("bookId")!!
        loadBookDetails()

        //handle click, goback

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

    }

    private fun loadBookDetails() {
        Log.d(TAG, "LoadbookDetails: Get Pdf url from db")
        //get book url using book id (database reference)
        //step 1 : Get book url
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get book url
                    val pdfUrl = "${snapshot.child("url").value}"
                    Log.d(TAG, "Ondatachange: PDF_URL $pdfUrl")
                    //step 2 load pdf using url from firebase storage
                    loadBookFromUrl(pdfUrl)
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

    }

    private fun loadBookFromUrl(pdfUrl: String) {
        Log.d(TAG, "loadBookFromUrl: Get pdf from firebase storage using URL")

        val reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl)
        reference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "loadBookFromUrl: pdf got from url")
                //load pdf
                val file = File.createTempFile("pdf", "pdf")
                file.deleteOnExit()
                val fos = FileOutputStream(file)
                fos.write(bytes)
                fos.flush()
                fos.close()
                val fileDescriptor =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fileDescriptor)
                currentPage = pdfRenderer.openPage(0)
                currentPageNumber = currentPage.index + 1
                pageCountSum = pdfRenderer.pageCount
                val bitmap = Bitmap.createBitmap(
                    currentPage.width,
                    currentPage.height,
                    Bitmap.Config.ARGB_8888
                )
                currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                binding.progressBar.visibility = View.GONE
                binding.optionPdfLl.visibility = View.VISIBLE
                binding.toolbarSubtitleTv.text = "$currentPageNumber/$pageCountSum"
                binding.pdfView.setImageBitmap(bitmap)

                // Add a listener for the next and previous buttons
                binding.previousButton.setOnClickListener {
                    if (currentPageNumber > 1) {
                        currentPageNumber--
                        showPage(
                            currentPageNumber,
                            pdfRenderer,
                            binding.pdfView,
                            binding.toolbarSubtitleTv
                        )
                    }
                }
                binding.nextButton.setOnClickListener {
                    if(currentPageNumber == pdfRenderer.pageCount -1) {
                        Toast.makeText(this, "End",Toast.LENGTH_SHORT).show()
                    } else if (currentPageNumber < pdfRenderer.pageCount - 1) {
                        currentPageNumber++
                        showPage(
                            currentPageNumber,
                            pdfRenderer,
                            binding.pdfView,
                            binding.toolbarSubtitleTv
                        )
                    }
                }
//                currentPage.close()
//                pdfRenderer.close()
//                fileDescriptor.close()
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "loadBookFromUrl:Fail to get url due to ${e.message}")

            }
    }

    private fun showPage(
        pageNumber: Int,
        pdfRenderer: PdfRenderer,
        pdfView: ImageView,
        toolbarSubtitleTv: TextView
    ) {
        currentPage.close()
        currentPage = pdfRenderer.openPage(pageNumber)
        val bitmap =
            Bitmap.createBitmap(currentPage.width, currentPage.height, Bitmap.Config.ARGB_8888)
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        toolbarSubtitleTv.text = "$currentPageNumber/$pageCountSum"
        pdfView.setImageBitmap(bitmap)
    }


}