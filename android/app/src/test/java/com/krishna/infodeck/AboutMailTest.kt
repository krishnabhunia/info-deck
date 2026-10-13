package com.krishna.infodeck

import com.krishna.infodeck.ui.FeedbackMail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutMailTest {
    @Test fun subjectFollowsTheChip() =
        listOf("Suggestion", "Feedback", "Bug", "Error").forEach {
            assertEquals("[InfoDeck " + BuildConfig.VERSION_NAME + "] " + it, FeedbackMail.subject(it))
        }

    @Test fun bodyHasTheNineContextLines() {
        val b = FeedbackMail.body("Bug")
        listOf("App: InfoDeck", "Version: ", "Report type: Bug", "Date: ", "Device: ", "Android: ", "Locale: ", "screenshot")
            .forEach { assertTrue(it, b.contains(it)) }
    }

    @Test fun bodyStartsWithSpaceForTheUser() = assertTrue(FeedbackMail.body("Bug").startsWith("\n\n"))

    @Test fun bodyNeverCarriesCardOrAccountData() {
        val b = FeedbackMail.body("Bug").lowercase()
        listOf("policy", "@gmail", "password", "account", "imei").forEach { assertFalse(it, b.contains(it)) }
    }

    @Test fun addressIsTheStandardOne() = assertEquals("kri.subsc@gmail.com", FeedbackMail.TO)
}
