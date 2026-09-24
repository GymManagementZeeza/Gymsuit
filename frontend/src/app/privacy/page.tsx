import type { Metadata } from "next";
import { ShieldCheck } from "lucide-react";
import Header from "@/components/Header";
import Footer from "@/components/Footer";

export const metadata: Metadata = {
  title: "Privacy Policy — GymSuite",
  description:
    "How GymSuite collects, uses, and protects personal data for gym owners, staff, trainers, and members.",
};

const sections = [
  {
    title: "1. Information we collect",
    body: [
      "Account information: when a gym owner, staff member, trainer, or member creates an account, we collect names, email addresses, phone numbers, and login credentials.",
      "Member records: gyms using GymSuite store member details such as contact information, date of birth, gender, photographs, membership plans, payment history, attendance, and health notes they choose to record.",
      "Payment information: we process subscription fees, joining fees, and other gym charges. Card and UPI details are handled by our payment partners; we store transaction references and amounts, not full card numbers.",
      "Usage data: we automatically collect device information, log data, and interaction analytics that help us keep the service reliable and improve it.",
    ],
  },
  {
    title: "2. How we use information",
    body: [
      "To provide and operate the GymSuite service, including member management, billing, notifications, and access control.",
      "To send transactional messages such as payment confirmations, renewal reminders, and security alerts.",
      "To detect, prevent, and address fraud, abuse, and technical issues.",
      "To comply with legal obligations applicable in India.",
    ],
  },
  {
    title: "3. Sharing of information",
    body: [
      "We do not sell personal data. We share it only with service providers that help us operate GymSuite (hosting, payments, messaging), under contracts that require them to protect it.",
      "Member data entered by a gym belongs to that gym; we process it on the gym's behalf and make it available to the gym's authorized staff.",
      "We may disclose information if required by law or to protect the rights, property, or safety of GymSuite, our users, or the public.",
    ],
  },
  {
    title: "4. Data security and retention",
    body: [
      "We use encryption in transit and at rest, access controls, and regular backups to protect personal data. No method of transmission over the internet is completely secure, but we work continuously to safeguard your information.",
      "We retain data for as long as an account is active or as needed to provide the service, and delete or anonymize it when it is no longer required, subject to legal retention requirements.",
    ],
  },
  {
    title: "5. Your rights",
    body: [
      "You may request access to, correction of, or deletion of your personal data by contacting your gym or writing to us at info@gymmanagement.com.",
      "Members should note that some records (for example, payment receipts) may need to be retained for accounting and legal purposes even after a deletion request.",
    ],
  },
  {
    title: "6. Children's privacy",
    body: [
      "GymSuite is intended for use by gyms and their adult members or staff. Gyms are responsible for obtaining any required parental consent before entering data about minors.",
    ],
  },
  {
    title: "7. Changes to this policy",
    body: [
      "We may update this Privacy Policy from time to time. We will post the revised version here and update the date below. Continued use of GymSuite after changes take effect means you accept the updated policy.",
    ],
  },
  {
    title: "8. Contact us",
    body: [
      "For privacy questions or requests, write to info@gymmanagement.com.",
    ],
  },
];

export default function PrivacyPage() {
  return (
    <div className="min-h-screen overflow-x-hidden bg-paper text-ink selection:bg-saffron selection:text-white">
      <Header />

      <main id="top">
        <section className="relative overflow-hidden border-b border-ink/10">
          <div className="absolute inset-0 dot-field opacity-70" aria-hidden="true" />
          <div className="container relative py-14 lg:py-20">
            <div className="max-w-3xl">
              <div className="eyebrow mb-6">
                <ShieldCheck className="h-3.5 w-3.5" /> Legal
              </div>
              <h1 className="font-display text-[clamp(2.4rem,5vw,4.2rem)] font-bold leading-[0.95] tracking-[-0.06em]">
                Privacy <span className="text-saffron">Policy</span>
              </h1>
              <p className="mt-6 max-w-[560px] text-lg leading-8 text-ink/68">
                How GymSuite collects, uses, and protects personal data for gym
                owners, staff, trainers, and members.
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
