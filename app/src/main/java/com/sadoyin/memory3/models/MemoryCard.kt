package com.sadoyin.memory3.models

data class MemoryCard(
    val identifier:Int,
    val imageUrl:String?=null,
    var isFaceUp: Boolean=false,
    var isMatched:Boolean=false
)