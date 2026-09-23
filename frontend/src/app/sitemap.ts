import type { MetadataRoute } from "next";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "https://gymsuit.app";

export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();

  return [
    { url: `${siteUrl}/`, lastModified: now, changeFrequency: "weekly", priority: 1 },
    { url: `${siteUrl}/login`, lastModified: now, changeFrequency: "monthly", priority: 0.6 },
    { url: `${siteUrl}/register`, lastModified: now, changeFrequency: "monthly", priority: 0.8 },
    { url: `${siteUrl}/register/owner`, lastModified: now, changeFrequency: "monthly", priority: 0.7 },
    { url: `${siteUrl}/register/member`, lastModified: now, changeFrequency: "monthly", priority: 0.7 },
    { url: `${siteUrl}/privacy`, lastModified: now, changeFrequency: "yearly", priority: 0.3 },
    { url: `${siteUrl}/terms`, lastModified: now, changeFrequency: "yearly", priority: 0.3 },
  ];
}
