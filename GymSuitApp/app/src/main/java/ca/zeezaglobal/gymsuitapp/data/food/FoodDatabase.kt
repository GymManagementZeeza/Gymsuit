package ca.zeezaglobal.gymsuitapp.data.food

/** A searchable food with typical single-serving nutrition (rough estimates). */
data class FoodItem(
    val name: String,
    val serving: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val aliases: String = ""
)

/**
 * Built-in offline food list: common Indian foods plus the Food-101 dishes.
 * Values are approximate typical servings, not lab-measured data.
 */
object FoodDatabase {

    private val INDIAN = """
Roti / Chapati|1 medium|100|3|18|2|phulka fulka
Paratha (plain)|1 piece|200|4|28|8|parantha
Aloo paratha|1 piece|280|6|38|11|potato paratha
Paneer paratha|1 piece|300|10|34|14|
Naan|1 piece|260|8|45|5|
Butter naan|1 piece|310|8|45|10|
Garlic naan|1 piece|290|8|45|8|
Puri|2 pieces|200|4|26|9|poori
Bhatura|1 piece|250|6|34|10|bature
Kulcha|1 piece|250|7|42|5|
Thepla|2 pieces|180|5|26|6|
Missi roti|1 piece|150|6|22|4|
Plain dosa|1 piece|130|3|22|3|dosai
Masala dosa|1 piece|280|6|42|9|
Rava dosa|1 piece|200|4|30|7|
Idli|2 pieces|130|4|26|1|idly
Medu vada|2 pieces|280|8|28|15|vada uzhunnu
Uttapam|1 piece|200|6|32|5|oothappam
Upma|1 cup|230|5|36|7|
Poha|1 cup|250|5|42|7|aval
Pongal|1 cup|280|8|40|10|ven pongal
Dhokla|2 pieces|120|4|20|3|
Khandvi|6 pieces|130|4|14|6|
Sambar|1 cup|130|6|20|3|
Rasam|1 cup|60|2|10|2|
Steamed rice|1 cup|200|4|44|0|white rice chawal
Jeera rice|1 cup|250|5|44|6|cumin rice
Vegetable pulao|1 cup|300|6|48|9|veg pulav
Vegetable biryani|1 plate|450|10|70|14|veg biryani
Chicken biryani|1 plate|550|28|65|18|
Mutton biryani|1 plate|600|30|65|23|lamb biryani
Egg biryani|1 plate|500|20|66|17|
Curd rice|1 cup|250|7|38|7|thayir sadam
Lemon rice|1 cup|280|5|46|8|
Tamarind rice|1 cup|300|5|48|9|puliyogare pulihora
Khichdi|1 cup|250|9|42|5|
Bisi bele bath|1 cup|300|9|44|9|
Dal tadka|1 cup|180|9|26|5|dal fry toor
Dal makhani|1 cup|280|10|28|14|
Moong dal|1 cup|150|10|24|1|
Masoor dal|1 cup|160|10|26|1|
Chana masala|1 cup|270|11|38|9|chole chickpea curry
Rajma|1 cup|250|11|38|6|kidney beans curry
Palak paneer|1 cup|300|14|10|23|spinach paneer
Paneer butter masala|1 cup|400|14|14|32|paneer makhani
Kadai paneer|1 cup|350|15|12|27|
Matar paneer|1 cup|320|14|16|22|
Shahi paneer|1 cup|420|14|14|34|
Paneer tikka|6 pieces|300|18|8|22|
Aloo gobi|1 cup|180|4|22|9|
Aloo matar|1 cup|190|5|26|8|
Bhindi masala|1 cup|150|3|14|9|okra ladyfinger
Baingan bharta|1 cup|150|3|14|9|brinjal eggplant
Mixed vegetable curry|1 cup|160|4|18|8|mix veg
Malai kofta|1 cup|450|9|28|34|
Sarson ka saag|1 cup|220|8|14|15|
Kadhi pakora|1 cup|250|8|22|14|
Chicken curry|1 cup|300|26|8|18|
Butter chicken|1 cup|450|28|14|32|murgh makhani
Chicken tikka masala|1 cup|400|30|12|26|
Tandoori chicken|2 pieces|260|30|4|13|
Chicken tikka|6 pieces|220|28|4|10|
Chicken 65|1 plate|350|24|18|20|
Mutton curry|1 cup|400|28|8|28|lamb curry
Rogan josh|1 cup|400|28|8|28|
Fish curry|1 cup|250|24|8|14|meen
Prawn curry|1 cup|220|22|8|11|shrimp curry
Egg curry|2 eggs|280|14|10|20|anda curry
Egg bhurji|2 eggs|200|13|4|15|scrambled eggs
Boiled egg|1 egg|78|6|1|5|
Omelette (masala)|2 eggs|190|13|4|14|
Seekh kebab|2 pieces|250|18|5|17|
Pav bhaji|1 plate|450|10|60|18|
Misal pav|1 plate|450|14|55|18|
Vada pav|1 piece|290|7|40|11|
Samosa|1 piece|260|4|28|15|
Kachori|1 piece|200|4|22|11|
Pakora / Bhajiya|6 pieces|250|6|24|14|pakoda bhaji
Aloo tikki|2 pieces|200|4|28|8|
Bhel puri|1 plate|250|5|42|7|
Pani puri|6 pieces|200|4|32|7|golgappa puchka
Sev puri|1 plate|280|5|34|14|
Dahi vada|2 pieces|250|8|30|10|dahi bhalla
Papdi chaat|1 plate|300|7|38|14|
Maggi noodles|1 packet|350|8|50|13|instant noodles
Veg sandwich|1 sandwich|250|8|38|7|
Grilled cheese sandwich|1 sandwich|400|15|30|24|
Curd / Dahi|1 cup|100|6|8|5|yogurt yoghurt
Raita|1 cup|120|5|10|6|
Sweet lassi|1 glass|220|7|36|5|
Salted lassi|1 glass|100|5|8|5|
Buttermilk (Chaas)|1 glass|40|2|5|1|chaas moru
Paneer|100 g|265|18|3|20|cottage cheese
Masala chai|1 cup|90|3|13|3|tea
Coffee with milk|1 cup|80|3|9|3|filter coffee
Milk|1 cup|150|8|12|8|
Ghee|1 tsp|45|0|0|5|clarified butter
Gulab jamun|2 pieces|300|4|48|11|
Rasgulla|2 pieces|200|4|42|2|
Jalebi|2 pieces|300|2|52|10|
Laddu|1 piece|180|3|24|8|ladoo besan motichoor
Kheer|1 bowl|250|6|38|8|payasam rice pudding
Gajar halwa|1/2 cup|250|4|34|11|carrot halwa
Sooji halwa|1/2 cup|280|3|40|12|suji rava kesari
Rasmalai|2 pieces|300|8|34|14|
Kaju katli|2 pieces|120|3|14|6|cashew barfi
Barfi|1 piece|140|3|18|6|burfi
Mysore pak|1 piece|200|2|20|13|
Peda|1 piece|90|2|14|3|
Kulfi|1 piece|200|5|22|10|
Murukku|30 g|150|2|18|8|chakli
Mixture / Namkeen|30 g|160|4|14|10|
Khakhra|1 piece|80|3|12|2|
Besan chilla|1 piece|140|7|16|5|cheela
Sprouts salad|1 cup|120|8|20|1|
Roasted chana|30 g|110|6|18|2|
Peanuts|30 g|170|7|5|14|groundnut
Banana|1 medium|105|1|27|0|
Apple|1 medium|95|0|25|0|
Orange|1 medium|62|1|15|0|
Mango|1 cup|100|1|25|1|
Papaya|1 cup|60|1|15|0|
Guava|1 medium|68|3|14|1|
Watermelon|1 cup|46|1|12|0|
Grapes|1 cup|104|1|27|0|
Bread slice|1 slice|80|3|15|1|toast
Butter|1 tsp|35|0|0|4|
Peanut butter|1 tbsp|95|4|3|8|
Oats (cooked)|1 cup|160|6|28|3|oatmeal porridge
Cornflakes with milk|1 bowl|250|8|44|5|
""".trim()

