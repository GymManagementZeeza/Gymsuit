"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import { Bell, Gift, Mail, MessageCircle, MessageSquare, RotateCcw, Smartphone, X } from "lucide-react";
import ErrorBanner from "@/components/dashboard/ErrorBanner";
import type { Member } from "@/lib/members";
import { notifyMember, whatsAppLink, type NotificationChannel, type NotificationType } from "@/lib/notifications";

const CHANNEL_OPTIONS: { value: NotificationChannel; label: string; icon: typeof MessageCircle; enabled: boolean }[] = [
  { value: "WHATSAPP", label: "WhatsApp", icon: MessageCircle, enabled: true },
  { value: "SMS", label: "SMS", icon: Smartphone, enabled: false },
  { value: "EMAIL", label: "Email", icon: Mail, enabled: false },
];

const NOTIFICATION_OPTIONS: {
  value: NotificationType;
  label: string;
  description: string;
  icon: typeof Bell;
  template: (member: Member) => string;
}[] = [
  {
    value: "PAYMENT_REMINDER",
    label: "Payment reminder",
    description: "Nudge them about a due or upcoming payment.",
    icon: Bell,
    template: (member) =>
      `Hi ${member.firstName}, just a reminder that your membership payment is due. Please settle it at your earliest convenience — let us know if you have any questions!`,
  },
  {
    value: "PROMOTION",
    label: "Promotion",
    description: "Share an offer, discount, or new class.",
    icon: Gift,
    template: (member) =>
      `Hi ${member.firstName}, we've got something special for you! Ask us at the front desk about our current offers.`,
  },
  {
    value: "COMEBACK_REMINDER",
    label: "Come back to the gym",
    description: "Nudge a member who hasn't been in for a while.",
    icon: RotateCcw,
    template: (member) =>
      `Hi ${member.firstName}, we miss you at the gym! Come back and pick up where you left off — we'd love to see you again.`,
  },
  {
    value: "CUSTOM",
    label: "Custom message",
    description: "Write anything else.",
    icon: MessageSquare,
    template: () => "",
  },
];

export default function NotifyModal({
  gymId,
  member,
  onClose,
  onSent,
}: {
  gymId: number;
  member: Member;
  onClose: () => void;
  onSent: () => void;
}) {
  const [selected, setSelected] = useState<NotificationType | null>(null);
  const [channel, setChannel] = useState<NotificationChannel>("WHATSAPP");
  const [message, setMessage] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectOption = (option: (typeof NOTIFICATION_OPTIONS)[number]) => {
    setSelected(option.value);
    setMessage(option.template(member));
  };

  const handleSend = async () => {
    if (!selected || !message.trim()) return;
    setError(null);
    setSending(true);
    const finalMessage = message.trim();
    try {
      await notifyMember(gymId, member.id, selected, channel, finalMessage);
      if (channel === "WHATSAPP") {
        window.open(whatsAppLink(member.phone, finalMessage), "_blank", "noopener,noreferrer");
      }
      onSent();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send this notification.");
      setSending(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto border border-[#d8d8d1] bg-white">
        <div className="flex items-center justify-between border-b border-[#e5e5de] p-5">
          <div>
            <p className="ledger-label">Notify</p>
            <h2 className="mt-2 text-lg font-bold tracking-[-0.02em]">
              {member.firstName} {member.lastName}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="grid size-8 place-items-center transition hover:bg-[#efefe9]"
            aria-label="Close"
          >
            <X className="size-4" />
          </button>
        </div>

        <div className="flex flex-col gap-5 p-5">
          <div className="grid grid-cols-2 gap-2">
            {NOTIFICATION_OPTIONS.map((option) => {
              const Icon = option.icon;
              const active = selected === option.value;
              return (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => selectOption(option)}
                  className={`flex flex-col items-start gap-2 border p-3 text-left transition ${
                    active ? "border-[#24241f] bg-[#fafaf6]" : "border-[#d8d8d1] hover:bg-[#fafaf6]"
                  }`}
                >
                  <span className="grid size-8 place-items-center rounded-full bg-[#c4d8fb] text-[#24241f]">
                    <Icon className="size-4" />
                  </span>
                  <span>
                    <span className="block text-sm font-bold">{option.label}</span>
                    <span className="block text-[11px] text-[#76766f]">{option.description}</span>
                  </span>
                </button>
              );
            })}
          </div>

          {selected && (
            <>
              <div className="flex flex-col gap-1.5">
                <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Send via</span>
                <div className="grid grid-cols-3 gap-2">
                  {CHANNEL_OPTIONS.map((option) => {
                    const Icon = option.icon;
                    const active = channel === option.value;
                    return (
                      <button
                        key={option.value}
                        type="button"
                        disabled={!option.enabled}
                        onClick={() => setChannel(option.value)}
                        className={`flex h-16 flex-col items-center justify-center gap-1 border text-xs font-bold transition disabled:cursor-not-allowed disabled:opacity-40 ${
                          active ? "border-[#24241f] bg-[#24241f] text-white" : "border-[#d8d8d1] bg-white"
                        }`}
                      >
                        <Icon className="size-4" />
                        {option.label}
                        {!option.enabled && <span className="text-[9px] font-normal opacity-70">Coming soon</span>}
                      </button>
                    );
                  })}
                </div>
              </div>

              <label className="flex flex-col gap-1.5">
                <span className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#76766f]">Message</span>
                <textarea
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  rows={4}
                  className="border border-[#d8d8d1] bg-white p-3 text-sm outline-none transition-colors focus:border-[#24241f]"
                />
              </label>
            </>
          )}

          <p className="text-[11px] text-[#8a8a82]">
            {channel === "WHATSAPP"
              ? "This opens WhatsApp with the message pre-filled to send yourself — nothing is sent automatically."
              : "SMS and email delivery aren't wired up yet — sending just records it to the gym's activity feed."}
          </p>

          {error && <ErrorBanner message={error} />}

          <div className="flex items-center justify-end gap-2 border-t border-[#e5e5de] pt-5">
            <button
              type="button"
              onClick={onClose}
              className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2]"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSend}
              disabled={!selected || !message.trim() || sending}
              className="flex h-9 items-center gap-2 bg-[#c7f36a] px-4 text-xs font-bold text-[#25251f] transition hover:bg-[#d8ff8a] disabled:opacity-60"
            >
              {sending ? "Sending…" : channel === "WHATSAPP" ? "Open WhatsApp" : "Send"}
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
}
