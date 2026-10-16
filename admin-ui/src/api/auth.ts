export interface Tokens {
  token: string;
  refreshToken: string | null;
}

export async function login(email: string, password: string): Promise<Tokens> {
  const res = await fetch("/api/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email,
      password,
      deviceUuid: "admin-web",
      deviceToken: null,
      deviceProperties: {},
    }),
  });

  if (!res.ok) throw new Error("Login failed");

  const data = await res.json();
  if (!data.result?.success || !data.token) throw new Error("Invalid credentials");

  return { token: data.token as string, refreshToken: (data.refreshToken as string | null) ?? null };
}

// Trades a refresh token for a new pair (the server rotates it: the old one
// stops working). Resolves null when the server rejects it (expired or
// revoked: the user must sign in again) and throws on network errors, which
// must not sign anybody out.
export async function refreshTokens(refreshToken: string): Promise<Tokens | null> {
  const res = await fetch("/api/token/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (res.status === 401 || res.status === 403) return null;
  if (!res.ok) throw new Error(`refresh failed: ${res.status}`);

  const data = await res.json();
  if (!data.result?.success || !data.token) return null;
  return { token: data.token as string, refreshToken: (data.refreshToken as string | null) ?? null };
}

// Best effort: revokes the refresh token server side when signing out.
export async function revokeRefreshToken(refreshToken: string): Promise<void> {
  try {
    await fetch("/api/logout", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    // Offline: the token will simply expire.
  }
}
