package kz.aday.bot.model;

public enum Category {
    FIRST("🍲 Первое", "первое"),
    SECOND("🍖 Второе","второе"),
    SALAD("🥗 Салат","салат"),
    BAKERY("🥐 Выпечка", "выпечка"),
    BREAD("🍞 Хлеб", "хлеб");

    private final String displayName;
    private final String value;

    Category(String displayName, String value) {
        this.displayName = displayName;
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getValue() {
        return value;
    }

    public static Category from(String message) {
        for (Category category: Category.values()) {
            if (message.toLowerCase().startsWith(category.value.toLowerCase())) {
                return category;
            }
        }
        return null;
    }
}
