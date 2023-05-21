package com.example.mybookapp.adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.mybookapp.MyApplication
import com.example.mybookapp.R
import com.example.mybookapp.databinding.RowCommentBinding
import com.example.mybookapp.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdapterComment: Adapter<AdapterComment.HolderComment> {

    //context
    var context: Context
    //array to hold comments
    val commentArrayList: ArrayList<ModelComment>
    //view binding
    private lateinit var binding: RowCommentBinding

    private lateinit var firebaseAuth: FirebaseAuth

    constructor(context: Context, commentArrayList: ArrayList<ModelComment>) {
        this.context = context
        this.commentArrayList = commentArrayList
        firebaseAuth = FirebaseAuth.getInstance()
    }


    inner class HolderComment(itemView: View): ViewHolder(itemView) {
         //init ui view of row_commen.xml
        val profileTv: ImageView = binding.profileTv
        val nameTv = binding.nameTv
        val dateTv = binding.dateTv
        val commentTv = binding.commentTv

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderComment {
        binding = RowCommentBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderComment(binding.root)
    }

    override fun onBindViewHolder(holder: HolderComment, position: Int) {
//  get data, setdata, handle click
        //get data
        val model = commentArrayList[position]

        val id = model.id
        val bookId = model.bookId
        val comment = model.comment
        val uid = model.uid
        val timestamp = model.timestamp
        //format date
        val date = MyApplication.formatTimeStamp(timestamp.toLong())
        //set data
        holder.dateTv.text = date
        holder.commentTv.text = comment
        // we don't have user name, profile picture but we have user uidm we will load using that uid
        loadUserDetails(model, holder)

        //handle click, show dialog to delete comment
        holder.itemView.setOnClickListener {
//            Requirements to delete a comment
//            1)User must be logged
//            2) Vid in comment (to be deleted)
            if(firebaseAuth.currentUser != null && firebaseAuth.uid == uid) {
                deleteCommentDialog(model, holder)
            }
        }

    }

    private fun deleteCommentDialog(model: ModelComment, holder: HolderComment) {
        //alert dialog
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Comment")
            .setTitle("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") {d, e ->
                //delete comment
                val bookId = model.bookId
                val commentId = model.id
                val ref = FirebaseDatabase.getInstance().getReference("Books")
                ref.child(bookId).child("Comments").child(commentId)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Comment deleted...", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {e ->
                        Toast.makeText(context, "Faild to delete comment due to ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel") {d, e->
                 d.dismiss()
            }
            .show()//don't missit
    }

    private fun loadUserDetails(model: ModelComment, holder: HolderComment) {
        val uid = model.uid
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(uid)
            .addListenerForSingleValueEvent((object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get name, profile image
                    val name = "${snapshot.child("name").value}"
                    val profileImage = "${snapshot.child("profileImage").value}"

                    //set data
                    holder.nameTv.text = name
                    try {
                        Glide.with(context)
                            .load(profileImage)
                            .placeholder(R.drawable.ic_baseline_person_24)
                            .into(holder.profileTv)

                    }catch (e: Exception) {
                        //in case image is empty or null, set default image
                        holder.profileTv.setImageResource(R.drawable.ic_baseline_person_24)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            }))
    }

    override fun getItemCount(): Int {
        return commentArrayList.size
    }
}