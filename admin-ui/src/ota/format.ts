import type { OtaStatus, OtaUpdate } from "./api";

export function formatBytes(bytes?: number | null): string {
  if (bytes == null) return "—";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

// "2026-11-07T19:50:09" (no zone: the build machine's clock) as written.
export function formatBuildTime(iso?: string | null): string {
  if (!iso) return "—";
  return iso.replace("T", " ");
}

export function formatDateTime(iso?: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatDuration(fromIso: string, toIso?: string | null): string {
  if (!toIso) return "—";
  const ms = new Date(toIso).getTime() - new Date(fromIso).getTime();
  if (!Number.isFinite(ms) || ms < 0) return "—";
  const s = Math.round(ms / 1000);
  if (s < 60) return `${s} s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} min ${s % 60} s`;
  return `${Math.floor(m / 60)} h ${m % 60} min`;
}

export const STATUS_LABEL: Record<OtaStatus, string> = {
  requested: "Waiting for the device",
  downloading: "Downloading",
  retrying: "Will retry",
  installed: "Restarting",
  validating: "Checking",
  rolling_back: "Rolling back",
  probing: "Testing the old firmware",
  confirmed: "Updated",
  failed: "Failed",
  rolled_back: "Rolled back",
  ignored: "Ignored",
  cancelled: "Cancelled",
};

export type StatusTone = "ok" | "bad" | "neutral" | "busy";

export function statusTone(status: OtaStatus): StatusTone {
  switch (status) {
    case "confirmed":
      return "ok";
    case "failed":
    case "rolled_back":
    case "rolling_back":
      return "bad";
    case "ignored":
    case "cancelled":
      return "neutral";
    default:
      return "busy";
  }
}

// The steps the page draws for an update, from the upload to the
// device confirming the new firmware.
export const STEPS = ["Upload", "Device notified", "Download", "Install", "Check", "Done"] as const;

// How far an update got: the index of the step it is on (or reached,
// once finished), and whether that step went wrong.
export function stepOf(update: Pick<OtaUpdate, "status" | "progress">): { index: number; failed: boolean } {
  switch (update.status) {
    case "requested":
      return { index: 1, failed: false };
    case "downloading":
    case "retrying":
      return { index: 2, failed: false };
    case "installed":
      return { index: 3, failed: false };
    case "validating":
    case "probing":
      return { index: 4, failed: false };
    case "rolling_back":
    case "rolled_back":
      return { index: 4, failed: true };
    case "confirmed":
      return { index: 5, failed: false };
    case "ignored":
      return { index: 1, failed: true };
    case "failed":
    case "cancelled":
      // Where it stopped: before, during or after the download.
      if (update.progress >= 100) return { index: 3, failed: true };
      return { index: update.progress > 0 || update.status === "failed" ? 2 : 1, failed: true };
  }
}

// What an update in this state can still do from the page.
export const canCancel = (status: OtaStatus) =>
  status === "requested" || status === "downloading" || status === "retrying";

export const isFirmwareFile = (file: File) => file.name.toLowerCase().endsWith(".bin");
