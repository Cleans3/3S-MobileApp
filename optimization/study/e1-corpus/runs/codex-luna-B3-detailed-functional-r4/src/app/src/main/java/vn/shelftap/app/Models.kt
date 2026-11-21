package vn.shelftap.app

data class Loan(val loanId:String,val copyId:String,val titleId:String,val title:String,val barcode:String,val outAt:String,val dueAt:String,val returnedAt:String?,val renewals:Int,val renewable:Boolean)
data class HistoryItem(val titleId:String,val title:String,val outAt:String,val returnedAt:String?)
data class Member(val memberId:String,val fullName:String,val email:String,val memberType:String,val status:String,val historyEnabled:Boolean,val mustChangePassword:Boolean)
data class BookDetail(val titleId:String,val isbn:String,val title:String,val authors:List<String>,val subjects:List<String>,val coverUrl:String?,val copiesAvailable:Int,val copiesTotal:Int)
