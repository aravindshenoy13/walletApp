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

fun categoryDisplayColor(context: Context, key: String): Int {
    val color = Color.parseColor(CategoryCatalog.colors.find { it.key == key }?.hex ?: "#C9DEF5")
    val dark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
    // Keep white line art readable on pastel backgrounds without changing saved colors.
    if (!dark) return color
    var displayed = color
    var shade = 0f
    while (androidx.core.graphics.ColorUtils.calculateContrast(Color.WHITE, displayed) < 2.5 && shade < 1f) {
        shade += 0.01f
        // Float rounding in blendARGB can turn an opaque alpha into 254. Contrast
        // requires an opaque background, and these badges are always solid colors.
        displayed = androidx.core.graphics.ColorUtils.setAlphaComponent(
            androidx.core.graphics.ColorUtils.blendARGB(color, Color.BLACK, shade.coerceAtMost(1f)), 255)
    }
    return displayed
}

fun ImageView.showCategory(iconKey: String, colorKey: String) {
    setImageResource(when (iconKey) {
        "money" -> R.drawable.ic_category_money
        "phone" -> R.drawable.ic_category_phone
        "fitness" -> R.drawable.ic_category_fitness
        "uncategorised" -> R.drawable.ic_category_uncategorised

        "plane" -> R.drawable.ic_category_plane
        "music" -> R.drawable.ic_category_music
        "heart" -> R.drawable.ic_category_heart
        "car" -> R.drawable.ic_category_car
        "coffee" -> R.drawable.ic_category_coffee
        "gift" -> R.drawable.ic_category_gift
        "bulb" -> R.drawable.ic_category_bulb
        "utensils" -> R.drawable.ic_category_utensils
        "shopping-basket" -> R.drawable.ic_category_shopping_basket
        "house" -> R.drawable.ic_category_house
        "book-open" -> R.drawable.ic_category_book_open
        "briefcase" -> R.drawable.ic_category_briefcase
        else -> R.drawable.ic_category_tag
    })
    imageTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.icon_stroke))
    background = GradientDrawable().apply {
        setColor(categoryDisplayColor(context, colorKey))
        cornerRadius = 4 * resources.displayMetrics.density
    }
}

class CategoryChoices(context: Context, choices: List<WalletCategory>) :
    ArrayAdapter<WalletCategory>(context, R.layout.item_category_choice, choices) {
    var selectedId: String? = null
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = row(position, convertView, parent)
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = row(position, convertView, parent)
    private fun row(position: Int, recycled: View?, parent: ViewGroup): View {
        val binding = recycled?.let(ItemCategoryChoiceBinding::bind)
            ?: ItemCategoryChoiceBinding.inflate(LayoutInflater.from(context), parent, false)
        val category = requireNotNull(getItem(position))
        binding.badge.showCategory(category.iconKey, category.backgroundColorKey)
        binding.root.isActivated = category.id == selectedId
        androidx.core.view.ViewCompat.setStateDescription(binding.root, if (category.id == selectedId) "Selected" else "Not selected")
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
            origin.text = if (category.isArchived) root.context.getString(R.string.archived) else ""
            origin.visibility = if (origin.text.isEmpty()) View.GONE else View.VISIBLE
            root.setOnClickListener { edit(category.id) }
        }
    }
}
