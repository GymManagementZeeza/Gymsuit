package com.zeezaglobal.gymmanagement.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NutritionistInviteEmailTest {

    @Test
    void rendersThemedEmailWithEscapedValuesAndHostedAssets() throws IOException {
        String html = NutritionistInviteEmail.html(
                "Asha <b>Menon</b>", "ravi@example.test",
                "https://diet.gymsuit.app/diet/invite?token=abc&x=1", "https://diet.gymsuit.app/");

        assertTrue(html.contains("Asha &lt;b&gt;Menon&lt;/b&gt; wants to guide"), "name is escaped in the headline");
        assertFalse(html.contains("<b>Menon</b>"), "raw HTML from the name never reaches the email");
        assertTrue(html.contains("href=\"https://diet.gymsuit.app/diet/invite?token=abc&amp;x=1\""), "accept link");
        assertFalse(html.contains("diet-invite-macros.gif"), "no remote animated GIF — deliverability");
        assertFalse(html.contains("fonts.googleapis.com"), "no webfont fetch — deliverability");
        assertTrue(html.contains("src=\"https://diet.gymsuit.app/GymSuit_logo_package/01-icon-brand.png\""), "logo");
        assertTrue(html.contains("#f45d22") && html.contains("#142321"), "site theme colours");
        assertFalse(html.matches("(?s).*%\\d+\\$s.*"), "no unfilled placeholders");

        // Optional local preview: mvn test -Demail.preview.dir=/some/dir -Demail.preview.site=http://localhost:4599
        String dir = System.getProperty("email.preview.dir");
        if (dir != null) {
            String site = System.getProperty("email.preview.site", "https://diet.gymsuit.app");
            Files.writeString(Path.of(dir, "nutritionist-invite.html"), NutritionistInviteEmail.html(
                    "Asha Menon", "ravi@example.test", site + "/diet/invite?token=preview", site));
        }
    }

    @Test
    void textVersionCarriesTheSameInviteWithoutMarkup() {
        String text = NutritionistInviteEmail.text(
                "Asha Menon", "ravi@example.test",
                "https://diet.gymsuit.app/diet/invite?token=abc");

        assertTrue(text.contains("Asha Menon invited you to GymSuit Diet"), "headline");
        assertTrue(text.contains("https://diet.gymsuit.app/diet/invite?token=abc"), "accept link");
        assertTrue(text.contains("ravi@example.test"), "invitee email");
        assertTrue(text.contains("expires in 7 days"), "expiry");
        assertFalse(text.contains("<a "), "no HTML markup");
        assertFalse(text.matches("(?s).*%\\d+\\$s.*"), "no unfilled placeholders");
    }
}
