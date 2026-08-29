import { authFetch } from "@/lib/auth";

export type NotificationType = "PAYMENT_REMINDER" | "PROMOTION" | "COMEBACK_REMINDER" | "CUSTOM";
export type NotificationChannel = "WHATSAPP" | "SMS" | "EMAIL";

export function notifyMember(
  gymId: number,
  memberId: number,
  type: NotificationType,
  channel: NotificationChannel,
  message: string
) {
  return authFetch<void>(`/api/gyms/${gymId}/members/${memberId}/notify`, {
    method: "POST",
    body: JSON.stringify({ type, channel, message }),
  });
}

/** Builds a wa.me link that opens WhatsApp with the message pre-filled for this member's number. */
export function whatsAppLink(phone: string, message: string) {
  const digits = phone.replace(/\D/g, "");
  // Most numbers in this app are entered without a country code — assume India (91) for bare 10-digit numbers.
  const withCountryCode = digits.length === 10 ? `91${digits}` : digits;
  return `https://wa.me/${withCountryCode}?text=${encodeURIComponent(message)}`;
}
