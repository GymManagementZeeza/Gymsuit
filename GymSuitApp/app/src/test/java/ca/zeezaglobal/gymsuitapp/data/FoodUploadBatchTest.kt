package ca.zeezaglobal.gymsuitapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodUploadBatchTest {

    private fun entries(count: Int) = List(count) { i ->
        FoodEntry(
            id = "entry-$i",
            name = "Food $i",
            meal = MealType.SNACK,
            calories = 100,
            proteinG = 1.0,
            carbsG = 1.0,
            fatG = 1.0,
            timestampMillis = i.toLong()
        )
    }

    @Test
    fun noEntriesMeansNoBatches() {
        assertTrue(batchFoodUploads(emptyList(), emptyMap()).isEmpty())
    }

    @Test
    fun entriesWithoutPhotosSplitByCount() {
        val batches = batchFoodUploads(entries(120), emptyMap())

        assertEquals(listOf(50, 50, 20), batches.map { it.size })
    }

    @Test
    fun photosSplitBySize() {
        val all = entries(4)
        val sizes = all.associate { it.id to 800_000L }

        val batches = batchFoodUploads(all, sizes)

        assertEquals(listOf(2, 2), batches.map { it.size })
    }

    @Test
    fun oversizedPhotoGetsItsOwnBatch() {
        val all = entries(3)
        val sizes = mapOf("entry-1" to 5_000_000L)

        val batches = batchFoodUploads(all, sizes)

        assertEquals(listOf(listOf("entry-0"), listOf("entry-1"), listOf("entry-2")), batches.map { b -> b.map { it.id } })
    }

    @Test
    fun everyEntryIsSentExactlyOnceInOrder() {
        val all = entries(137)
        val sizes = all.filterIndexed { i, _ -> i % 3 == 0 }.associate { it.id to 400_000L }

        val flattened = batchFoodUploads(all, sizes).flatten()

        assertEquals(all, flattened)
    }
}
