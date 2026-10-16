// Mirrors the server's LogEntryDto (com.server.croniot.livelog). Fields the
// server leaves out (a server line has no device, a device line no thread)
// are simply absent.

export type LogPlatform = "SERVER" | "IOT" | "ANDROID" | "IOS";
export type LogKind = "LOG" | "EVENT";

export const PLATFORMS: LogPlatform[] = ["SERVER", "IOT", "ANDROID", "IOS"];
export const KINDS: LogKind[] = ["LOG", "EVENT"];

// Same scale as croniot-iot and Logback: lower is more severe.
export const LEVEL = { ERROR: 0, WARN: 1, INFO: 2, DEBUG: 3, TRACE: 4 } as const;
export const LEVELS = [0, 1, 2, 3, 4] as const;
export const LEVEL_NAMES = ["Error", "Warn", "Info", "Debug", "Trace"] as const;

export interface LogDeviceRef {
  uuid: string;
  name?: string | null;
}

export interface LogOwnerRef {
  uuid: string;
  nickname?: string | null;
  email: string;
}

export interface LogEntry {
  id: string;
  platform: LogPlatform;
  kind: LogKind;
  level: number;
  tag: string;
  message: string;
  receivedAtMs: number;
  device?: LogDeviceRef | null;
  owner?: LogOwnerRef | null;
  bootId?: number | null;
  seq?: number | null;
  uptimeMs?: number | null;
  thread?: string | null;
  traceId?: string | null;
  throwable?: string | null;
}

// The stream fell behind and lost `count` entries received between
// fromMs and toMs (all platforms).
export interface DroppedNotice {
  count: number;
  fromMs: number;
  toMs: number;
}

// Empty array = no filter on that field.
export interface ServerLogFilter {
  platforms: LogPlatform[];
  kinds: LogKind[];
  levels: number[];
  devices: string[];
  owners: string[];
}

export const NO_FILTER: ServerLogFilter = { platforms: [], kinds: [], levels: [], devices: [], owners: [] };

export function filterParams(filter: ServerLogFilter): URLSearchParams {
  const params = new URLSearchParams();
  filter.platforms.forEach((v) => params.append("platform", v));
  filter.kinds.forEach((v) => params.append("kind", v));
  filter.levels.forEach((v) => params.append("level", String(v)));
  filter.devices.forEach((v) => params.append("device", v));
  filter.owners.forEach((v) => params.append("owner", v));
  return params;
}
