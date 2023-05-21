package com.example.mybookapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.mybookapp.adapters.AdapterPdfAdmin
import com.example.mybookapp.databinding.ActivityPdfListAdminBinding
import com.example.mybookapp.models.ModelPdf
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PdfListAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfListAdminBinding

    companion object {
        private val TAG = "PDF LIST ADMIN"
    }
    //category id, title
    private var categoryId = ""
    private var category = ""

    //arraylist to hold books
    private lateinit var pdfArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfAdmin: AdapterPdfAdmin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
    // get from itent
        val intent = intent
        categoryId = intent.getStringExtra("categoryId")!!
        category = intent.getStringExtra("category")!!
        //set pdf category
        binding.subTitleTv.text = category
        //load pdf/books
        loadPdfList()
        //search
        binding.searchEt.addTextChangedListener(
            object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                try {
                    Log.d(TAG, "${s}")
                    adapterPdfAdmin.filter.filter(s)
                } catch (e: Exception) {
                   Log.d(TAG, "${e.message}")
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        //handle click, gobback
        binding.backBtn.setOnClickListener {
             onBackPressed()
        }
    }

    private fun loadPdfList() {
       //init arraylist
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    pdfArrayList.clear()
                    for (ds in snapshot.children) {
                        var model = ds.getValue(ModelPdf::class.java)
                        pdfArrayList.add(model!!)
                        Log.d(TAG, "onDatachange: ${model.title}")
                    }
                    //setup adapter
                    adapterPdfAdmin = AdapterPdfAdmin(this@PdfListAdminActivity, pdfArrayList)
                    binding.booksRv.adapter = adapterPdfAdmin
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
    }
}