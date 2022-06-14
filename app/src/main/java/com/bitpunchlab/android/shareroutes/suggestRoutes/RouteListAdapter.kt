package com.bitpunchlab.android.shareroutes.suggestRoutes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bitpunchlab.android.shareroutes.databinding.ItemRouteBinding
import com.bitpunchlab.android.shareroutes.models.Route

class RouteListAdapter(var clickListener: RouteOnClickListener) : ListAdapter<Route, RouteViewHolder>(RouteDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        return RouteViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = getItem(position)
        holder.bind(route, clickListener)
    }

}


class RouteViewHolder(val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(route: Route, onClickListener: RouteOnClickListener) {
        binding.route = route
        binding.clickListener = onClickListener
        binding.executePendingBindings()
    }

    companion object {
        fun from(parent: ViewGroup): RouteViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemRouteBinding.inflate(layoutInflater, parent, false)

            return RouteViewHolder(binding)
        }
    }
}

class RouteDiffCallback : DiffUtil.ItemCallback<Route>() {
    override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
        return oldItem.id == newItem.id
    }

}

class RouteOnClickListener(val clickListener: (Route) -> Unit) {
    fun onClick(route: Route) = clickListener(route)
}