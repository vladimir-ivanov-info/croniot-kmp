import { useEffect, useReducer, useRef, useState } from "react";
import { fetchHistory } from "./history";
import { initialLiveLogsState, liveLogsReducer } from "./liveLogsState";
import { openLogStream, type StreamStatus } from "./logStream";
import type { LogEntry, ServerLogFilter } from "./types";

// Live entries are applied in small batches rather than one render per
// line: a device in DEBUG can send hundreds per second.
const FLUSH_INTERVAL_MS = 200;
const GAP_REFILL_LIMIT = 1000;

export function useLiveLogs(token: string | null, filter: ServerLogFilter) {
  const [state, dispatch] = useReducer(liveLogsReducer, initialLiveLogsState);
  const [status, setStatus] = useState<StreamStatus>("connecting");
  const queue = useRef<LogEntry[]>([]);

  // Reopen everything when the server-side filter changes.
  const filterKey = JSON.stringify(filter);

  useEffect(() => {
    const currentFilter = JSON.parse(filterKey) as ServerLogFilter;
    dispatch({ type: "reset" });
    queue.current = [];
    const refills = new AbortController();

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

  return {
    entries: state.entries,
    pendingCount: state.pending.length,
    paused: state.paused,
    status,
    pause: () => dispatch({ type: "pause" }),
    resume: () => dispatch({ type: "resume" }),
    // For loading older history into the same list (dedupe included).
    addHistory: (entries: LogEntry[]) => dispatch({ type: "history", entries }),
  };
}
