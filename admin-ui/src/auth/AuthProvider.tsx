import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { refreshTokens, revokeRefreshToken, type Tokens } from "../api/auth";
import { setAuthSession, setUnauthorizedHandler } from "../api/client";
import { AuthContext } from "./AuthContext";

export const TOKEN_KEY = "croniot_admin_token";
export const REFRESH_TOKEN_KEY = "croniot_admin_refresh_token";

function readItem(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeItem(key: string, value: string | null): void {
  try {
    if (value) localStorage.setItem(key, value);
    else localStorage.removeItem(key);
  } catch {
    // Storage unavailable (private mode...): the session just won't survive a reload.
  }
}

function writeTokens(tokens: Tokens | null): void {
  writeItem(TOKEN_KEY, tokens?.token ?? null);
  writeItem(REFRESH_TOKEN_KEY, tokens?.refreshToken ?? null);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => readItem(TOKEN_KEY));
  // Latest access token. Renewals update it without touching `token`, so the
  // live log stream and the pages don't restart every 15 minutes.
  const latest = useRef<string | null>(token);
  const pendingRefresh = useRef<Promise<string | null> | null>(null);

  const login = useCallback((tokens: Tokens) => {
    writeTokens(tokens);
    latest.current = tokens.token;
    setToken(tokens.token);
  }, []);

  const logout = useCallback(() => {
    const refreshToken = readItem(REFRESH_TOKEN_KEY);
    if (refreshToken) void revokeRefreshToken(refreshToken);
    writeTokens(null);
    latest.current = null;
    setToken(null);
  }, []);

  // A network error rejects (and signs nobody out); only a refused token resolves null.
  // One renewal at a time: the server rotates refresh tokens, so two parallel
  // calls with the same one would make the second fail and sign the user out.
  const refresh = useCallback((): Promise<string | null> => {
    if (pendingRefresh.current) return pendingRefresh.current;
    const refreshToken = readItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) return Promise.resolve(null);
    const pending = refreshTokens(refreshToken)
      .then((tokens) => {
        if (!tokens) return null;
        writeTokens(tokens);
        latest.current = tokens.token;
        return tokens.token;
      })
      .finally(() => {
        pendingRefresh.current = null;
      });
    pendingRefresh.current = pending;
    return pending;
  }, []);

  // Any 401 that renewing can't fix (refresh token expired, revoked admin...) signs out.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    setAuthSession({ getToken: () => latest.current, refresh });
    return () => {
      setUnauthorizedHandler(() => {});
      setAuthSession(null);
    };
  }, [logout, refresh]);

  const value = useMemo(() => ({ token, login, logout }), [token, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
