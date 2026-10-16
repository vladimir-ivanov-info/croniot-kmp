import { apiFetch } from "../api/client";

// Mirrors the server's ServerLogLevelDto.
export interface ServerLogLevel {
  root: string;
  overrides: Record<string, string>;
  revertsAtMs?: number | null;
}

export const SERVER_LEVELS = ["ERROR", "WARN", "INFO", "DEBUG", "TRACE"] as const;
const PATH = "/api/admin/server/log_level";

async function json(res: Response): Promise<ServerLogLevel> {
  if (!res.ok) throw new Error(`${PATH} failed: ${res.status}`);
  return res.json();
}

export const getServerLogLevel = (token: string | null, signal?: AbortSignal) =>
  apiFetch(PATH, token, { signal }).then(json);

export const setServerLogLevel = (token: string | null, level: string, ttlMinutes = 30) =>
  apiFetch(PATH, token, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ level, ttlMinutes }),
  }).then(json);

export const revertServerLogLevel = (token: string | null) =>
  apiFetch(PATH, token, { method: "DELETE" }).then(json);
