"use client";

/* Settings page: FLEX design language — rounded light cards, lime accents, pill badges. */
import { useEffect, useState } from "react";
import { FlexButton, FlexCard, FlexIconBadge, FlexPageHeader, FlexPill } from "@/components/dashboard/ui";
import GymProfileModal from "@/components/dashboard/GymProfileModal";
import UpiIdModal from "@/components/UpiIdModal";
import JoiningFeeModal from "@/components/dashboard/JoiningFeeModal";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import { useSession } from "@/hooks/useSession";
import { getGym, type Gym } from "@/lib/gyms";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import { LOCALES, type Dictionary, type Locale } from "@/lib/i18n/dictionaries";
import { BellRing, Building2, Check, CreditCard, Image as ImageIcon, Languages, LockKeyhole, Save, UsersRound, Wallet } from "lucide-react";

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
      className="flex w-fit gap-1 rounded-full bg-stone-100 p-1"
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
            className={`rounded-full px-4 py-2 text-xs font-bold transition ${
              active ? "bg-stone-900 text-white" : "text-stone-600 hover:text-stone-900"
            }`}
          >
            {nameFor(item.id)}
          </button>
        );
      })}
    </div>
  );
}

const ghostButtonClass =
  "inline-flex w-fit shrink-0 items-center gap-2 rounded-full bg-stone-100 px-4 py-2 text-xs font-bold text-stone-700 transition hover:bg-stone-200 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-50";

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
  ];

  return (
    <div className="page-enter space-y-6">
      <FlexPageHeader
        title={t.settings.titleA}
        subtitle={t.settings.description}
        actions={
          <FlexButton variant="lime">
            <Save className="size-4" />
            {t.common.saveChanges}
          </FlexButton>
        }
      />

      <div className="grid gap-6 xl:grid-cols-12">
        <FlexCard className="xl:col-span-8">
          <div className="pb-4">
            <p className="text-[11px] font-bold uppercase tracking-[0.12em] text-stone-500">
              {gym?.name ?? t.settings.yourGym}
            </p>
            <h2 className="mt-1 text-lg font-black tracking-tight text-stone-900">{t.settings.gymSetup}</h2>
          </div>

          {error && (
            <div className="pb-2">
              <ErrorBanner message={error} />
            </div>
          )}

          <div className="divide-y divide-stone-100">
            <div className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center">
              <FlexIconBadge>
                <Building2 className="size-5" />
              </FlexIconBadge>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold text-stone-900">{t.settings.gymProfile}</p>
                  {gym && (
                    <FlexPill tone={gymProfileComplete(gym) ? "green" : "amber"}>
                      {gymProfileComplete(gym) ? t.settings.complete : t.settings.incomplete}
                    </FlexPill>
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-stone-500">
                  {loading ? t.common.loading : gym ? gymProfileSummary(gym, t) : t.settings.gymDetailsError}
                </p>
              </div>
              <button onClick={() => setEditing(true)} disabled={!gym} className={ghostButtonClass}>
                {t.settings.editProfile}
              </button>
            </div>

            <div className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center">
              <FlexIconBadge>
                <CreditCard className="size-5" />
              </FlexIconBadge>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold text-stone-900">{t.settings.onlinePayments}</p>
                  {gym && (
                    <FlexPill tone={gym.upiId ? "green" : "amber"}>
                      {gym.upiId ? t.settings.connected : t.settings.notSet}
                    </FlexPill>
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-stone-500">
                  {loading
                    ? t.common.loading
                    : gym?.upiId
                      ? t.settings.upiDescriptionSet(gym.upiId)
                      : t.settings.upiDescriptionUnset}
                </p>
              </div>
              <button onClick={() => setEditingUpi(true)} disabled={!gym} className={ghostButtonClass}>
                {gym?.upiId ? t.settings.editUpiId : t.settings.addUpiId}
              </button>
            </div>

            <div className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center">
              <FlexIconBadge>
                <Wallet className="size-5" />
              </FlexIconBadge>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold text-stone-900">{t.settings.joiningFee}</p>
                  {gym && (
                    <FlexPill tone={gym.joiningFee != null ? "green" : "amber"}>
                      {gym.joiningFee != null ? t.settings.setBadge : t.settings.notSet}
                    </FlexPill>
                  )}
                </div>
                <p className="mt-1 text-xs leading-relaxed text-stone-500">
                  {loading
                    ? t.common.loading
                    : gym?.joiningFee != null
                      ? t.settings.joiningFeeSet(gym.joiningFeeCurrency ?? "", gym.joiningFee)
                      : t.settings.joiningFeeUnset}
                </p>
              </div>
              <button onClick={() => setEditingFee(true)} disabled={!gym} className={ghostButtonClass}>
                {gym?.joiningFee != null ? t.settings.editJoiningFee : t.settings.setJoiningFee}
              </button>
            </div>

            <div className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center">
              <FlexIconBadge>
                <Languages className="size-5" />
              </FlexIconBadge>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-bold text-stone-900">{t.settings.language}</p>
                  <FlexPill tone="lime">{languageName}</FlexPill>
                </div>
                <p className="mt-1 text-xs leading-relaxed text-stone-500">{t.settings.languageText}</p>
              </div>
              <LanguageToggle />
            </div>

            {otherSettings.map((item) => (
              <div className="flex flex-col gap-4 py-4 sm:flex-row sm:items-center" key={item.title}>
                <FlexIconBadge>
                  <item.icon className="size-5" />
                </FlexIconBadge>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-bold text-stone-900">{item.title}</p>
                    <FlexPill tone={item.status === "Setup" ? "amber" : "green"}>{item.status}</FlexPill>
                  </div>
                  <p className="mt-1 text-xs leading-relaxed text-stone-500">{item.text}</p>
                </div>
                <button type="button" className={ghostButtonClass}>
                  {item.action}
                </button>
              </div>
            ))}
          </div>
        </FlexCard>

        <section className="flex flex-col rounded-[1.75rem] bg-gradient-to-br from-[#d3f34f] to-[#a4d614] p-6 text-stone-900 shadow-[0_2px_16px_rgba(120,160,20,0.25)] xl:col-span-4">
          <div className="flex items-center justify-between">
            <span className="grid size-10 shrink-0 place-items-center rounded-2xl bg-black/[0.07] text-stone-900">
              <LockKeyhole className="size-5" />
            </span>
            <FlexPill tone="stone">{t.settings.ownerControlled}</FlexPill>
          </div>
          <p className="mt-7 text-2xl font-black tracking-tight">
            {t.settings.asideHeadlineA}
            <br />
            {t.settings.asideHeadlineB}
          </p>
          <p className="mt-4 text-xs font-medium leading-relaxed text-stone-800/80">
            {t.settings.asideText}
          </p>
          <div className="mt-auto pt-7">
            <FlexButton variant="dark">
              <Check className="size-3.5" />
              {t.settings.auditLog}
            </FlexButton>
          </div>
        </section>
      </div>

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
