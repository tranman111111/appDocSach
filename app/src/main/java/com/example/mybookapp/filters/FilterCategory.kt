package com.example.mybookapp.filters

import android.widget.Filter
import com.example.mybookapp.adapters.AdapterCategory
import com.example.mybookapp.models.ModelCategory

class FilterCategory: Filter {
    // arraylist we want to search
    private var filderList: ArrayList<ModelCategory>

    //adapter filder need to implement
    private var adapterCategory: AdapterCategory

    constructor(filderList: ArrayList<ModelCategory>, adapterCategory: AdapterCategory) : super() {
        this.filderList = filderList
        this.adapterCategory = adapterCategory
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint = constraint
        val results = FilterResults()

        // value should not be null and not empty
        if(constraint != null && constraint.isNotEmpty()) {
            //change to uppercase, or lower case to case sentivity
            constraint = constraint.toString().uppercase()
            val filteredModel:ArrayList<ModelCategory> = ArrayList()
            for (i in 0 until filderList.size) {
                //validate
                if(filderList[i].category.uppercase().contains(constraint)) {
                    filteredModel.add(filderList[i])
                }
            }

            results.count = filteredModel.size
            results.values = filteredModel
        } else {
            results.count = filderList.size
            results.values = filderList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        // apply filter changes
        adapterCategory.categoryArrayList = results.values as ArrayList<ModelCategory>

        // notify change
        adapterCategory.notifyDataSetChanged()
    }
}