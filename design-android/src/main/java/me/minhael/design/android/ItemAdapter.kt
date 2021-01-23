package me.minhael.design.android

import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    private val list = ArrayList<Item>()
    private val viewTypes = SparseIntArray()
    private val layoutIds = ArrayList<Int>()

    fun put(item: Item, position: Int = list.size) {
        putItem(item, position)
        notifyItemInserted(position)
    }

    fun put(position: Int, vararg items: Item) {
        items.forEachIndexed { index, i -> putItem(i, position + index) }
        notifyItemRangeInserted(position, items.size)
    }

    fun update(vararg items: Item) {
        items.forEach {
            val index = list.indexOf(it)
            if (index > -1) {
                updateItem(it, index)
                notifyItemChanged(index)
            }
        }
    }

    fun update(position: Int, vararg items: Item) {
        val size = min(list.size - position, items.size)
        for (i in 0 until size) {
            updateItem(items[i], position + i)
        }
        for (i in 0 until items.size - size) {
            putItem(items[size + i], list.size)
        }
//        notifyItemRangeRemoved(position, items.size)
//        notifyItemRangeInserted(position, items.size)
        notifyItemRangeChanged(position, items.size)
    }

    fun remove(position: Int) {
        list.removeAt(position)
        for (i in position..list.size) {
            viewTypes.put(i, viewTypes[i + 1])
        }
        notifyItemRemoved(position)
    }

    fun clear() {
        list.clear()
        viewTypes.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount() = list.size

    override fun getItemViewType(position: Int): Int {
        return viewTypes[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(layoutIds[viewType], parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        list[position].binder(holder.itemView)
    }

    private fun putItem(item: Item, position: Int) {
        viewTypes.put(position, layoutIds.find(item.resLayoutId))
        list.add(position, item)
    }

    private fun updateItem(item: Item, position: Int) {
        viewTypes.put(position, layoutIds.find(item.resLayoutId))
        list[position] = item
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    data class Item(
        val resLayoutId: Int,
        val binder: (View) -> Unit
    )

    companion object {

        /**
         * Find the index of item in this list. Insert it if not exists.
         */
        private fun <T> MutableList<T>.find(item: T) =
            if (contains(item)) indexOf(item) else append(item)

        /**
         * Append an item to this list and return its index
         */
        private fun <T> MutableList<T>.append(item: T): Int = add(item).run { lastIndexOf(item) }
    }
}