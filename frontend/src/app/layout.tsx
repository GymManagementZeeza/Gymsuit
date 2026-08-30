import type { Metadata, Viewport } from "next";
import { Space_Grotesk, DM_Sans } from "next/font/google";
import "./globals.css";

const spaceGrotesk = Space_Grotesk({
  variable: "--font-display",
  subsets: ["latin"],
  weight: ["500", "600", "700"],
});

const dmSans = DM_Sans({
  variable: "--font-sans",
  subsets: ["latin"],
});

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000";
const title = "GymSuite — Gym management, without the admin";
const description =
  "GymSuite brings members, payments, notifications and paid-member access into one easy-to-run system for independent gyms.";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: title,
    template: "%s",
  },
  description,
  keywords: [
    "gym management software",
    "gym membership management",
    "fitness studio software",
    "gym billing software",
    "member check-in system",
    "personal trainer scheduling",
  ],
  applicationName: "GymSuite",
  authors: [{ name: "GymSuite" }],
  creator: "GymSuite",
  publisher: "GymSuite",
  formatDetection: { email: false, address: false, telephone: false },
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    url: siteUrl,
    siteName: "GymSuite",
    title,
    description,
    images: [{ url: "/opengraph-image.png", width: 1024, height: 1024, alt: "GymSuite" }],
  },
  twitter: {
    card: "summary_large_image",
    title,
    description,
    images: ["/twitter-image.png"],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: { index: true, follow: true },
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: "GymSuite",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#0c1e3e",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${spaceGrotesk.variable} ${dmSans.variable} h-full antialiased`}
    >
      <body className="h-full">{children}</body>
    </html>
  );
}
