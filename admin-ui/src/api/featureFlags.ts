import type { FeatureFlag } from "../types";

export async function fetchFlags(): Promise<FeatureFlag[]> {
  const res = await fetch("/api/feature_flags");
  if (!res.ok) throw new Error("Failed to fetch flags");
  return res.json();
}

export async function setFlag(
  name: string,
  enabled: boolean,
  token: string
): Promise<FeatureFlag> {
  const res = await fetch(`/api/admin/feature_flags/${encodeURIComponent(name)}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ enabled }),
  });

  if (res.status === 401) throw new Error("Unauthorized");
  if (!res.ok) throw new Error("Failed to update flag");

  return res.json();
}
