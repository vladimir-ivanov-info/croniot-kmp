import { apiFetch, UnauthorizedError } from "../api/client";
import { SseParser } from "./sseParser";
import { filterParams, type DroppedNotice, type LogEntry, type ServerLogFilter } from "./types";

export type StreamStatus = "connecting" | "live" | "reconnecting" | "closed";

export interface LogStreamOptions {
  token: string | null;
  filter: ServerLogFilter;
  onEntry: (entry: LogEntry) => void;
  // Entries received between fromMs and toMs may be missing from what
  // onEntry delivered: the server reported dropping some, or the
  // connection was down. The caller refills that range from history.
  onGap: (fromMs: number, toMs: number) => void;
  onStatus: (status: StreamStatus) => void;
  // Delay before reconnect attempt n (1, 2, ...). Default: 1 s doubling up to 30 s.
  backoffMs?: (attempt: number) => number;
}

const defaultBackoff = (attempt: number) => Math.min(30_000, 1000 * 2 ** (attempt - 1));

// Opens GET /api/admin/logs/stream with fetch (not EventSource, which
// cannot send the Authorization header) and keeps it open: if the
// connection ends or fails it reconnects with backoff, and reports the
// time it was down as a gap. A 401 signs out (api/client) and stops.
export function openLogStream(options: LogStreamOptions): { close: () => void } {
  const { token, filter, onEntry, onGap, onStatus } = options;
  const backoff = options.backoffMs ?? defaultBackoff;
  const controller = new AbortController();
  let closed = false;
  let lastReceivedAtMs: number | null = null;

  const handle = (event: string, data: string) => {
    if (event === "log") {
      const entry = JSON.parse(data) as LogEntry;
      lastReceivedAtMs = entry.receivedAtMs;
      onEntry(entry);
    } else if (event === "dropped") {
      const notice = JSON.parse(data) as DroppedNotice;
      onGap(notice.fromMs, notice.toMs);
    }
  };

  const connectOnce = async (): Promise<void> => {
    const res = await apiFetch(`/api/admin/logs/stream?${filterParams(filter)}`, token, {
      headers: { Accept: "text/event-stream" },
      signal: controller.signal,
    });
    if (!res.ok || !res.body) throw new Error(`stream failed: ${res.status}`);

    // Anything that arrived while we were disconnected is only in history.
    if (lastReceivedAtMs !== null) onGap(lastReceivedAtMs, Date.now());
    onStatus("live");

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    const parser = new SseParser();
    for (;;) {
      const { value, done } = await reader.read();
      if (done) return;
      for (const ev of parser.push(decoder.decode(value, { stream: true }))) handle(ev.event, ev.data);
    }
  };

  const run = async () => {
    let attempt = 0;
    while (!closed) {
      onStatus(attempt === 0 ? "connecting" : "reconnecting");
      try {
        await connectOnce();
        attempt = 0;
      } catch (e) {
        if (e instanceof UnauthorizedError) {
          closed = true;
          onStatus("closed");
          return;
        }
      }
      if (closed) return;
      attempt += 1;
      onStatus("reconnecting");
      await new Promise((resolve) => setTimeout(resolve, backoff(attempt)));
    }
  };

  void run();

  return {
    close() {
      if (closed) return;
      closed = true;
      controller.abort();
      onStatus("closed");
    },
  };
}
