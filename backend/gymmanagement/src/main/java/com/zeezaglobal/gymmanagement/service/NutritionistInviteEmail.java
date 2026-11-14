package com.zeezaglobal.gymmanagement.service;

/**
 * HTML for the email a standalone nutritionist's client receives, styled like diet.gymsuit.app
 * (ink / paper / sand / saffron / sage, Space Grotesk headings).
 *
 * Built for email clients: table layout, inline styles, emoji icons (no SVG, which Gmail blocks) and an
 * animated GIF hosted on the diet site, which animates everywhere including Gmail. The CSS keyframe
 * animations in the head (sliding feature rows, pulsing button, floating icon) are a progressive extra for
 * clients that support them (Apple Mail, iOS Mail); they only move things and never hide them, so clients
 * without animation support simply show the static design.
 */
final class NutritionistInviteEmail {

    private static final String INK = "#142321";
    private static final String PAPER = "#fbf7ee";
    private static final String SAND = "#f1ebdd";
    private static final String SAFFRON = "#f45d22";
    private static final String SAGE = "#6e9b79";
    private static final String MUTED = "#5b6664";
    private static final String HEADING = "'Space Grotesk', 'Segoe UI', Helvetica, Arial, sans-serif";
    private static final String BODY = "'DM Sans', 'Segoe UI', Helvetica, Arial, sans-serif";

    private NutritionistInviteEmail() {
    }

