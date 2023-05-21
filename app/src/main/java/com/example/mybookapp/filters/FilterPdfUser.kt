package com.example.mybookapp.filters

import android.util.Log
import android.widget.Filter
import com.example.mybookapp.adapters.AdapterPdfUser
import com.example.mybookapp.models.ModelPdf

class FilterPdfUser: Filter {
    //arraylist in which we want to search
    var filterList: ArrayList<ModelPdf>
    //adapter in which need to implemented
    var adapterPdfUser: AdapterPdfUser

    companion object {
        private val TAG = "FILTER_PDF_USER"
    }

    //constructor
    constructor(filterList: ArrayList<ModelPdf>, adapterPdfUser: AdapterPdfUser) : super() {
        this.filterList = filterList
        this.adapterPdfUser = adapterPdfUser
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()
        try {
            var constraint = constraint
            Log.d(TAG, "Search with ${constraint}")
//            val results = FilterResults()
            //value to be search should not be null and not empty
            if (constraint != null && constraint.isNotEmpty()) {
                //not null nor empty
                //change to upper case or lower case to remove case sensitivity
                constraint = constraint.toString().uppercase()
                val filterModels = ArrayList<ModelPdf>()
                for (i in filterList.indices) {
                    if (filterList[i].title.uppercase().contains(constraint)) {
                        //search value
                        filterModels.add(filterList[i])
                    }
                }
                results.count = filterModels.size
                results.values = filterModels
            } else {
                //either it's null or is empty
                //return original list and size
                results.count = filterList.size
                results.values = filterList
            }
            return results
        } catch (e: Exception) {
            Log.d(TAG, "Search exception ${e.message}")
        }
        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        //apply filter changes
        adapterPdfUser.pdfArrayList = results.values as ArrayList<ModelPdf>
        //notify changes
        adapterPdfUser.notifyDataSetChanged()

    }
}