    val ALL: List<FoodItem> by lazy {
        val indian = INDIAN.lines().mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 6) null else FoodItem(
                name = p[0], serving = p[1], kcal = p[2].toInt(), proteinG = p[3].toInt(),
                carbsG = p[4].toInt(), fatG = p[5].toInt(), aliases = p.getOrElse(6) { "" }
            )
        }
        val known = indian.map { it.name.lowercase() }.toSet()
        val western = FoodNutrition.CLASSES.mapNotNull { id ->
            FoodNutrition.forClass(id)?.let {
                FoodItem(it.name, it.serving, it.kcal, it.proteinG, it.carbsG, it.fatG, id.replace('_', ' '))
            }
        }.filter { it.name.lowercase() !in known }
        indian + western
    }

    /** Case-insensitive search; every typed word must appear in the name or aliases. Best matches first. */
    fun search(query: String, limit: Int = 6): List<FoodItem> {
        val tokens = query.lowercase().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        return ALL.mapNotNull { item ->
            val name = item.name.lowercase()
            val haystack = "$name ${item.aliases.lowercase()}"
            if (!tokens.all { it in haystack }) return@mapNotNull null
            val score = when {
                name.startsWith(tokens.first()) -> 0
                name.split(" ", "/", "(").any { it.startsWith(tokens.first()) } -> 1
                else -> 2
            }
            score to item
        }.sortedWith(compareBy({ it.first }, { it.second.name.length })).take(limit).map { it.second }
    }
}
