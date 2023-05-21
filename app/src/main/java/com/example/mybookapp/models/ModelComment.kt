package com.example.mybookapp.models

class ModelComment {

    //variables, shoule be wotk some spelling anh type as we added in firebase
    var id = ""
    var bookId = ""
    var timestamp = ""
    var comment = ""
    var uid = ""

    //emprty constructor

    constructor()
    //param constructor

    constructor(id: String, bookId: String, timestamp: String, comment: String, uid: String) {
        this.id = id
        this.bookId = bookId
        this.timestamp = timestamp
        this.comment = comment
        this.uid = uid
    }


}