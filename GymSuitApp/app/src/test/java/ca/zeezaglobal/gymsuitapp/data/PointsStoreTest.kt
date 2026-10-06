package ca.zeezaglobal.gymsuitapp.data

import ca.zeezaglobal.gymsuitapp.data.model.PointsTransactionDto
import org.junit.Assert.*
import org.junit.Test

class PointsStoreTest {

    @Test
    fun pointsForWorkout_calculatesCorrectFormula() {
        // 50 base + 5 per set + 2 per minute
        val points = PointsConfig.pointsForWorkout(setCount = 4, durationMinutes = 30)
        assertEquals(50 + 4 * 5 + 30 * 2, points) // 130
    }

    @Test
    fun pointsForWorkout_zeroDurationAndSets_returnsBasePoints() {
        val points = PointsConfig.pointsForWorkout(setCount = 0, durationMinutes = 0)
        assertEquals(50, points)
    }

    @Test
    fun rupeesFor_convertsAt100PointsPerRupee() {
        assertEquals(1.0, PointsConfig.rupeesFor(100), 0.001)
        assertEquals(2.5, PointsConfig.rupeesFor(250), 0.001)
        assertEquals(0.0, PointsConfig.rupeesFor(0), 0.001)
    }

    @Test
    fun formattedRupees_formatsWithRupeeSymbol() {
        assertEquals("₹2.50", PointsConfig.formattedRupees(250))
        assertEquals("₹0.00", PointsConfig.formattedRupees(0))
        assertEquals("₹10.00", PointsConfig.formattedRupees(1000))
    }

    @Test
    fun pointsEntry_isRedemptionProperty() {
        val earned = PointsEntry(points = 50, reason = "Workout: Bench")
        assertFalse(earned.isRedemption)

        val redeemed = PointsEntry(points = -100, reason = "Redeemed for cash", rupeeValue = 1.0)
        assertTrue(redeemed.isRedemption)
    }

    @Test
    fun mergeRemote_deduplicatesAndUpdatesBalance() {
        val remoteTxs = listOf(
            PointsTransactionDto(id = "tx-1", points = 50, reason = "Workout: Squat", createdAt = "2026-10-04T08:00:00"),
            PointsTransactionDto(id = "tx-2", points = 75, reason = "Workout: Deadlift", createdAt = "2026-10-04T09:00:00")
        )

        PointsStore.mergeRemote(remoteBalance = 125, remoteTransactions = remoteTxs)

        assertEquals(125, PointsStore.balance.value)
        assertTrue(PointsStore.history.value.any { it.id == "tx-1" })
        assertTrue(PointsStore.history.value.any { it.id == "tx-2" })

        // Merge again with duplicate tx-1 plus a new tx-3
        val remoteTxs2 = listOf(
            PointsTransactionDto(id = "tx-1", points = 50, reason = "Workout: Squat", createdAt = "2026-10-04T08:00:00"),
            PointsTransactionDto(id = "tx-3", points = -100, reason = "Redeemed for cash", rupeeValue = 1.0, createdAt = "2026-10-04T10:00:00")
        )

        PointsStore.mergeRemote(remoteBalance = 25, remoteTransactions = remoteTxs2)

        assertEquals(25, PointsStore.balance.value)
        assertEquals(1, PointsStore.history.value.count { it.id == "tx-1" })
        assertTrue(PointsStore.history.value.any { it.id == "tx-3" })
    }
}
