"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { resolveDictionary, type Dictionary, type Locale } from "./dictionaries";

const STORAGE_KEY = "gymsuite_locale";

const LanguageContext = createContext<{
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: Dictionary;
}>({ locale: "en", setLocale: () => {}, t: resolveDictionary("en") });

export function LanguageProvider({
  children,
  initialLocale,
}: {
  children: ReactNode;
  initialLocale: Locale;
}) {
  // The server reads the locale cookie and passes it in, so the first paint
  // already matches and there is no hydration mismatch.
  const [locale, setLocaleState] = useState<Locale>(initialLocale);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const setLocale = (next: Locale) => {
    setLocaleState(next);
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
      document.cookie = `${STORAGE_KEY}=${next}; path=/; max-age=31536000; SameSite=Lax`;
    } catch {
      /* storage unavailable — locale still applies for this session */
    }
  };

  const t = useMemo(() => resolveDictionary(locale), [locale]);

  return <LanguageContext.Provider value={{ locale, setLocale, t }}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  return useContext(LanguageContext);
}
