import type { LogEntry } from "./types";

const pad = (n: number, width = 2) => String(n).padStart(width, "0");

// Local time with milliseconds; the date is added when it is not today.
export function formatTime(ms: number, now = new Date()): string {
  const d = new Date(ms);
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
  const sameDay = d.toDateString() === now.toDateString();
  return sameDay ? time : `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`;
}

export function deviceLabel(entry: LogEntry): string | null {
  return entry.device ? entry.device.name || entry.device.uuid : null;
}

export function ownerLabel(entry: LogEntry): string | null {
  return entry.owner ? entry.owner.nickname || entry.owner.email : null;
}

export const PLATFORM_LABELS: Record<string, string> = {
  SERVER: "SRV",
  IOT: "IoT",
  ANDROID: "Android",
  IOS: "iOS",
};

// "just now", "5 min ago", "3 h ago", "2 d ago".
export function formatAgo(ms: number, now = Date.now()): string {
  const s = Math.max(0, Math.round((now - ms) / 1000));
  if (s < 45) return "just now";
  if (s < 3600) return `${Math.round(s / 60)} min ago`;
  if (s < 86400) return `${Math.round(s / 3600)} h ago`;
  return `${Math.round(s / 86400)} d ago`;
}

// Share of received records that were duplicates, e.g. "12.5 %"; null when
// nothing was received.
export function duplicateRatio(received: number, duplicated: number): string | null {
  if (received <= 0) return null;
  return `${((duplicated / received) * 100).toFixed(1)} %`;
}
