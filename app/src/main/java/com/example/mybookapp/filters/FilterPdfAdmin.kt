package com.example.mybookapp.filters

import android.widget.Filter
import com.example.mybookapp.adapters.AdapterPdfAdmin
import com.example.mybookapp.models.ModelPdf

//used to filter data from recycleview | search pdf from pdf list in recycleview
class FilterPdfAdmin:Filter {
    //arraylist in which we want to filter
    var filterList: ArrayList<ModelPdf>
    //adapter in which filter need to implemmented
    var adapterPdfAdmin: AdapterPdfAdmin

    //constructor
    constructor(filterList: ArrayList<ModelPdf>, adapterPdfAdmin: AdapterPdfAdmin) {
        this.filterList = filterList
        this.adapterPdfAdmin = adapterPdfAdmin
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint: CharSequence? = constraint
        val results = FilterResults()
        //value to be searched should not be null and not empty
        if(constraint != null && constraint.isNotEmpty()) {
            //change to upper case, or lowercase to avoid case sensitivity
            constraint = constraint.toString().lowercase()
            val filteredModels = ArrayList<ModelPdf>()
            for (i in filterList.indices) {
                //validate if match
                if(filterList[i].title.lowercase().contains(constraint)) {
                    //search value is similar to value in list, add to filter list
                    filteredModels.add(filterList[i])
                }
            }
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            //search value is either null or empty, return all data
            results.count = filterList.size
            results.values = filterList
        }

        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        //apply filter changes
        adapterPdfAdmin.pdfArrayList = results.values as ArrayList<ModelPdf>
        //notifychanges
        adapterPdfAdmin.notifyDataSetChanged()
    }


}