package com.example.mybookapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.mybookapp.MyApplication
import com.example.mybookapp.activities.PdfDetailActivity
import com.example.mybookapp.databinding.RowPdfFavoriteBinding
import com.example.mybookapp.models.ModelPdf
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterPdfFavorite : Adapter<AdapterPdfFavorite.HolderPdfFavorite> {

    //Context
    private val context: Context
    //Arraylist to hold books
    private val booksArrayList: ArrayList<ModelPdf>
    //constructor
    //view binding
    private lateinit var binding: RowPdfFavoriteBinding

    constructor(context: Context, booksArrayList: ArrayList<ModelPdf>) {
        this.context = context
        this.booksArrayList = booksArrayList
    }
    //view holder class to manage UI views of row_pdf_favorite.xml
    inner class HolderPdfFavorite(itemView: View): ViewHolder(itemView) {
        //init ui views
        var pdfView = binding.pdfView
        var progressBar = binding.progressBar
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var removeFavBtn = binding.removeFavBtn
        var categoryTv =  binding.categoryTv
        var dateTv = binding.dateTv
        var sizeTv = binding.sizeTv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfFavorite {
        //bind/inflate row_pdf_favorite.xml
        binding = RowPdfFavoriteBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfFavorite(binding.root)
    }

    override fun onBindViewHolder(holder: HolderPdfFavorite, position: Int) {
//        get data, set data, handle click
        val model = booksArrayList[position]
        loadBookDetails(model, holder)
        //handle click, open pdf details page, pass book id to load details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfDetailActivity::class.java)
            intent.putExtra("bookId", model.id) // pass book id to id
            context.startActivity(intent)
        }

        //handle click , remove from favorite
        holder.removeFavBtn.setOnClickListener {
            MyApplication.removeFromFavorite(context, model.id)
        }
    }

    private fun loadBookDetails(model: ModelPdf, holder: HolderPdfFavorite) {
        val bookId = model.id

        val ref = FirebaseDatabase.getInstance().getReference("Books")
            ref.child(bookId)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                         val categoryId = "${snapshot.child("categoryId").value}"
                         val description = "${snapshot.child("description").value}"
                         val downloadsCount = "${snapshot.child("downloadsCount").value}"
                         val timestamp = "${snapshot.child("timestamp").value}"
                         val title = "${snapshot.child("title").value}"
                         val uid = "${snapshot.child("uid").value}"
                         val url = "${snapshot.child("url").value}"
                         val viewsCount = "${snapshot.child("viewsCount").value}"

                        //set data to model
                        model.isFavorite = true
                        model.title = title
                        model.description = description
                        model.categoryId = categoryId
                        model.timestamp = timestamp.toLong()
                        model.uid = uid
                        model.url = url
                        model.viewsCount = viewsCount.toLong()
                        model.downloadsCount = downloadsCount.toLong()


                        //format date
                         val date = MyApplication.formatTimeStamp(timestamp.toLong())
                        MyApplication.loadCategory("${categoryId}", holder.categoryTv)
                        MyApplication.loadPdfFromUrlSinglePage("$url", "$title", holder.pdfView, holder.progressBar, null)
                        MyApplication.loadPdfSize("$url", "$title", holder.sizeTv)

                        holder.titleTv.text = title
                        holder.descriptionTv.text = description
                        holder.dateTv.text = date
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
    }

    override fun getItemCount(): Int {
        return booksArrayList.size//return size of list|number of items in list
    }

}