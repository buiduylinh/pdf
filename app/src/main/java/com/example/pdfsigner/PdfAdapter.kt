package com.example.pdfsigner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_pdf.view.*

class PdfAdapter(val onClick: (FileModel) -> Unit) : RecyclerView.Adapter<PdfAdapter.ItemHolder>() {
    var list : ArrayList<FileModel> = ArrayList()
    fun addData(list : ArrayList<FileModel>) {
        this.list.addAll(list)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
      return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_pdf, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.onBind(list[position])
    }

    inner class ItemHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        fun onBind(fileModel: FileModel) {
            itemView.nameFile.text = fileModel.displayName
            itemView.setOnClickListener {
                onClick.invoke(fileModel)
            }
        }
    }
}