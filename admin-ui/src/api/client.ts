// Every call to the server goes through here, so the JWT is attached in one
// place and an expired/invalid token signs the user out wherever it happens
// (the AuthProvider registers that handler). Before doing so it tries once to
// renew the session with the refresh token (see setAuthSession).

export class UnauthorizedError extends Error {
  constructor() {
    super("Unauthorized");
    this.name = "UnauthorizedError";
  }
}

let onUnauthorized: () => void = () => {};

export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

// The AuthProvider hooks the session in here. `getToken` is the latest access
// token (callers may hold a stale one after a renewal) and `refresh` renews it,
// resolving null when the session can't be renewed.
export interface AuthSession {
  getToken: () => string | null;
  refresh: () => Promise<string | null>;
}

let session: AuthSession | null = null;

export function setAuthSession(next: AuthSession | null): void {
  session = next;
}

function send(path: string, token: string | null, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  return fetch(path, { ...init, headers });
}

export async function apiFetch(
  path: string,
  token: string | null,
  init: RequestInit = {}
): Promise<Response> {
  const sent = session?.getToken() ?? token;
  let res = await send(path, sent, init);
  if (res.status === 401 && session) {
    // Someone else may have renewed already: only refresh if the token we
    // sent is still the current one.
    const current = session.getToken();
    const renewed = current !== null && current !== sent ? current : await session.refresh();
    if (renewed) res = await send(path, renewed, init);
  }
  if (res.status === 401) {
    onUnauthorized();
    throw new UnauthorizedError();
  }
  return res;
}

export async function apiGetJson<T>(
  path: string,
  token: string | null,
  signal?: AbortSignal
): Promise<T> {
  const res = await apiFetch(path, token, { signal });
  if (!res.ok) throw new Error(`GET ${path} failed: ${res.status}`);
  return (await res.json()) as T;
}
