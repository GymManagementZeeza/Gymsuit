"use client";

import { createPortal } from "react-dom";
import { AlertTriangle } from "lucide-react";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export default function ConfirmDialog({
  title,
  description,
  confirmLabel,
  cancelLabel,
  onConfirm,
  onCancel,
  disabled = false,
}: {
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  disabled?: boolean;
}) {
  const { t } = useLanguage();
  const finalConfirmLabel = confirmLabel ?? t.common.confirm;
  const finalCancelLabel = cancelLabel ?? t.common.cancel;
  return createPortal(
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-[#141410]/50 p-4">
      <div className="w-full max-w-sm border border-[#d8d8d1] bg-white p-5">
        <div className="flex items-start gap-3">
          <span className="grid size-9 shrink-0 place-items-center bg-red-50 text-red-600">
            <AlertTriangle className="size-4.5" />
          </span>
          <div>
            <h2 className="text-base font-bold tracking-[-0.02em]">{title}</h2>
            <p className="mt-1.5 text-sm leading-6 text-[#666660]">{description}</p>
          </div>
        </div>
        <div className="mt-6 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={disabled}
            className="h-9 border border-[#d8d8d1] px-4 text-xs font-bold transition hover:bg-[#f7f7f2] disabled:opacity-50"
          >
            {finalCancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={disabled}
            className="h-9 bg-red-600 px-4 text-xs font-bold text-white transition hover:bg-red-700 disabled:opacity-50"
          >
            {finalConfirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
