package ca.zeezaglobal.gymsuitapp.data.food

import org.junit.Assert.*
import org.junit.Test

class FoodDatabaseTest {

    @Test
    fun foodNutrition_forClassReturnsExpectedNutrition() {
        val pie = FoodNutrition.forClass("apple_pie")
        assertNotNull(pie)
        assertEquals("Apple pie", pie!!.name)
        assertTrue(pie.kcal > 0)
        assertTrue(pie.carbsG > 0)

        val nonExistent = FoodNutrition.forClass("unknown_dish_xyz")
        assertNull(nonExistent)
    }

    @Test
    fun foodDatabase_containsFoods() {
        assertTrue(FoodDatabase.ALL.isNotEmpty())
        assertTrue(FoodDatabase.ALL.any { it.name.contains("Roti", ignoreCase = true) })
        assertTrue(FoodDatabase.ALL.any { it.name.contains("Biryani", ignoreCase = true) })
    }

    @Test
    fun foodDatabase_searchFindsMatches() {
        val rotiResults = FoodDatabase.search("roti")
        assertTrue(rotiResults.isNotEmpty())
        assertTrue(rotiResults.any { it.name.contains("Roti", ignoreCase = true) })

        val biryaniResults = FoodDatabase.search("biryani")
        assertTrue(biryaniResults.isNotEmpty())
        assertTrue(biryaniResults.any { it.name.contains("Biryani", ignoreCase = true) })
    }

    @Test
    fun foodDatabase_emptyQueryReturnsEmptyList() {
        assertTrue(FoodDatabase.search("").isEmpty())
        assertTrue(FoodDatabase.search("   ").isEmpty())
    }

    @Test
    fun foodDatabase_noMatchReturnsEmptyList() {
        val results = FoodDatabase.search("completely_unmatched_query_12345")
        assertTrue(results.isEmpty())
    }
}
