import type { Metadata } from "next";
import { FileText } from "lucide-react";
import Header from "@/components/Header";
import Footer from "@/components/Footer";

export const metadata: Metadata = {
  title: "Terms & Conditions — GymSuite",
  description:
    "The terms governing your use of GymSuite gym management software.",
};

const sections = [
  {
    title: "1. The service",
    body: [
      "GymSuite provides gym management software for membership administration, billing and payments, staff and trainer management, notifications, and member access control, delivered through web and mobile applications.",
      "We may update, improve, or change features of the service from time to time. We will make reasonable efforts to notify you of changes that materially affect your use.",
    ],
  },
  {
    title: "2. Accounts and responsibilities",
    body: [
      "Gym owners are responsible for the accuracy of the data they and their staff enter into GymSuite, including member records, plans, pricing, and payment entries.",
      "You are responsible for keeping login credentials confidential and for all activity under your account. Notify us promptly at info@gymmanagement.com if you suspect unauthorized access.",
      "Gyms are responsible for obtaining any consents required to collect and process their members' personal data, including consent for communications sent through GymSuite.",
    ],
  },
  {
    title: "3. Subscriptions and payments",
    body: [
      "GymSuite is billed as a subscription per gym location, as described on our pricing page or in your order. Fees are exclusive of applicable taxes, including GST in India, unless stated otherwise.",
      "Member payments collected through GymSuite are processed by our payment partners and settled to the gym's linked account per the partner's payout schedule.",
      "Subscription fees are non-refundable except where required by law. If a payment fails, we may suspend access until the account is brought current.",
    ],
  },
  {
    title: "4. Acceptable use",
    body: [
      "You agree not to misuse the service: no unlawful content, no attempts to breach security, no reverse engineering, and no use that harms other users or the service.",
      "You will not enter false member payment records or manipulate attendance and billing data to misrepresent gym operations.",
    ],
  },
  {
    title: "5. Availability and liability",
    body: [
      "We aim for high availability but do not guarantee uninterrupted service. We are not liable for losses arising from downtime, data entry errors by your staff, or actions of third-party payment and messaging providers.",
      "To the maximum extent permitted by law, our total liability for any claim relating to the service is limited to the subscription fees you paid in the twelve months before the claim.",
    ],
  },
  {
    title: "6. Termination",
    body: [
      "You may cancel your subscription at any time; access continues until the end of the current billing period.",
      "We may suspend or terminate accounts that violate these terms or remain unpaid after notice. On termination, you may request an export of your gym's data within 30 days, after which it may be deleted.",
    ],
  },
  {
    title: "7. Governing law",
    body: [
      "These terms are governed by the laws of India. Disputes will be subject to the jurisdiction of the courts at the location of GymSuite's registered office in India.",
    ],
  },
  {
    title: "8. Changes to these terms",
    body: [
      "We may revise these Terms & Conditions from time to time. We will post the updated version here with a new date. Continued use of GymSuite after changes take effect constitutes acceptance of the revised terms.",
    ],
  },
  {
    title: "9. Contact us",
    body: [
      "For questions about these terms, write to info@gymmanagement.com.",
    ],
  },
];

export default function TermsPage() {
  return (
    <div className="min-h-screen overflow-x-hidden bg-paper text-ink selection:bg-saffron selection:text-white">
      <Header />

      <main id="top">
        <section className="relative overflow-hidden border-b border-ink/10">
          <div className="absolute inset-0 dot-field opacity-70" aria-hidden="true" />
          <div className="container relative py-14 lg:py-20">
            <div className="max-w-3xl">
              <div className="eyebrow mb-6">
                <FileText className="h-3.5 w-3.5" /> Legal
              </div>
              <h1 className="font-display text-[clamp(2.4rem,5vw,4.2rem)] font-bold leading-[0.95] tracking-[-0.06em]">
                Terms <span className="text-saffron">&amp;</span> Conditions
              </h1>
              <p className="mt-6 max-w-[560px] text-lg leading-8 text-ink/68">
                The terms governing your use of GymSuite gym management software.
              </p>
              <p className="mt-4 text-sm font-medium text-ink/50">Last updated: 22 September 2026</p>
            </div>
          </div>
        </section>

        <section className="border-b border-ink/10">
          <div className="container py-12 lg:py-16">
            <div className="max-w-3xl space-y-10">
              {sections.map((section) => (
                <div key={section.title}>
                  <h2 className="font-display text-xl font-bold tracking-[-0.03em] sm:text-2xl">
                    {section.title}
                  </h2>
                  <ul className="mt-4 space-y-3">
                    {section.body.map((paragraph, index) => (
                      <li key={index} className="leading-7 text-ink/70">
                        {paragraph}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
