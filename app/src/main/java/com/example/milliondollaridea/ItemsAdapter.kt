package com.example.milliondollaridea

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.milliondollaridea.ItemsAdapter.MyViewHolder

data class ItemsAdapter(var list: List<ItemViewModel>, var context: Context) : RecyclerView.Adapter<MyViewHolder>() {
    interface ItemClickListener {
        fun onItemClick(position: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.itemresourcefile, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        var obj = ItemViewModel(list.get(position).name, list.get(position).image);
        lateinit var selected:String
        holder.imageView.setImageResource(obj.image)
        holder.btnclick.setOnClickListener(View.OnClickListener { view ->
            val intent = Intent(view.context, ItemDetaildActivity::class.java)
            val item = list.get(holder.adapterPosition)
            intent.putExtra("image",item.image)
            context.startActivity(intent)
        })
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MyViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) , View.OnClickListener{
        val imageView: ImageView = itemView.findViewById(R.id.itemImg_id)
        var btnclick: RelativeLayout = itemView.findViewById(R.id.linearclick)
        override fun onClick(p0: View?) {
            TODO("Not yet implemented")
        }

    }

}
