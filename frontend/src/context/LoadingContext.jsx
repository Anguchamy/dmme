import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { FullPageLoader } from "../components/Loader";

const LoadingContext = createContext(null);

/**
 * Global branded loading overlay.
 * - Route changes under /app show the loader until the page marks ready
 * - Login / logout / other actions use runWithLoader
 */
export function LoadingProvider({ children }) {
  const [pageLoading, setPageLoading] = useState(false);
  const [actionCount, setActionCount] = useState(0);
  const [label, setLabel] = useState("Loading…");
  const location = useLocation();
  const prevPath = useRef(location.pathname);

  const beginAction = useCallback((nextLabel = "Loading…") => {
    if (nextLabel) setLabel(nextLabel);
    setActionCount((n) => n + 1);
  }, []);

  const endAction = useCallback(() => {
    setActionCount((n) => Math.max(0, n - 1));
  }, []);

  const setPageBusy = useCallback((on, nextLabel = "Loading…") => {
    if (on && nextLabel) setLabel(nextLabel);
    setPageLoading(!!on);
  }, []);

  const runWithLoader = useCallback(async (fn, nextLabel = "Loading…") => {
    beginAction(nextLabel);
    try {
      return await fn();
    } finally {
      endAction();
    }
  }, [beginAction, endAction]);

  // Navigating between app pages: show the brand loader until the page clears it.
  useEffect(() => {
    if (prevPath.current === location.pathname) return;
    prevPath.current = location.pathname;
    if (location.pathname.startsWith("/app")) {
      setLabel("Loading…");
      setPageLoading(true);
    } else {
      setPageLoading(false);
    }
  }, [location.pathname]);

  const busy = pageLoading || actionCount > 0;

  const value = {
    beginAction,
    endAction,
    setPageBusy,
    runWithLoader,
    busy,
  };

  return (
    <LoadingContext.Provider value={value}>
      {children}
      {busy && <FullPageLoader label={label} />}
    </LoadingContext.Provider>
  );
}

export function useLoading() {
  const ctx = useContext(LoadingContext);
  if (!ctx) {
    throw new Error("useLoading must be used within LoadingProvider");
  }
  return ctx;
}

/**
 * Tie a page's async ready-state to the global brand loader.
 * Pass true while fetching; false once the UI has everything it needs.
 */
export function usePageLoading(isLoading, label = "Loading…") {
  const { setPageBusy } = useLoading();
  useEffect(() => {
    setPageBusy(!!isLoading, label);
    return () => setPageBusy(false);
  }, [isLoading, label, setPageBusy]);
}
