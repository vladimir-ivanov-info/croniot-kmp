// Every call to the server goes through here, so the JWT is attached in one
// place and an expired/invalid token signs the user out wherever it happens
// (the AuthProvider registers that handler).

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

export async function apiFetch(
  path: string,
  token: string | null,
  init: RequestInit = {}
): Promise<Response> {
  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const res = await fetch(path, { ...init, headers });
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
