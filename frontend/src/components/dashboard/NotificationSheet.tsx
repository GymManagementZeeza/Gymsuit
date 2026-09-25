"use client";

import { Bell, X } from "lucide-react";

export default function NotificationSheet({ onClose }: { onClose: () => void }) {
  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-black/40"
        onClick={onClose}
        aria-hidden="true"
      />
      <aside
        role="dialog"
        aria-label="Notifications"
        className="fixed inset-y-0 right-0 z-50 flex w-full max-w-sm flex-col bg-white shadow-xl"
      >
        <div className="flex items-center justify-between border-b border-[#e5e5de] px-5 py-4">
          <h2 className="text-base font-bold tracking-[-0.01em]">Notifications</h2>
          <button
            type="button"
            onClick={onClose}
            className="grid size-8 place-items-center border border-[#d8d8d1] transition hover:border-[#24241f]"
            aria-label="Close notifications"
          >
            <X className="size-4" />
          </button>
        </div>
        <div className="flex flex-1 flex-col items-center justify-center gap-3 p-8 text-center">
          <span className="grid size-12 place-items-center rounded-full bg-[#f1f1eb] text-[#8a8a82]">
            <Bell className="size-5" />
          </span>
          <p className="text-sm font-bold">No notifications</p>
          <p className="max-w-[240px] text-xs leading-5 text-[#8a8a82]">
            You&apos;re all caught up. New alerts will appear here.
          </p>
        </div>
      </aside>
    </>
  );
}
