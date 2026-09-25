"use client";

/* Training Ledger page: System settings use explicit domains and grounded configuration summaries. */
import { useEffect, useState } from "react";
import { PageHeading } from "@/components/dashboard/DashboardShell";
import { ActionButton, StatusPill } from "@/components/dashboard/ui";
import GymProfileModal from "@/components/dashboard/GymProfileModal";
import UpiIdModal from "@/components/UpiIdModal";
import JoiningFeeModal from "@/components/dashboard/JoiningFeeModal";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { getGym, type Gym } from "@/lib/gyms";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import { LOCALES, type Dictionary, type Locale } from "@/lib/i18n/dictionaries";
import { BellRing, Building2, Check, CreditCard, Globe2, Image as ImageIcon, Languages, LockKeyhole, Save, UsersRound, Wallet } from "lucide-react";

function gymProfileSummary(gym: Gym, t: Dictionary) {
  const parts = [gym.addressLine, gym.city, gym.phone, gym.email].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : t.settings.noGymDetails;
}

function gymProfileComplete(gym: Gym) {
  return Boolean(gym.addressLine && gym.phone && gym.email);
}

function LanguageToggle() {
  const { t, locale, setLocale } = useLanguage();
  const nameFor = (id: Locale) => (id === "ml" ? t.settings.malayalam : t.settings.english);
  return (
    <div
      className="flex w-fit border border-[#d8d8d1]"
      role="group"
      aria-label={t.settings.changeLanguage}
    >
      {LOCALES.map((item) => {
        const active = locale === item.id;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => setLocale(item.id)}
            aria-pressed={active}
            className={`px-4 py-2 text-xs font-bold transition ${
              active ? "bg-[#24241f] text-white" : "bg-white text-[#24241f] hover:bg-[#f1f1eb]"
            }`}
          >
            {nameFor(item.id)}
          </button>
        );
      })}
    </div>
  );
}

