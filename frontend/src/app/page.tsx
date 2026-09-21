import Image from "next/image";
import {
  AppWindow,
  ArrowRight,
  BarChart3,
  BellRing,
  Check,
  ChevronDown,
  CircleDollarSign,
  Fingerprint,
  Gauge,
  Monitor,
  MoveRight,
  ShieldCheck,
  Smartphone,
  Sparkles,
  UsersRound,
  Wifi,
  Zap,
} from "lucide-react";
import Header from "@/components/Header";
import Footer from "@/components/Footer";

const featureCards = [
  {
    number: "01",
    icon: UsersRound,
    title: "Members, without the member register",
    text: "Know who joined, renewed, paused, or needs a nudge — from one crisp operating view.",
    tag: "Memberships",
  },
  {
    number: "02",
    icon: CircleDollarSign,
    title: "Collect on time. Keep the context.",
    text: "Track fees, dues and payment links in the same place you manage a member's plan.",
    tag: "Payments",
  },
  {
    number: "03",
    icon: BellRing,
    title: "Every reminder has a reason",
    text: "Send timely payment, renewal and class notifications that feel personal, not spammy.",
    tag: "Notifications",
  },
];

const platformCards = [
  { icon: Smartphone, title: "Android", text: "Built for the phone in every pocket." },
  { icon: AppWindow, title: "iOS", text: "A polished member experience on iPhone." },
  { icon: Monitor, title: "Web", text: "Your operations desk, wherever you are." },
];

const accessPoints = [
  { icon: Fingerprint, label: "Fingerprint-based entry" },
  { icon: Gauge, label: "Live entry visibility" },
  { icon: ShieldCheck, label: "Paid plan verification" },
  { icon: Zap, label: "Faster reception flow" },
];

const faqs = [
  {
    q: "Can I start with only a few members?",
    a: "Yes. GymSuite is free for your first 20 active members, so you can establish the right operating rhythm before your gym grows.",
  },
  {
    q: "Does it work with my existing entry hardware?",
    a: "GymSuite is designed for hardware-connected access workflows, including tripod entry and fingerprint-based member verification.",
  },
  {
    q: "Will members have their own login?",
    a: "Yes. Each member receives their own personalised login for following progress, plans and timely gym updates.",
  },
];

const pricingItems = [
  "Member management",
  "Payment collection",
  "Smart notifications",
  "Member progress login",
  "Android, iOS & web access",
  "Hardware-ready access control",
];

