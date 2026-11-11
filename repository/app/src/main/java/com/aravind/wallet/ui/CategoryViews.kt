package com.aravind.wallet.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aravind.wallet.R
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.databinding.ItemCategoryBinding
import com.aravind.wallet.databinding.ItemCategoryChoiceBinding
import com.aravind.wallet.domain.CategoryCatalog

fun ImageView.showCategory(iconKey: String, colorKey: String) {
    setImageResource(when (iconKey) {
        "utensils" -> R.drawable.ic_category_utensils
        "shopping-basket" -> R.drawable.ic_category_shopping_basket
        "house" -> R.drawable.ic_category_house
        "book-open" -> R.drawable.ic_category_book_open
        "briefcase" -> R.drawable.ic_category_briefcase
        else -> R.drawable.ic_category_tag
    })
    imageTintList = null
    background = GradientDrawable().apply {
        setColor(Color.parseColor(CategoryCatalog.colors.find { it.key == colorKey }?.hex ?: "#C9DEF5"))
        cornerRadius = 4 * resources.displayMetrics.density
    }
}

class CategoryChoices(context: Context, val choices: List<WalletCategory>) :
    ArrayAdapter<WalletCategory>(context, R.layout.item_category_choice, choices) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = row(position, convertView, parent)
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = row(position, convertView, parent)
    private fun row(position: Int, recycled: View?, parent: ViewGroup): View {
        val binding = recycled?.let(ItemCategoryChoiceBinding::bind)
            ?: ItemCategoryChoiceBinding.inflate(LayoutInflater.from(context), parent, false)
        val category = choices[position]
        binding.badge.showCategory(category.iconKey, category.backgroundColorKey)
        binding.label.text = if (category.isArchived) context.getString(R.string.archived_name, category.name) else category.name
        return binding.root
    }
}

class CategoriesAdapter(private val edit: (String) -> Unit) : ListAdapter<WalletCategory, CategoriesAdapter.Holder>(
    object : DiffUtil.ItemCallback<WalletCategory>() {
        override fun areItemsTheSame(old: WalletCategory, new: WalletCategory) = old.id == new.id
        override fun areContentsTheSame(old: WalletCategory, new: WalletCategory) = old == new
    },
) {
    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }
    class Holder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val category = getItem(position)
        holder.binding.apply {
            name.text = category.name
            badge.showCategory(category.iconKey, category.backgroundColorKey)
            val base = root.context.getString(if (category.isBuiltIn) R.string.built_in else R.string.custom)
            origin.text = if (category.isArchived) root.context.getString(R.string.archived_name, base) else base
            root.setOnClickListener { edit(category.id) }
        }
    }
}
