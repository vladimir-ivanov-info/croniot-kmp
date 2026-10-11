import type { FeatureFlag } from "../types";
import { apiFetch } from "./client";

export async function fetchFlags(): Promise<FeatureFlag[]> {
  const res = await apiFetch("/api/feature_flags", null);
  if (!res.ok) throw new Error("Failed to fetch flags");
  return res.json();
}

// A 401 signs the user out (see api/client.ts) and throws UnauthorizedError.
export async function setFlag(
  name: string,
  enabled: boolean,
  token: string
): Promise<FeatureFlag> {
  const res = await apiFetch(`/api/admin/feature_flags/${encodeURIComponent(name)}`, token, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled }),
  });
  if (!res.ok) throw new Error("Failed to update flag");
  return res.json();
}
