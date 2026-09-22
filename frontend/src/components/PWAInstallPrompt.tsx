"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { Download, X, Share, PlusSquare, MoreVertical, Smartphone } from "lucide-react";

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
}

export default function PWAInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isStandalone, setIsStandalone] = useState<boolean>(false);
  const [showPrompt, setShowPrompt] = useState<boolean>(false);
  const [isIOS, setIsIOS] = useState<boolean>(false);
  const [isMobile, setIsMobile] = useState<boolean>(false);
  const [showHelpModal, setShowHelpModal] = useState<boolean>(false);

  useEffect(() => {
    // 1. Check if app is running in standalone mode (already installed)
    const checkStandalone = () => {
      const isStandaloneMode =
        window.matchMedia("(display-mode: standalone)").matches ||
        ("standalone" in navigator && (navigator as unknown as { standalone: boolean }).standalone);
      setIsStandalone(Boolean(isStandaloneMode));
    };

    checkStandalone();

    // 2. Detect Device Type
    const userAgent = window.navigator.userAgent.toLowerCase();
    const isIosDevice = /iphone|ipad|ipod/.test(userAgent);
    const isMobileDevice = /mobi|android|iphone|ipad|ipod/i.test(userAgent);
    setIsIOS(isIosDevice);
    setIsMobile(isMobileDevice);

    // 3. Listen for browser's beforeinstallprompt event (Android / Chrome / Edge)
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      const installEvent = e as BeforeInstallPromptEvent;
      setDeferredPrompt(installEvent);

      const isDismissed = sessionStorage.getItem("gymsuite_pwa_dismissed");
      if (!isDismissed) {
        setShowPrompt(true);
      }
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);

    // 4. Listen for appinstalled event
    const handleAppInstalled = () => {
      setDeferredPrompt(null);
      setShowPrompt(false);
      console.log("GymSuit PWA was successfully installed");
    };

    window.addEventListener("appinstalled", handleAppInstalled);

    // 5. On mobile devices (not in standalone), ensure prompt banner shows up
    if (isMobileDevice && !isStandalone) {
      const isDismissed = sessionStorage.getItem("gymsuite_pwa_dismissed");
      if (!isDismissed) {
        setShowPrompt(true);
      }
    }

    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
      window.removeEventListener("appinstalled", handleAppInstalled);
    };
  }, [isStandalone]);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      // Trigger native browser install dialog
      try {
        await deferredPrompt.prompt();
        const choiceResult = await deferredPrompt.userChoice;
        if (choiceResult.outcome === "accepted") {
          setShowPrompt(false);
        }
        setDeferredPrompt(null);
      } catch (err) {
        console.error("Install prompt error:", err);
        setShowHelpModal(true);
      }
    } else {
      // Show manual install guide for iOS or local HTTP connections
      setShowHelpModal(true);
    }
  };

  const handleDismiss = () => {
    setShowPrompt(false);
    sessionStorage.setItem("gymsuite_pwa_dismissed", "true");
  };

  // Do not render if app is running in standalone mode or prompt dismissed
  if (isStandalone || (!showPrompt && !showHelpModal)) {
    return null;
  }

  return (
    <>
      {/* Floating PWA Install Banner */}
      {showPrompt && (
        <div className="fixed bottom-4 left-4 right-4 md:left-auto md:right-6 md:w-96 z-50 animate-in fade-in slide-in-from-bottom-4 duration-300">
          <div className="bg-slate-950/95 backdrop-blur-xl border border-slate-800 shadow-2xl rounded-2xl p-4 text-white flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <div className="relative w-11 h-11 rounded-xl overflow-hidden shrink-0 bg-slate-900 border border-slate-800 flex items-center justify-center">
                <Image
                  src="/icons/icon-192.png"
                  alt="GymSuite Logo"
                  width={44}
                  height={44}
                  className="object-cover"
                  onError={(e) => {
                    (e.target as HTMLElement).style.display = "none";
                  }}
                />
              </div>
              <div className="min-w-0">
                <h4 className="font-bold text-sm text-slate-100 truncate">
                  Install GymSuit
                </h4>
                <p className="text-xs text-slate-400 truncate">
                  Install app on your phone
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <button
                onClick={handleInstallClick}
                className="inline-flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-semibold text-xs px-3.5 py-2 rounded-xl transition-all active:scale-95 shadow-lg shadow-emerald-500/20"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Install</span>
              </button>

              <button
                onClick={handleDismiss}
                className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 rounded-lg transition-colors"
                aria-label="Close"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Manual Install Instructions Modal */}
      {showHelpModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-end sm:items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 text-white max-w-sm w-full animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <Image
                  src="/icons/icon-192.png"
                  alt="GymSuite"
                  width={40}
                  height={40}
                  className="rounded-xl"
                />
                <div>
                  <h3 className="font-bold text-base text-slate-100">Install GymSuit</h3>
                  <p className="text-xs text-slate-400">
                    {isIOS ? "iOS Safari Instructions" : "Mobile App Instructions"}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowHelpModal(false)}
                className="p-1 text-slate-400 hover:text-white rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {isIOS ? (
              <div className="space-y-3.5 text-sm text-slate-300 my-5">
                <div className="flex items-start gap-3 bg-slate-800/50 p-3.5 rounded-2xl border border-slate-700/50">
                  <div className="p-2 bg-slate-700/50 rounded-xl text-emerald-400 shrink-0">
                    <Share className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-semibold text-slate-100 text-xs mb-0.5">1. Tap Share</p>
                    <p className="text-xs text-slate-400">
                      Tap the Share button in Safari&apos;s bottom toolbar.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 bg-slate-800/50 p-3.5 rounded-2xl border border-slate-700/50">
                  <div className="p-2 bg-slate-700/50 rounded-xl text-emerald-400 shrink-0">
                    <PlusSquare className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-semibold text-slate-100 text-xs mb-0.5">
                      2. Add to Home Screen
                    </p>
                    <p className="text-xs text-slate-400">
                      Scroll down and tap &quot;Add to Home Screen&quot;.
                    </p>
                  </div>
                </div>
              </div>
            ) : (
              <div className="space-y-3.5 text-sm text-slate-300 my-5">
                <div className="flex items-start gap-3 bg-slate-800/50 p-3.5 rounded-2xl border border-slate-700/50">
                  <div className="p-2 bg-slate-700/50 rounded-xl text-emerald-400 shrink-0">
                    <MoreVertical className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-semibold text-slate-100 text-xs mb-0.5">
                      1. Open Browser Menu
                    </p>
                    <p className="text-xs text-slate-400">
                      Tap the 3 dots (&#8482;) menu icon in Chrome top-right corner.
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3 bg-slate-800/50 p-3.5 rounded-2xl border border-slate-700/50">
                  <div className="p-2 bg-slate-700/50 rounded-xl text-emerald-400 shrink-0">
                    <Smartphone className="w-5 h-5" />
                  </div>
                  <div>
                    <p className="font-semibold text-slate-100 text-xs mb-0.5">
                      2. Install App or Add to Home Screen
                    </p>
                    <p className="text-xs text-slate-400">
                      Tap &quot;Install App&quot; or &quot;Add to Home screen&quot;.
                    </p>
                  </div>
                </div>
              </div>
            )}

            <button
              onClick={() => setShowHelpModal(false)}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold py-3 rounded-2xl text-sm transition-all"
            >
              Got it
            </button>
          </div>
        </div>
      )}
    </>
  );
}
