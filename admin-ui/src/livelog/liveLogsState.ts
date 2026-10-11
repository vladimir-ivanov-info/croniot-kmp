import type { LogEntry } from "./types";

// What the viewer holds, newest first. While paused, live entries go to
// `pending` instead, so the list stops moving but nothing is lost; resume
// moves them in.
export interface LiveLogsState {
  entries: LogEntry[];
  pending: LogEntry[];
  paused: boolean;
  // An older history page is being fetched.
  loadingOlder: boolean;
  // History has nothing older than what is shown.
  reachedStart: boolean;
}

export const MAX_ENTRIES = 10_000;

export const initialLiveLogsState: LiveLogsState = { entries: [], pending: [], paused: false, loadingOlder: false, reachedStart: false };

export type LiveLogsAction =
  // Live entries, and gap refills (they may be newer than what is shown).
  | { type: "receive"; entries: LogEntry[] }
  // Older pages of history: they go below what is shown, so they are added
  // even while paused - the visible part of the list does not move.
  | { type: "history"; entries: LogEntry[]; reachedStart: boolean }
  | { type: "loadingOlder" }
  | { type: "pause" }
  | { type: "resume" }
  | { type: "reset" };

// Newest first; ties broken by id descending (the server orders the same
// way), so live and history interleave identically.
export function compareNewestFirst(a: LogEntry, b: LogEntry): number {
  if (a.receivedAtMs !== b.receivedAtMs) return b.receivedAtMs - a.receivedAtMs;
  return a.id < b.id ? 1 : a.id > b.id ? -1 : 0;
}

// Union by id (an entry seen live and again in history is kept once),
// sorted newest first, trimmed to `max` by dropping the oldest.
export function mergeEntries(current: LogEntry[], incoming: LogEntry[], max = MAX_ENTRIES): LogEntry[] {
  if (incoming.length === 0) return current;
  const seen = new Set(current.map((e) => e.id));
  const fresh = incoming.filter((e) => !seen.has(e.id) && seen.add(e.id));
  if (fresh.length === 0) return current;
  return [...current, ...fresh].sort(compareNewestFirst).slice(0, max);
}

export function liveLogsReducer(state: LiveLogsState, action: LiveLogsAction): LiveLogsState {
  switch (action.type) {
    case "receive": {
      if (!state.paused) return { ...state, entries: mergeEntries(state.entries, action.entries) };
      const shown = new Set(state.entries.map((e) => e.id));
      const notShown = action.entries.filter((e) => !shown.has(e.id));
      return { ...state, pending: mergeEntries(state.pending, notShown) };
    }
    case "history":
      return {
        ...state,
        entries: mergeEntries(state.entries, action.entries),
        loadingOlder: false,
        reachedStart: state.reachedStart || action.reachedStart,
      };
    case "loadingOlder":
      return { ...state, loadingOlder: true };
    case "pause":
      return state.paused ? state : { ...state, paused: true };
    case "resume":
      return { ...state, entries: mergeEntries(state.entries, state.pending), pending: [], paused: false };
    case "reset":
      return initialLiveLogsState;
  }
}
