import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";

const ActiveAccountContext = createContext(null);
const LS_KEY = "dmme.activeAccountId";

export function ActiveAccountProvider({ children }) {
  const [accounts, setAccounts] = useState([]);
  const [activeAccountId, setActiveIdState] = useState(() => {
    const v = localStorage.getItem(LS_KEY);
    return v ? Number(v) : null;
  });
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(() => {
    return api
      .get("/api/instagram/accounts")
      .then((d) => {
        const list = Array.isArray(d) ? d : [];
        setAccounts(list);
        return list;
      })
      .catch(() => {
        setAccounts([]);
        return [];
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Keep the selected account valid; default to the first connected account.
  useEffect(() => {
    if (loading) return;
    if (accounts.length === 0) {
      if (activeAccountId !== null) setActiveIdState(null);
      return;
    }
    if (!accounts.some((a) => a.id === activeAccountId)) {
      setActiveIdState(accounts[0].id);
    }
  }, [accounts, loading, activeAccountId]);

  const setActiveAccountId = useCallback((id) => {
    setActiveIdState(id);
    if (id == null) localStorage.removeItem(LS_KEY);
    else localStorage.setItem(LS_KEY, String(id));
  }, []);

  const activeAccount = useMemo(
    () => accounts.find((a) => a.id === activeAccountId) || null,
    [accounts, activeAccountId]
  );

  const value = useMemo(
    () => ({ accounts, activeAccount, activeAccountId, setActiveAccountId, loading, refresh }),
    [accounts, activeAccount, activeAccountId, setActiveAccountId, loading, refresh]
  );

  return <ActiveAccountContext.Provider value={value}>{children}</ActiveAccountContext.Provider>;
}

export function useActiveAccount() {
  return (
    useContext(ActiveAccountContext) || {
      accounts: [],
      activeAccount: null,
      activeAccountId: null,
      setActiveAccountId: () => {},
      loading: false,
      refresh: () => {},
    }
  );
}