export default function SettingsPage() {
  const session = useSession();
  const { t, locale } = useLanguage();
  const gymId = session?.gymId ?? null;

  const [gym, setGym] = useState<Gym | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [editingUpi, setEditingUpi] = useState(false);
  const [editingFee, setEditingFee] = useState(false);

  useEffect(() => {
    if (!gymId) return;
    getGym(gymId)
      .then(setGym)
      .catch((err) => setError(err instanceof Error ? err.message : t.settings.errorLoadingGym))
      .finally(() => setLoading(false));
  }, [gymId, t]);

  const languageName = locale === "ml" ? t.settings.malayalam : t.settings.english;

  const otherSettings = [
    { icon: ImageIcon, title: t.settings.branding, text: t.settings.brandingText, action: t.settings.manageBranding, status: t.settings.updated },
    { icon: UsersRound, title: t.settings.rolesAccess, text: t.settings.rolesAccessText, action: t.settings.reviewRoles, status: t.settings.rolesCount },
    { icon: BellRing, title: t.settings.notifications, text: t.settings.notificationsText, action: t.settings.editAlerts, status: t.settings.alertsEnabled },
    { icon: Globe2, title: t.settings.regionalFormat, text: t.settings.regionalFormatText, action: t.settings.editRegional, status: t.settings.regionalStatus(languageName) },
  ];

  return (
    <div className="page-enter space-y-7">
      <PageHeading
        eyebrow={t.settings.eyebrow}
        title={
          <>
            {t.settings.titleA}
            <br />
            <em className="font-normal">{t.settings.titleB}</em>
          </>
        }
        description={t.settings.description}
        actions={<ActionButton icon={<Save className="size-4" />}>{t.common.saveChanges}</ActionButton>}
      />
      <section className="grid gap-4 xl:grid-cols-12">
        <div className="border border-[#d8d8d1] bg-white xl:col-span-8">
          <div className="border-b border-[#e7e7e1] p-5">
            <p className="ledger-label">{gym?.name ?? t.settings.yourGym}</p>
            <h2 className="mt-2 text-xl font-bold">{t.settings.gymSetup}</h2>
          </div>

          {error && (
            <div className="p-5">
              <ErrorBanner message={error} />
            </div>
          )}

          <div className="divide-y divide-[#e8e8e2]">
            <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
              <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                <Building2 className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold">{t.settings.gymProfile}</p>
                  {gym && (
                    <StatusPill
                      label={gymProfileComplete(gym) ? t.settings.complete : t.settings.incomplete}
                      tone={gymProfileComplete(gym) ? "blue" : "orange"}
                    />
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-[#72726b]">
                  {loading ? t.common.loading : gym ? gymProfileSummary(gym, t) : t.settings.gymDetailsError}
                </p>
              </div>
              <button
                onClick={() => setEditing(true)}
                disabled={!gym}
                className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f] disabled:opacity-50"
              >
                {t.settings.editProfile}
              </button>
            </div>

            <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
              <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                <CreditCard className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold">{t.settings.onlinePayments}</p>
                  {gym && (
                    <StatusPill label={gym.upiId ? t.settings.connected : t.settings.notSet} tone={gym.upiId ? "blue" : "orange"} />
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-[#72726b]">
                  {loading
                    ? t.common.loading
                    : gym?.upiId
                      ? t.settings.upiDescriptionSet(gym.upiId)
                      : t.settings.upiDescriptionUnset}
                </p>
              </div>
              <button
                onClick={() => setEditingUpi(true)}
                disabled={!gym}
                className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f] disabled:opacity-50"
              >
                {gym?.upiId ? t.settings.editUpiId : t.settings.addUpiId}
              </button>
            </div>

            <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
              <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                <Wallet className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold">{t.settings.joiningFee}</p>
                  {gym && (
                    <StatusPill
                      label={gym.joiningFee != null ? t.settings.setBadge : t.settings.notSet}
                      tone={gym.joiningFee != null ? "blue" : "orange"}
                    />
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-[#72726b]">
                  {loading
                    ? t.common.loading
                    : gym?.joiningFee != null
                      ? t.settings.joiningFeeSet(gym.joiningFeeCurrency ?? "", gym.joiningFee)
                      : t.settings.joiningFeeUnset}
                </p>
              </div>
              <button
                onClick={() => setEditingFee(true)}
                disabled={!gym}
                className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f] disabled:opacity-50"
              >
                {gym?.joiningFee != null ? t.settings.editJoiningFee : t.settings.setJoiningFee}
              </button>
            </div>

            <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
              <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                <Languages className="size-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold">{t.settings.language}</p>
                  <StatusPill label={languageName} tone="blue" />
                </div>
                <p className="mt-1 text-xs leading-relaxed text-[#72726b]">{t.settings.languageText}</p>
              </div>
              <LanguageToggle />
            </div>

            {otherSettings.map((item) => (
              <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center" key={item.title}>
                <div className="grid size-10 shrink-0 place-items-center bg-[#f1f1eb] text-[#3b3b35]">
                  <item.icon className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-bold">{item.title}</p>
                    <StatusPill label={item.status} tone={item.status === "Setup" ? "orange" : "blue"} />
                  </div>
                  <p className="mt-1 text-xs leading-relaxed text-[#72726b]">{item.text}</p>
                </div>
                <button className="w-fit border border-[#d8d8d1] bg-white px-3 py-2 text-xs font-bold transition hover:border-[#24241f]">{item.action}</button>
              </div>
            ))}
          </div>
        </div>
        <aside className="cut-corner bg-[#24241f] p-5 text-white xl:col-span-4">
          <div className="flex items-center justify-between">
            <LockKeyhole className="size-6 text-[#c7f36a]" />
            <StatusPill label={t.settings.ownerControlled} tone="lime" />
          </div>
          <p className="display-face mt-7 text-3xl leading-[1.05]">
            {t.settings.asideHeadlineA}
            <br />
            {t.settings.asideHeadlineB}
          </p>
          <p className="mt-4 text-xs leading-relaxed text-[#b8b8af]">
            {t.settings.asideText}
          </p>
          <button className="mt-7 flex items-center gap-2 border border-white/25 px-3 py-2 text-xs font-bold transition hover:bg-white hover:text-[#24241f]">
            <Check className="size-3.5 text-[#c7f36a]" /> {t.settings.auditLog}
          </button>
        </aside>
      </section>

      {editing && gymId && gym && (
        <GymProfileModal
          gymId={gymId}
          gym={gym}
          onClose={() => setEditing(false)}
          onSaved={(updated) => {
            setGym(updated);
            setEditing(false);
          }}
        />
      )}

      {editingUpi && gymId && gym && (
        <UpiIdModal
          gymId={gymId}
          gym={gym}
          onClose={() => setEditingUpi(false)}
          onSaved={(updated) => {
            setGym(updated);
            setEditingUpi(false);
          }}
        />
      )}

      {editingFee && gymId && gym && (
        <JoiningFeeModal
          gymId={gymId}
          gym={gym}
          onClose={() => setEditingFee(false)}
          onSaved={(updated) => {
            setGym(updated);
            setEditingFee(false);
          }}
        />
      )}
    </div>
  );
}
