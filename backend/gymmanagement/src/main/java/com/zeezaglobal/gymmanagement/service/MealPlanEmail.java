package com.zeezaglobal.gymmanagement.service;

import java.util.ArrayList;
import java.util.List;

/**
 * "Your meal recommendations are ready" email, in the diet.gymsuit.app theme (same look as
 * {@link NutritionistInviteEmail}). It only says which meals were planned; the plan itself is read in the
 * GymSuit app, which the email links to on Google Play.
 */
final class MealPlanEmail {

    private static final String INK = "#142321";
    private static final String PAPER = "#fbf7ee";
    private static final String SAND = "#f1ebdd";
    private static final String SAFFRON = "#f45d22";
    private static final String MUTED = "#5b6664";
    private static final String HEADING = "'Space Grotesk', 'Segoe UI', Helvetica, Arial, sans-serif";
    private static final String BODY = "'DM Sans', 'Segoe UI', Helvetica, Arial, sans-serif";

    private MealPlanEmail() {
    }

    /** One row per meal: icon, label, and whether the nutritionist filled it in. */
    record Meal(String icon, String label, boolean planned) {
    }

    static String html(String nutritionistName, String clientFirstName, String dateLabel,
                       List<Meal> meals, String appUrl, String siteUrl) {
        String name = escape(nutritionistName);
        String first = escape(clientFirstName == null || clientFirstName.isBlank() ? "there" : clientFirstName);
        String day = escape(dateLabel);
        String app = escape(appUrl);
        String base = escape(siteUrl.replaceAll("/+$", ""));
        String logo = base + "/GymSuit_logo_package/01-icon-brand.png";

        List<String> rows = new ArrayList<>();
        for (Meal meal : meals) {
            rows.add(mealRow(meal));
        }

        return """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="color-scheme" content="light">
                <meta name="supported-color-schemes" content="light">
                <title>Your meal plan from %1$s</title>
                <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;700&family=Space+Grotesk:wght@500;700&display=swap" rel="stylesheet">
                <style>
                  /* Motion only, never opacity: a client that pauses animations must still show everything. */
                  @keyframes gs-pulse { 0%%, 100%% { box-shadow: 0 0 0 0 rgba(244,93,34,.45); } 70%% { box-shadow: 0 0 0 14px rgba(244,93,34,0); } }
                  @keyframes gs-float { 0%%, 100%% { transform: translateY(0); } 50%% { transform: translateY(-6px); } }
                  @keyframes gs-slide-in { from { transform: translateX(-10px); } to { transform: none; } }
                  .gs-cta { animation: gs-pulse 2.2s ease-out infinite; }
                  .gs-float { display: inline-block; animation: gs-float 3s ease-in-out infinite; }
                  .gs-meal { animation: gs-slide-in .6s ease-out; }
                  @media (prefers-reduced-motion: reduce) { .gs-cta, .gs-float, .gs-meal { animation: none !important; } }
                  @media (max-width: 620px) {
                    .gs-wrap { width: 100%% !important; }
                    .gs-pad { padding-left: 22px !important; padding-right: 22px !important; }
                    .gs-h1 { font-size: 28px !important; line-height: 32px !important; }
                  }
                </style>
                </head>
                <body style="margin:0;padding:0;background:%5$s;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">%1$s sent your meal plan for %3$s. Open the GymSuit app to see what to eat.</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%5$s;">
                <tr><td align="center" style="padding:28px 12px;">
                  <table role="presentation" class="gs-wrap" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:600px;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #e9e2d2;">

                    <tr><td class="gs-pad" style="background:%4$s;padding:22px 36px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                        <td style="background:%5$s;border-radius:12px;padding:6px;line-height:0;"><img src="%6$s" width="28" height="28" alt="GymSuit" style="display:block;border:0;"></td>
                        <td style="padding-left:12px;font-family:%10$s;font-size:20px;font-weight:700;letter-spacing:-0.5px;color:%5$s;">gymsuite <span style="color:%7$s;">diet</span></td>
                      </tr></table>
                    </td></tr>

                    <tr><td class="gs-pad" style="padding:34px 36px 6px;">
                      <span style="display:inline-block;font-family:%11$s;font-size:11px;font-weight:700;letter-spacing:1.6px;text-transform:uppercase;color:%4$s;background:%8$s;border-radius:999px;padding:7px 14px;"><span class="gs-float">🍽️</span>&nbsp; Meal plan · %3$s</span>
                      <h1 class="gs-h1" style="margin:18px 0 0;font-family:%10$s;font-size:34px;line-height:38px;font-weight:700;letter-spacing:-1px;color:%4$s;">Hi %2$s, your meals are <span style="color:%7$s;">planned.</span></h1>
                      <p style="margin:14px 0 0;font-family:%11$s;font-size:16px;line-height:25px;color:%9$s;">%1$s has sent your meal recommendations for <strong style="color:%4$s;">%3$s</strong>. They're waiting for you in the GymSuit app, on the Food tab.</p>
                    </td></tr>

                    <tr><td class="gs-pad" style="padding:22px 36px 4px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%8$s;border-radius:18px;"><tr><td style="padding:10px 18px;">
                        %12$s
                      </td></tr></table>
                    </td></tr>

                    <tr><td class="gs-pad" align="center" style="padding:24px 36px 6px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                        <td class="gs-cta" align="center" style="border-radius:999px;background:%7$s;">
                          <a href="%13$s" style="display:inline-block;padding:16px 30px;font-family:%10$s;font-size:16px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:999px;">▶&nbsp; Open in the GymSuit app</a>
                        </td>
                      </tr></table>
                      <p style="margin:14px 0 0;font-family:%11$s;font-size:13px;line-height:20px;color:%9$s;">Don't have the app yet? Get it free on Google Play, then sign in with this email address.</p>
                    </td></tr>

                    <tr><td class="gs-pad" style="padding:24px 36px 30px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%5$s;border-radius:16px;"><tr><td style="padding:16px 18px;font-family:%11$s;font-size:12px;line-height:19px;color:%9$s;">
                        💡 Log what you actually eat in the app — %1$s sees it and can adjust your next plan.<br>
                        Button not working? Open this link:<br>
                        <a href="%13$s" style="color:%7$s;word-break:break-all;">%13$s</a>
                      </td></tr></table>
                    </td></tr>
                  </table>
                  <p style="margin:18px 0 0;font-family:%11$s;font-size:12px;color:%9$s;">Sent by GymSuit Diet on behalf of %1$s</p>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                name,                  // 1
                first,                 // 2
                day,                   // 3
                INK,                   // 4
                PAPER,                 // 5
                logo,                  // 6
                SAFFRON,               // 7
                SAND,                  // 8
                MUTED,                 // 9
                HEADING,               // 10
                BODY,                  // 11
                String.join("\n", rows), // 12
                app);                  // 13
    }

    private static String mealRow(Meal meal) {
        String status = meal.planned()
                ? "<span style=\"font-family:" + BODY + ";font-size:12px;font-weight:700;color:#3d7a52;"
                + "background:#e4efe7;border-radius:999px;padding:4px 10px;\">✓ Planned</span>"
                : "<span style=\"font-family:" + BODY + ";font-size:12px;color:" + MUTED + ";\">—</span>";
        return """
                <table role="presentation" class="gs-meal" width="100%%" cellpadding="0" cellspacing="0" border="0"><tr>
                  <td width="44" style="padding:8px 0;"><div style="width:38px;height:38px;border-radius:12px;background:#ffffff;text-align:center;line-height:38px;font-size:20px;">%1$s</div></td>
                  <td style="padding:8px 0 8px 12px;font-family:%3$s;font-size:16px;font-weight:700;color:%4$s;">%2$s</td>
                  <td align="right" style="padding:8px 0;">%5$s</td>
                </tr></table>""".formatted(meal.icon(), escape(meal.label()), HEADING, INK, status);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