    /**
     * @param nutritionistName who sent the invite (escaped here)
     * @param email            the invited address (escaped here)
     * @param link             the accept link (escaped here)
     * @param siteUrl          base URL of the diet site, used for hosted images
     */
    static String html(String nutritionistName, String email, String link, String siteUrl) {
        String name = escape(nutritionistName);
        String to = escape(email);
        String href = escape(link);
        String base = escape(siteUrl.replaceAll("/+$", ""));
        String logo = base + "/GymSuit_logo_package/01-icon-brand.png";
        String gif = base + "/email/diet-invite-macros.gif";

        return """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta name="color-scheme" content="light">
                <meta name="supported-color-schemes" content="light">
                <title>%1$s invited you to GymSuit Diet</title>
                <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;700&family=Space+Grotesk:wght@500;700&display=swap" rel="stylesheet">
                <style>
                  /* Motion only, never opacity: a client that pauses animations must still show everything. */
                  @keyframes gs-slide-in { from { transform: translateX(-10px); } to { transform: none; } }
                  @keyframes gs-pulse { 0%%, 100%% { box-shadow: 0 0 0 0 rgba(244,93,34,.45); } 70%% { box-shadow: 0 0 0 14px rgba(244,93,34,0); } }
                  @keyframes gs-float { 0%%, 100%% { transform: translateY(0); } 50%% { transform: translateY(-6px); } }
                  .gs-feature { animation: gs-slide-in .6s ease-out; }
                  .gs-feature-2 { animation-delay: .12s; }
                  .gs-feature-3 { animation-delay: .24s; }
                  .gs-cta { animation: gs-pulse 2.2s ease-out infinite; }
                  .gs-float { display: inline-block; animation: gs-float 3s ease-in-out infinite; }
                  @media (prefers-reduced-motion: reduce) { .gs-feature, .gs-cta, .gs-float { animation: none !important; } }
                  @media (max-width: 620px) {
                    .gs-wrap { width: 100%% !important; }
                    .gs-pad { padding-left: 22px !important; padding-right: 22px !important; }
                    .gs-h1 { font-size: 28px !important; line-height: 32px !important; }
                    .gs-gif { width: 100%% !important; height: auto !important; }
                  }
                </style>
                </head>
                <body style="margin:0;padding:0;background:%5$s;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">%1$s would like to follow your meals, macros and weight to guide your diet. View-only — accept in one tap.</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%5$s;">
                <tr><td align="center" style="padding:28px 12px;">
                  <table role="presentation" class="gs-wrap" width="600" cellpadding="0" cellspacing="0" border="0" style="width:600px;max-width:600px;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #e9e2d2;">

                    <!-- header -->
                    <tr><td class="gs-pad" style="background:%4$s;padding:22px 36px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                        <td style="background:%5$s;border-radius:12px;padding:6px;line-height:0;"><img src="%6$s" width="28" height="28" alt="GymSuit" style="display:block;border:0;"></td>
                        <td style="padding-left:12px;font-family:%11$s;font-size:20px;font-weight:700;letter-spacing:-0.5px;color:%5$s;">gymsuite <span style="color:%7$s;">diet</span></td>
                      </tr></table>
                    </td></tr>

                    <!-- hero -->
                    <tr><td class="gs-pad" style="padding:34px 36px 8px;">
                      <span style="display:inline-block;font-family:%12$s;font-size:11px;font-weight:700;letter-spacing:1.6px;text-transform:uppercase;color:%4$s;background:%8$s;border-radius:999px;padding:7px 14px;"><span class="gs-float">🥗</span>&nbsp; Nutrition invite</span>
                      <h1 class="gs-h1" style="margin:18px 0 0;font-family:%11$s;font-size:34px;line-height:38px;font-weight:700;letter-spacing:-1px;color:%4$s;">%1$s wants to guide <span style="color:%7$s;">your nutrition.</span></h1>
                      <p style="margin:14px 0 0;font-family:%12$s;font-size:16px;line-height:25px;color:%10$s;">%1$s is a nutrition expert on GymSuit. Accept the invite and they'll see what you log in the GymSuit app, so they can shape your diet around what you actually eat.</p>
                    </td></tr>

                    <!-- animated macros -->
                    <tr><td class="gs-pad" style="padding:24px 28px 6px;" align="center">
                      <img class="gs-gif" src="%13$s" width="544" height="214" alt="Today's plate: protein, carbs and fat filling up" style="display:block;width:544px;max-width:100%%;height:auto;border:0;border-radius:18px;">
                    </td></tr>

                    <!-- features -->
                    <tr><td class="gs-pad" style="padding:18px 36px 4px;">
                      %14$s
                      %15$s
                      %16$s
                    </td></tr>

                    <!-- CTA -->
                    <tr><td class="gs-pad" align="center" style="padding:22px 36px 6px;">
                      <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                        <td class="gs-cta" align="center" style="border-radius:999px;background:%7$s;">
                          <a href="%3$s" style="display:inline-block;padding:16px 34px;font-family:%11$s;font-size:16px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:999px;">Accept invitation&nbsp;&nbsp;→</a>
                        </td>
                      </tr></table>
                      <p style="margin:14px 0 0;font-family:%12$s;font-size:13px;line-height:20px;color:%10$s;">New to GymSuit? Create your account right from the link, then sign in to the GymSuit app with <strong style="color:%4$s;">%2$s</strong>.</p>
                    </td></tr>

                    <!-- footer -->
                    <tr><td class="gs-pad" style="padding:26px 36px 30px;">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%5$s;border-radius:16px;"><tr><td style="padding:16px 18px;font-family:%12$s;font-size:12px;line-height:19px;color:%10$s;">
                        ⏳ This invitation expires in <strong style="color:%4$s;">7 days</strong>. If you weren't expecting it, you can ignore this email.<br>
                        Button not working? Paste this link into your browser:<br>
                        <a href="%3$s" style="color:%7$s;word-break:break-all;">%3$s</a>
                      </td></tr></table>
                    </td></tr>
                  </table>
                  <p style="margin:18px 0 0;font-family:%12$s;font-size:12px;color:%10$s;">Sent by GymSuit Diet · <a href="%9$s" style="color:%10$s;">diet.gymsuit.app</a></p>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(
                name,                 // 1
                to,                   // 2
                href,                 // 3
                INK,                  // 4
                PAPER,                // 5
                logo,                 // 6
                SAFFRON,              // 7
                SAND,                 // 8
                base,                 // 9
                MUTED,                // 10
                HEADING,              // 11
                BODY,                 // 12
                gif,                  // 13
                feature("gs-feature", "🍽️", SAND, "You log, as usual",
                        "Meals, photos and weigh-ins you add in the GymSuit app."),          // 14
                feature("gs-feature gs-feature-2", "📊", "#e4efe7", "They see your macros",
                        "Calories, protein, carbs, fat and your weight trend."),              // 15
                feature("gs-feature gs-feature-3", "🔒", "#fde3d7", "View-only, always",
                        "They can't change anything — and you can be removed anytime."));    // 16
    }

    private static String feature(String cssClass, String icon, String iconBg, String title, String text) {
        return """
                <table role="presentation" class="%1$s" width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 12px;"><tr>
                  <td width="48" valign="top"><div style="width:44px;height:44px;border-radius:14px;background:%3$s;text-align:center;line-height:44px;font-size:22px;">%2$s</div></td>
                  <td valign="top" style="padding-left:14px;">
                    <div style="font-family:%6$s;font-size:16px;font-weight:700;color:%7$s;line-height:22px;">%4$s</div>
                    <div style="font-family:%8$s;font-size:14px;line-height:21px;color:%9$s;">%5$s</div>
                  </td>
                </tr></table>""".formatted(cssClass, icon, iconBg, title, text, HEADING, INK, BODY, MUTED);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
