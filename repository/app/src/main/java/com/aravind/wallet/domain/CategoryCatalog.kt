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
    )
    val colors = listOf(
        CategoryColor("Red", "#D56565"), CategoryColor("Orange", "#C88645"),
        CategoryColor("Yellow", "#B7A13C"), CategoryColor("Green", "#5C9B70"),
        CategoryColor("Teal", "#479C98"), CategoryColor("Blue", "#598FD0"),
        CategoryColor("Purple", "#A177CA"), CategoryColor("Pink", "#C46F9D"),
        CategoryColor("Rose", "#F2C7C7"), CategoryColor("Peach", "#F7D5BE"),
        CategoryColor("Butter", "#F3E8B8"), CategoryColor("Mint", "#CCE8D4"),
        CategoryColor("Seafoam", "#C5E6E2"), CategoryColor("Sky", "#C9DEF5"),
        CategoryColor("Lilac", "#DFD0EF"), CategoryColor("Blush", "#F2D0E2"),
    )
}
