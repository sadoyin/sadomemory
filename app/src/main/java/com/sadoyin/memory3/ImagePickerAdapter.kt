package com.sadoyin.memory3

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sadoyin.memory3.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val chosenImageUris: List<Uri>,
    private val boardSize: BoardSize,
    private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener{
        fun onPlaceHolderClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {


        val view =LayoutInflater.from(context).inflate(R.layout.card_image,parent,false)
        val cardWidth =parent.width/boardSize.getWidth()
        val cardHeight =parent.height/boardSize.getHeight()
        val cardSideLenght = min(cardWidth,cardHeight)
        val layoutParams =view.findViewById<View>(R.id.ivCustomImage).layoutParams
        layoutParams.width =cardSideLenght
        layoutParams.height =cardSideLenght
        return ViewHolder(view)


    }

    override fun getItemCount()= boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {


        if (position < chosenImageUris.size){
            holder.bind(chosenImageUris[position])
        }else{
            holder.bind()
        }

    }

    inner class ViewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        private val ivCustomImage =itemView.findViewById<ImageView>(R.id.ivCustomImage)

        fun bind(uri: Uri) {

            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

        fun bind() {

            ivCustomImage.setOnClickListener{
                imageClickListener.onPlaceHolderClicked()
            }

        }
    }

}
