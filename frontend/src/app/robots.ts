import type { MetadataRoute } from "next";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "https://gymsuit.app";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: "/",
        disallow: [
          "/dashboard",
          "/dashboard/",
          "/clientdashboard",
          "/clientdashboard/",
          "/trainerdashboard",
          "/trainerdashboard/",
          "/otp",
          "/scanner",
        ],
      },
    ],
    sitemap: `${siteUrl}/sitemap.xml`,
  };
}
