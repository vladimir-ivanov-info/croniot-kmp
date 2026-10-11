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
