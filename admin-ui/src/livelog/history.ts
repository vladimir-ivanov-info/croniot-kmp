import { apiGetJson } from "../api/client";
import { filterParams, type LogEntry, type ServerLogFilter } from "./types";

export interface HistoryQuery {
  filter: ServerLogFilter;
  tags?: string[];
  text?: string;
  fromMs?: number;
  toMs?: number;
  before?: { ms: number; id: string };
  limit?: number;
}

// GET /api/admin/logs: stored entries, newest first.
export function fetchHistory(token: string | null, query: HistoryQuery, signal?: AbortSignal): Promise<LogEntry[]> {
  const params = filterParams(query.filter);
  query.tags?.forEach((t) => params.append("tag", t));
  if (query.text) params.set("q", query.text);
  if (query.fromMs !== undefined) params.set("fromMs", String(query.fromMs));
  if (query.toMs !== undefined) params.set("toMs", String(query.toMs));
  if (query.before) {
    params.set("before", String(query.before.ms));
    params.set("beforeId", query.before.id);
  }
  if (query.limit !== undefined) params.set("limit", String(query.limit));
  return apiGetJson<LogEntry[]>(`/api/admin/logs?${params}`, token, signal);
}
