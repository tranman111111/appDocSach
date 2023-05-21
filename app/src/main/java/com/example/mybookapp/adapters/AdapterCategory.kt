package com.example.mybookapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.mybookapp.filters.FilterCategory
import com.example.mybookapp.models.ModelCategory
import com.example.mybookapp.activities.PdfListAdminActivity
import com.example.mybookapp.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterCategory:Adapter<AdapterCategory.HolderCategory>, Filterable {

    private lateinit var binding: RowCategoryBinding

    private val context: Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory>
    private var filter: FilterCategory? = null

    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList = categoryArrayList
    }


    // viewholder class to hold/init ui view for row_category
    inner class HolderCategory(itemView: View): ViewHolder(itemView) {
        // init ui views
        var categoryTv: TextView = binding.categoryTv
        var deleteBtn: ImageButton = binding.deleteBtn

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflate bind row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent , false)
        return HolderCategory(binding.root)
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        //Get data, set data, handle click
        //get Data

        val model = categoryArrayList[position]
        val id = model.id
        val category = model.category
        val uid = model.uid
        val timestamp = model.timestamp
        // set Data

        holder.categoryTv.text = category
        //Hanld click
        holder.deleteBtn.setOnClickListener {
            //Confirm before delete
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Delete")
                .setMessage("Are you sure want to delete this category")
                .setPositiveButton("Confirm") { a, d ->
                   Toast.makeText(context, "Deleting....", Toast.LENGTH_SHORT).show()
                   deleteCategory(model)
                }
                .setNegativeButton("Cancel") { a, d ->
                    a.dismiss()
                }
                .show()
        }

        // handle click, start pdf list admin activity
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PdfListAdminActivity::class.java)
            intent.putExtra("categoryId", id)
            intent.putExtra("category", category)

            context.startActivity(intent)

        }

    }

    private fun deleteCategory(model: ModelCategory) {
     // get id of category to delete
        val  id = model.id
        // Firebase db > Categories > categoryid
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted....", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Unable to Delete....", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = categoryArrayList.size // number of items in list
    override fun getFilter(): Filter {
        if(filter == null) {
            filter = FilterCategory(filterList, this)
        }
        return filter as FilterCategory
    }
}