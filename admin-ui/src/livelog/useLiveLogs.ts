import { useCallback, useEffect, useReducer, useRef, useState } from "react";
import { fetchHistory } from "./history";
import { initialLiveLogsState, liveLogsReducer } from "./liveLogsState";
import { openLogStream, type StreamStatus } from "./logStream";
import type { LogEntry, ServerLogFilter } from "./types";

// Live entries are applied in small batches rather than one render per
// line: a device in DEBUG can send hundreds per second.
const FLUSH_INTERVAL_MS = 200;
const GAP_REFILL_LIMIT = 1000;
export const HISTORY_PAGE_SIZE = 200;

export function useLiveLogs(token: string | null, filter: ServerLogFilter) {
  const [state, dispatch] = useReducer(liveLogsReducer, initialLiveLogsState);
  const [status, setStatus] = useState<StreamStatus>("connecting");
  const queue = useRef<LogEntry[]>([]);
  const entriesRef = useRef<LogEntry[]>([]);
  useEffect(() => {
    entriesRef.current = state.entries;
  }, [state.entries]);
  const loadingOlderRef = useRef(false);
  const generation = useRef(0);

  // Reopen everything when the server-side filter changes.
  const filterKey = JSON.stringify(filter);

  useEffect(() => {
    const currentFilter = JSON.parse(filterKey) as ServerLogFilter;
    dispatch({ type: "reset" });
    queue.current = [];
    generation.current += 1;
    loadingOlderRef.current = false;
    const refills = new AbortController();

    // What was already stored when the page opened.
    fetchHistory(token, { filter: currentFilter, limit: HISTORY_PAGE_SIZE }, refills.signal)
      .then((entries) => dispatch({ type: "history", entries, reachedStart: entries.length < HISTORY_PAGE_SIZE }))
      .catch(() => {
        // The live stream still works; older entries just aren't shown.
      });

    const flush = setInterval(() => {
      if (queue.current.length === 0) return;
      const batch = queue.current;
      queue.current = [];
      dispatch({ type: "receive", entries: batch });
    }, FLUSH_INTERVAL_MS);

    const stream = openLogStream({
      token,
      filter: currentFilter,
      onEntry: (entry) => queue.current.push(entry),
      onGap: (fromMs, toMs) => {
        fetchHistory(token, { filter: currentFilter, fromMs, toMs, limit: GAP_REFILL_LIMIT }, refills.signal)
          .then((entries) => dispatch({ type: "receive", entries }))
          .catch(() => {
            // Refill failed (aborted, server down): the gap stays visible
            // as missing rows; scrolling back through history can still
            // load them.
          });
      },
      onStatus: setStatus,
    });

    return () => {
      clearInterval(flush);
      refills.abort();
      stream.close();
    };
  }, [token, filterKey]);

  // Next page of history, older than the oldest entry shown.
  const loadOlder = useCallback(() => {
    const oldest = entriesRef.current[entriesRef.current.length - 1];
    if (!oldest || loadingOlderRef.current || state.reachedStart) return;
    const filter = JSON.parse(filterKey) as ServerLogFilter;
    const started = generation.current;
    loadingOlderRef.current = true;
    dispatch({ type: "loadingOlder" });
    fetchHistory(token, { filter, before: { ms: oldest.receivedAtMs, id: oldest.id }, limit: HISTORY_PAGE_SIZE })
      .then((entries) => {
        if (started !== generation.current) return; // filter changed meanwhile
        dispatch({ type: "history", entries, reachedStart: entries.length < HISTORY_PAGE_SIZE });
      })
      .catch(() => {
        if (started === generation.current) dispatch({ type: "history", entries: [], reachedStart: false });
      })
      .finally(() => {
        if (started === generation.current) loadingOlderRef.current = false;
      });
  }, [token, filterKey, state.reachedStart]);

  return {
    entries: state.entries,
    loadOlder,
    loadingOlder: state.loadingOlder,
    reachedStart: state.reachedStart,
    pendingCount: state.pending.length,
    paused: state.paused,
    status,
    pause: () => dispatch({ type: "pause" }),
    resume: () => dispatch({ type: "resume" }),
  };
}
