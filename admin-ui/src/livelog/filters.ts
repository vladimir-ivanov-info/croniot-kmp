import { KINDS, LEVEL, LEVELS, PLATFORMS, type LogEntry, type LogKind, type LogPlatform, type ServerLogFilter } from "./types";

// Everything the viewer can filter on. Empty array / empty text = no filter
// on that field, so the default (nothing chosen) shows everything.
export interface LogFilter extends ServerLogFilter {
  tags: string[];
  text: string;
}

export const EMPTY_FILTER: LogFilter = { platforms: [], kinds: [], levels: [], devices: [], owners: [], tags: [], text: "" };

export const SERIOUS_LEVELS = [LEVEL.ERROR, LEVEL.WARN];

// The part the server applies (to the live stream and history); tags and
// text are applied here, in the browser.
export function serverPart(filter: LogFilter): ServerLogFilter {
  return { platforms: filter.platforms, kinds: filter.kinds, levels: filter.levels, devices: filter.devices, owners: filter.owners };
}

export function matchesClient(entry: LogEntry, filter: LogFilter): boolean {
  if (filter.tags.length > 0 && !filter.tags.includes(entry.tag)) return false;
  const text = filter.text.trim().toLowerCase();
  if (!text) return true;
  return [entry.message, entry.tag, entry.device?.name, entry.device?.uuid, entry.owner?.nickname, entry.owner?.email]
    .some((field) => field?.toLowerCase().includes(text));
}

// A chip shows as selected when its value passes the filter: with nothing
// chosen, all of them do.
export function isSelected<T>(chosen: T[], value: T): boolean {
  return chosen.length === 0 || chosen.includes(value);
}

// Toggles one value of a multi-valued field whose possible values are
// `all`. Choosing every value is the same as choosing none, and is stored
// as none, so the URL stays short and "everything" has one spelling.
export function toggleValue<T>(chosen: T[], value: T, all: readonly T[]): T[] {
  const current = chosen.length === 0 ? [...all] : chosen;
  const next = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
  return next.length === all.length ? [] : next;
}

// --- URL round trip (so a filtered view can be shared and Back works) ---

const list = (params: URLSearchParams, key: string) =>
  params.getAll(key).flatMap((v) => v.split(",")).map((v) => v.trim()).filter(Boolean);

export function filterFromSearchParams(params: URLSearchParams): LogFilter {
  return {
    platforms: list(params, "platform").filter((v): v is LogPlatform => (PLATFORMS as string[]).includes(v)),
    kinds: list(params, "kind").filter((v): v is LogKind => (KINDS as string[]).includes(v)),
    levels: list(params, "level").map(Number).filter((v) => (LEVELS as readonly number[]).includes(v)),
    devices: list(params, "device"),
    owners: list(params, "owner"),
    tags: list(params, "tag"),
    text: params.get("q") ?? "",
  };
}

export function filterToSearchParams(filter: LogFilter): URLSearchParams {
  const params = new URLSearchParams();
  filter.platforms.forEach((v) => params.append("platform", v));
  filter.kinds.forEach((v) => params.append("kind", v));
  filter.levels.forEach((v) => params.append("level", String(v)));
  filter.devices.forEach((v) => params.append("device", v));
  filter.owners.forEach((v) => params.append("owner", v));
  filter.tags.forEach((v) => params.append("tag", v));
  if (filter.text) params.set("q", filter.text);
  return params;
}
