import { apiFetch, apiGetJson, apiUpload } from "../api/client";

// Mirrors the server's com.server.croniot.ota DTOs (OtaDtos.kt).
export interface FirmwareRelease {
  id: number;
  project: string;
  version: string;
  sizeBytes: number;
  sha256: string;
  createdAt: string;
  builtAt?: string | null;
  idfVersion?: string | null;
}

export interface InstalledFirmware {
  project: string;
  version: string;
  builtAt?: string | null;
  idfVersion?: string | null;
  sizeBytes?: number | null;
  slot?: string | null;
  reportedAt: string;
  release?: FirmwareRelease | null;
}

export type OtaStatus =
  | "requested"
  | "downloading"
  | "retrying"
  | "installed"
  | "validating"
  | "rolling_back"
  | "probing"
  | "confirmed"
  | "failed"
  | "rolled_back"
  | "ignored"
  | "cancelled";

export interface OtaUpdate {
  requestId: string;
  releaseId?: number | null;
  version: string;
  fromVersion?: string | null;
  status: OtaStatus;
  finished: boolean;
  progress: number;
  detail?: string | null;
  requestedAt: string;
  updatedAt: string;
  finishedAt?: string | null;
}

export interface FirmwareOverview {
  installed?: InstalledFirmware | null;
  current?: OtaUpdate | null;
  history: OtaUpdate[];
}

const devicePath = (uuid: string) => `/api/admin/devices/${encodeURIComponent(uuid)}`;

export const fetchFirmwareOverview = (token: string | null, uuid: string, signal?: AbortSignal) =>
  apiGetJson<FirmwareOverview>(`${devicePath(uuid)}/firmware`, token, signal);

// The server answers errors as {code, message}; show its message.
async function errorMessage(res: Response | { status: number; body: string }, fallback: string) {
  const text = res instanceof Response ? await res.text() : res.body;
  try {
    const parsed = JSON.parse(text) as { message?: string };
    if (parsed.message) return parsed.message;
  } catch {
    // not JSON
  }
  return `${fallback} (HTTP ${res.status})`;
}

export async function uploadFirmware(
  token: string | null,
  file: Blob,
  onProgress: (fraction: number) => void,
  signal?: AbortSignal
): Promise<FirmwareRelease> {
  const res = await apiUpload("/api/admin/firmware", token, file, (sent, total) => onProgress(sent / total), signal);
  if (res.status !== 200 && res.status !== 201) {
    throw new Error(await errorMessage(res, "Upload failed"));
  }
  return JSON.parse(res.body) as FirmwareRelease;
}

export async function requestUpdate(token: string | null, uuid: string, releaseId: number): Promise<void> {
  const res = await apiFetch(`${devicePath(uuid)}/ota`, token, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ releaseId }),
  });
  if (!res.ok) throw new Error(await errorMessage(res, "Could not start the update"));
}

export async function cancelUpdate(token: string | null, uuid: string): Promise<void> {
  const res = await apiFetch(`${devicePath(uuid)}/ota`, token, { method: "DELETE" });
  if (!res.ok) throw new Error(await errorMessage(res, "Could not cancel the update"));
}
