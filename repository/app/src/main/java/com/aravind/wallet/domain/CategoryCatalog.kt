package com.aravind.wallet.domain

data class CategoryIcon(val key: String, val label: String)
data class CategoryColor(val key: String, val hex: String)

object CategoryCatalog {
    val icons = listOf(
        CategoryIcon("utensils", "Food"), CategoryIcon("shopping-basket", "Basket"),
        CategoryIcon("house", "Home"), CategoryIcon("book-open", "Book"),
        CategoryIcon("briefcase", "Work"), CategoryIcon("tag", "Other"),
        CategoryIcon("plane", "Travel"),
        CategoryIcon("music", "Music"),
        CategoryIcon("heart", "Health"),
        CategoryIcon("car", "Car"),
        CategoryIcon("coffee", "Coffee"),
        CategoryIcon("gift", "Gifts"),
        CategoryIcon("bulb", "Utilities"),
        CategoryIcon("money", "Money"), CategoryIcon("phone", "Phone"),
        CategoryIcon("fitness", "Fitness"),
        CategoryIcon("uncategorised", "Uncategorised"),
    )
    // Accept old stored keys and backups; removed artwork renders as Other.
    val supportedIconKeys = icons.map { it.key }.toSet() + setOf("pet", "tools")
    val colors = listOf(
        CategoryColor("Red", "#D56565"), CategoryColor("Orange", "#C88645"),
        CategoryColor("Yellow", "#B7A13C"), CategoryColor("Green", "#5C9B70"),
        CategoryColor("Teal", "#479C98"), CategoryColor("Blue", "#598FD0"),
        CategoryColor("Purple", "#A177CA"), CategoryColor("Pink", "#C46F9D"),
        CategoryColor("Rose", "#B9B5AC"), CategoryColor("Peach", "#C7B39A"),
        CategoryColor("Butter", "#BCC49F"), CategoryColor("Mint", "#A9BEC2"),
        CategoryColor("Seafoam", "#B8B0C2"), CategoryColor("Sky", "#C9DEF5"),
        CategoryColor("Lilac", "#C6BAAD"), CategoryColor("Blush", "#F2D0E2"),
    )
}
