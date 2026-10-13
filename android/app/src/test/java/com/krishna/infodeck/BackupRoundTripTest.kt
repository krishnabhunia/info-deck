package com.krishna.infodeck

import com.krishna.infodeck.backup.BCard
import com.krishna.infodeck.backup.BCategory
import com.krishna.infodeck.backup.BData
import com.krishna.infodeck.backup.BDate
import com.krishna.infodeck.backup.BSched
import com.krishna.infodeck.backup.BackupFile
import com.krishna.infodeck.backup.BackupIo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupRoundTripTest {

    private fun sample() = BackupFile(
        exportedAt = 1_755_000_000_000L,
        categories = listOf(
            BCategory(1, "Uncategorised", 7, 100000, true),
            BCategory(2, "Insurance", 0, 0, false)
        ),
        cards = listOf(
            BCard(
                id = 10, categoryId = 2, label = "Car insurance", keyDateLocalId = 100,
                manualOrder = 1, createdAt = 1L, updatedAt = 2L,
                data = listOf(BData("Policy no", "AB-99/2231", 0)),
                dates = listOf(BDate(100, "Expiry", 20_500, 0), BDate(101, "Bought", 19_000, 1)),
                schedulers = listOf(BSched(100, "DAYS", 30, "BEFORE", 540, true))
            )
        )
    )

    @Test fun encodesAndDecodesBackToTheSameContent() {
        val decoded = BackupIo.decode(BackupIo.encode(sample()))
        assertNotNull(decoded)
        assertEquals(sample(), decoded)
    }

    @Test fun cardDetailSurvivesTheRoundTrip() {
        val decoded = BackupIo.decode(BackupIo.encode(sample()))!!
        val card = decoded.cards.first()
        assertEquals("Car insurance", card.label)
        assertEquals("AB-99/2231", card.data.first().value)
        assertEquals(2, card.dates.size)
        assertEquals(100L, card.keyDateLocalId)
        assertEquals("DAYS", card.schedulers.first().unit)
        assertEquals(30, card.schedulers.first().amount)
        assertEquals("BEFORE", card.schedulers.first().direction)
    }

    @Test fun schedulerStaysPointedAtItsOwnDate() {
        val decoded = BackupIo.decode(BackupIo.encode(sample()))!!
        val card = decoded.cards.first()
        assertEquals(card.dates.first().localId, card.schedulers.first().dateLocalId)
    }

    @Test fun headerCarriesAppNameAndVersion() {
        val decoded = BackupIo.decode(BackupIo.encode(sample()))!!
        assertEquals("InfoDeck", decoded.app)
        assertEquals(1, decoded.version)
    }

    @Test fun garbageDecodesToNullInsteadOfThrowing() {
        assertNull(BackupIo.decode("not json at all"))
        assertNull(BackupIo.decode("{\"unexpected\":true}"))
        assertNull(BackupIo.decode(""))
    }

    @Test fun unknownKeysAreIgnoredSoNewerBackupsStillLoad() {
        val text = BackupIo.encode(sample()).replaceFirst("{", "{\"futureField\": 7,")
        assertNotNull(BackupIo.decode(text))
    }
}