export default function Home() {
  return (
    <div className="min-h-screen overflow-x-hidden bg-paper text-ink selection:bg-saffron selection:text-white">
      <Header />

      <main id="top">
        <section className="relative overflow-hidden border-b border-ink/10">
          <div className="absolute inset-0 dot-field opacity-70" aria-hidden="true" />
          <div className="container relative grid min-h-[680px] items-center gap-12 py-14 lg:grid-cols-[0.94fr_1.06fr] lg:py-20">
            <div className="relative z-10 max-w-[620px] pt-3 lg:pt-0">
              <div className="eyebrow mb-6">
                <Sparkles className="h-3.5 w-3.5" /> India-ready gym management
              </div>
              <h1 className="font-display text-[clamp(3rem,6.4vw,6.3rem)] font-bold leading-[0.9] tracking-[-0.075em] text-ink">
                Your gym is open.
                <br />
                <span className="relative inline-block text-saffron">Your admin</span> should not
                be.
              </h1>
              <p className="mt-7 max-w-[535px] text-lg leading-8 text-ink/68 sm:text-xl">
                GymSuite brings members, payments, notifications and paid-member access into one
                easy-to-run system — built for independent gyms across India.
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
                <Link
                  href="/login"
                  className="button-saffron group justify-center px-6 py-3.5 text-base"
                >
                  See GymSuite in action{" "}
                  <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                </Link>
                <a href="#pricing" className="button-quiet justify-center px-5 py-3.5 text-base">
                  Start free for 20 members
                </a>
              </div>
              <div className="mt-9 flex flex-wrap items-center gap-x-5 gap-y-3 text-sm font-medium text-ink/60">
                <span className="inline-flex items-center gap-2">
                  <Check className="h-4 w-4 text-sage" /> No setup fee
                </span>
                <span className="inline-flex items-center gap-2">
                  <Check className="h-4 w-4 text-sage" /> Android, iOS &amp; web
                </span>
                <span className="inline-flex items-center gap-2">
                  <Check className="h-4 w-4 text-sage" /> Made for Indian gyms
                </span>
              </div>
            </div>

            <div className="relative mx-auto w-full max-w-[680px] lg:mr-0">
              <div
                className="absolute -right-11 top-6 h-52 w-52 rounded-full border-[26px] border-saffron/15 sm:-right-4 sm:h-64 sm:w-64"
                aria-hidden="true"
              />
              <div className="relative overflow-hidden rounded-[2rem] border-[7px] border-ink bg-ink shadow-[16px_20px_0_rgba(20,35,33,0.12)]">
                <div className="relative h-[395px] w-full sm:h-[510px]">
                  <Image
                    src="/gymsuit-hero-operations.jpg"
                    alt="A gym receptionist using GymSuite on a tablet at the front desk"
                    fill
                    priority
                    className="object-cover object-[68%_center]"
                    sizes="(min-width: 1024px) 620px, 100vw"
                  />
                </div>
                <div className="absolute inset-0 bg-gradient-to-t from-ink/85 via-ink/10 to-transparent" />
                <div className="absolute inset-x-4 bottom-4 flex items-end justify-between gap-3 sm:inset-x-6 sm:bottom-6">
                  <div className="rounded-2xl bg-paper/95 px-4 py-3 shadow-lg backdrop-blur-sm sm:px-5">
                    <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.13em] text-ink/50">
                      <span className="pulse-dot" /> Today, 10:24 AM
                    </div>
                    <p className="mt-1 font-display text-base font-bold tracking-[-0.03em] text-ink sm:text-lg">
                      32 members checked in
                    </p>
                  </div>
                  <div className="hidden rounded-2xl bg-saffron px-4 py-3 text-white shadow-lg sm:block">
                    <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-white/75">
                      Collections
                    </p>
                    <p className="font-display text-xl font-bold tracking-[-0.04em]">₹18,420</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="border-b border-ink/10 bg-ink py-5 text-paper">
          <div className="container grid gap-4 sm:grid-cols-[1fr_auto] sm:items-center">
            <p className="font-display text-lg font-bold tracking-[-0.04em]">
              Built around the daily reality of a growing gym.
            </p>
            <div className="flex flex-wrap gap-x-6 gap-y-2 text-xs font-bold uppercase tracking-[0.14em] text-paper/55 sm:justify-end">
              <span>Member first</span>
              <span>•</span>
              <span>Owner friendly</span>
              <span>•</span>
              <span>Hardware ready</span>
            </div>
          </div>
        </section>

        <section id="product" className="container py-24 sm:py-32">
          <div className="grid gap-11 lg:grid-cols-[0.75fr_1.25fr] lg:gap-20">
            <div className="max-w-[410px]">
              <div className="eyebrow mb-5">One operating rhythm</div>
              <h2 className="display-title">
                Less juggling.
                <br />
                More gym.
              </h2>
              <p className="section-copy mt-6">
                The business of training is personal. GymSuite makes the back-office work feel
                just as considered.
              </p>
              <div className="mt-8 h-px w-full bg-ink/15" />
              <p className="mt-5 text-sm leading-6 text-ink/60">
                Every core action lives in context — so your team can move from a member question
                to the answer without switching tools.
              </p>
            </div>
            <div className="grid gap-4 md:grid-cols-3">
              {featureCards.map((feature, index) => {
                const Icon = feature.icon;
                return (
                  <article
                    key={feature.number}
                    className={`feature-card ${index === 1 ? "md:translate-y-10" : ""}`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <span className="feature-number">{feature.number}</span>
                      <span className="grid h-11 w-11 place-items-center rounded-xl bg-ink text-paper">
                        <Icon className="h-5 w-5" />
                      </span>
                    </div>
                    <p className="mt-12 text-xs font-bold uppercase tracking-[0.15em] text-saffron">
                      {feature.tag}
                    </p>
                    <h3 className="mt-3 font-display text-2xl font-bold leading-[1.02] tracking-[-0.055em]">
                      {feature.title}
                    </h3>
                    <p className="mt-4 text-sm leading-6 text-ink/65">{feature.text}</p>
                    <a
                      href="#contact"
                      className="mt-7 inline-flex items-center gap-2 text-sm font-bold text-ink transition-colors hover:text-saffron"
                    >
                      Explore <MoveRight className="h-4 w-4" />
                    </a>
                  </article>
                );
              })}
            </div>
          </div>
        </section>

        <section id="access" className="bg-ink py-20 text-paper sm:py-28">
          <div className="container grid items-center gap-12 lg:grid-cols-[1.05fr_0.95fr] lg:gap-20">
            <div className="relative order-2 lg:order-1">
              <div
                className="absolute -left-4 -top-4 h-24 w-24 border border-paper/20 sm:-left-7 sm:-top-7"
                aria-hidden="true"
              />
              <div className="overflow-hidden rounded-[1.75rem] border border-paper/15 bg-white/5 p-2">
                <div className="relative h-[360px] w-full overflow-hidden rounded-[1.3rem] sm:h-[480px]">
                  <Image
                    src="/gymsuit-access-hardware.jpg"
                    alt="Members tapping their phone to enter through a tripod access gate"
                    fill
                    className="object-cover"
                    sizes="(min-width: 1024px) 560px, 100vw"
                  />
                </div>
              </div>
              <div className="absolute -bottom-4 right-3 max-w-[235px] rounded-2xl bg-paper p-4 text-ink shadow-xl sm:-right-8 sm:bottom-8 sm:p-5">
                <div className="flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.15em] text-sage">
                  <ShieldCheck className="h-4 w-4" /> Access approved
                </div>
                <p className="mt-2 font-display text-lg font-bold tracking-[-0.04em]">
                  Paid plans unlock the gate.
                </p>
                <p className="mt-1 text-xs leading-5 text-ink/60">
                  No manual verification at rush hour.
                </p>
              </div>
            </div>
            <div className="order-1 max-w-[540px] lg:order-2">
              <div className="eyebrow mb-5 border-paper/25 bg-paper/10 text-paper">
                <Wifi className="h-3.5 w-3.5" /> Hardware, in sync
              </div>
              <h2 className="display-title text-paper">More control at the front door.</h2>
              <p className="mt-6 text-lg leading-8 text-paper/65">
                Connect tripod entry and fingerprint unlocks to real member status. Active paid
                members walk in; expired plans stay at the desk, where you can resolve them.
              </p>
              <div className="mt-9 grid gap-3 sm:grid-cols-2">
                {accessPoints.map((point) => {
                  const Icon = point.icon;
                  return (
                    <div key={point.label} className="access-point">
                      <Icon className="h-5 w-5 text-saffron" />
                      <span>{point.label}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </section>

        <section id="members" className="relative overflow-hidden bg-sand py-24 sm:py-32">
          <div className="container grid items-center gap-12 lg:grid-cols-[0.92fr_1.08fr] lg:gap-24">
            <div className="max-w-[520px]">
              <div className="eyebrow mb-5">A member experience worth returning to</div>
              <h2 className="display-title">Give every client a reason to stay on track.</h2>
              <p className="section-copy mt-6">
                A personalised member login turns progress, plans and notifications into an
                everyday fitness companion — not another forgotten app.
              </p>
              <div className="mt-8 flex items-center gap-4">
                <span className="grid h-12 w-12 place-items-center rounded-2xl bg-saffron text-white">
                  <BarChart3 className="h-5 w-5" />
                </span>
                <p className="max-w-[340px] text-sm leading-6 text-ink/65">
                  Members can see the work adding up, while your team stays present with the next
                  useful nudge.
                </p>
              </div>
              <div className="mt-9">
                <a
                  href="#contact"
                  className="inline-flex items-center gap-2 text-sm font-semibold text-ink transition-colors hover:text-saffron"
                >
                  Explore the member journey <ArrowRight className="h-4 w-4" />
                </a>
              </div>
            </div>
            <div className="relative mx-auto w-full max-w-[600px]">
              <div
                className="absolute -right-9 top-4 h-[90%] w-[76%] rounded-[3rem] bg-sage/20"
                aria-hidden="true"
              />
              <div className="relative ml-auto w-[78%] overflow-hidden rounded-[2.5rem] border-[7px] border-ink bg-ink shadow-[15px_22px_0_rgba(20,35,33,0.14)]">
                <div className="relative h-[495px] w-full">
                  <Image
                    src="/gymsuit-member-progress.jpg"
                    alt="A gym member checking their progress on their phone after a workout"
                    fill
                    className="object-cover"
                    sizes="(min-width: 1024px) 470px, 78vw"
                  />
                </div>
                <div className="absolute inset-x-3 top-3 rounded-2xl bg-ink/80 p-3 backdrop-blur-sm">
                  <div className="flex items-center justify-between text-paper">
                    <span className="text-xs font-bold">Hello, Dev</span>
                    <span className="rounded-full bg-sage px-2 py-1 text-[9px] font-bold uppercase tracking-[0.12em] text-ink">
                      On track
                    </span>
                  </div>
                  <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-paper/20">
                    <div className="h-full w-[72%] rounded-full bg-saffron" />
                  </div>
                  <div className="mt-2 flex justify-between text-[10px] font-medium text-paper/65">
                    <span>Training consistency</span>
                    <span>72%</span>
                  </div>
                </div>
              </div>
              <div className="absolute bottom-8 left-0 rounded-2xl border border-ink/10 bg-paper p-4 shadow-lg sm:-left-9 sm:p-5">
                <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-ink/45">
                  This week
                </p>
                <p className="mt-1 font-display text-xl font-bold tracking-[-0.05em]">
                  4 / 5 sessions
                </p>
                <div className="mt-3 flex gap-1.5">
                  {[1, 2, 3, 4, 5].map((day) => (
                    <span
                      key={day}
                      className={`h-2.5 w-2.5 rounded-full ${day < 5 ? "bg-saffron" : "bg-ink/15"}`}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="container py-24 sm:py-32">
          <div className="flex flex-col gap-6 border-b border-ink/15 pb-10 sm:flex-row sm:items-end sm:justify-between">
            <div className="max-w-[570px]">
              <div className="eyebrow mb-5">The same gym, every screen</div>
              <h2 className="display-title">Your desk, your floor, their phone.</h2>
            </div>
            <p className="max-w-[320px] text-sm leading-6 text-ink/65">
              GymSuite keeps the operational picture connected, without making everyone work the
              same way.
            </p>
          </div>
          <div className="mt-5 grid border-x border-ink/10 sm:grid-cols-3">
            {platformCards.map((platform) => {
              const Icon = platform.icon;
              return (
                <article
                  key={platform.title}
                  className="group border-b border-ink/10 p-7 transition-colors hover:bg-sand sm:border-b-0 sm:border-r last:sm:border-r-0 sm:p-9"
                >
                  <span className="grid h-12 w-12 place-items-center rounded-2xl bg-ink text-paper transition-transform duration-200 group-hover:-rotate-3">
                    <Icon className="h-5 w-5" />
                  </span>
                  <h3 className="mt-12 font-display text-2xl font-bold tracking-[-0.05em]">
                    {platform.title}
                  </h3>
                  <p className="mt-2 max-w-[220px] text-sm leading-6 text-ink/60">
                    {platform.text}
                  </p>
                </article>
              );
            })}
          </div>
        </section>

        <section id="pricing" className="bg-saffron py-20 text-white sm:py-28">
          <div className="container grid gap-12 lg:grid-cols-[0.86fr_1.14fr] lg:items-center lg:gap-20">
            <div>
              <div className="eyebrow mb-5 border-white/30 bg-white/10 text-white">
                A better beginning
              </div>
              <h2 className="font-display text-[clamp(2.8rem,5vw,5rem)] font-bold leading-[0.92] tracking-[-0.075em]">
                Start free.
                <br />
                Build with confidence.
              </h2>
              <p className="mt-6 max-w-[430px] text-lg leading-8 text-white/80">
                Run GymSuite for your first 20 members at no cost. Experience the system on your
                floor before you scale it with your community.
              </p>
            </div>
            <div className="relative rounded-[1.75rem] bg-paper p-6 text-ink shadow-[12px_14px_0_rgba(20,35,33,0.18)] sm:p-8">
              <span className="absolute -top-4 right-6 rounded-full bg-ink px-4 py-2 text-[10px] font-bold uppercase tracking-[0.14em] text-paper">
                20 members free
              </span>
              <p className="text-xs font-bold uppercase tracking-[0.16em] text-saffron">
                GymSuite starter
              </p>
              <div className="mt-3 flex items-end gap-2">
                <span className="font-display text-5xl font-bold tracking-[-0.07em]">₹0</span>
                <span className="mb-1 text-sm font-medium text-ink/55">
                  for your first 20 active members
                </span>
              </div>
              <div className="my-7 h-px bg-ink/10" />
              <div className="grid gap-3 sm:grid-cols-2">
                {pricingItems.map((item) => (
                  <div key={item} className="flex gap-2.5 text-sm font-medium text-ink/75">
                    <Check className="mt-0.5 h-4 w-4 shrink-0 text-sage" />
                    {item}
                  </div>
                ))}
              </div>
              <a
                href="#contact"
                className="button-ink mt-8 w-full justify-center py-3.5"
              >
                Start with GymSuite <ArrowRight className="h-4 w-4" />
              </a>
            </div>
          </div>
        </section>

        <section className="container py-20 sm:py-28">
          <div className="grid gap-10 lg:grid-cols-[0.8fr_1.2fr]">
            <div>
              <div className="eyebrow mb-5">Clear answers</div>
              <h2 className="display-title">Before you bring GymSuite to the floor.</h2>
            </div>
            <div className="divide-y divide-ink/12 border-y border-ink/12">
              {faqs.map((faq) => (
                <details key={faq.q} className="group py-5">
                  <summary className="flex cursor-pointer list-none items-center justify-between gap-5 font-display text-lg font-bold tracking-[-0.035em] marker:content-none">
                    {faq.q}
                    <ChevronDown className="h-5 w-5 shrink-0 text-saffron transition-transform group-open:rotate-180" />
                  </summary>
                  <p className="max-w-[600px] pt-3 text-sm leading-6 text-ink/65">{faq.a}</p>
                </details>
              ))}
            </div>
          </div>
        </section>

        <section id="contact" className="bg-ink py-20 text-paper sm:py-28">
          <div className="container relative overflow-hidden rounded-[2rem] border border-paper/15 bg-[#1c3430] px-6 py-12 sm:px-12 sm:py-16">
            <div
              className="absolute -right-28 -top-32 h-80 w-80 rounded-full border-[45px] border-saffron/25"
              aria-hidden="true"
            />
            <div className="relative max-w-[700px]">
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-saffron">
                Get your admin back
              </p>
              <h2 className="mt-4 font-display text-[clamp(2.8rem,5vw,5.5rem)] font-bold leading-[0.91] tracking-[-0.075em]">
                A stronger gym starts with a calmer system.
              </h2>
              <p className="mt-6 max-w-[560px] text-lg leading-8 text-paper/65">
                See how GymSuite can bring your membership, collections and access flow into one
                steady rhythm.
              </p>
              <a
                href="mailto:info@gymmanagement.com"
                className="button-saffron mt-8 px-6 py-3.5"
              >
                Book your GymSuite demo <ArrowRight className="h-4 w-4" />
              </a>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
