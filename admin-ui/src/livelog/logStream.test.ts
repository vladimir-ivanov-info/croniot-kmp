import { afterEach, describe, expect, it, vi } from "vitest";
import { openLogStream, type StreamStatus } from "./logStream";
import { NO_FILTER, type LogEntry } from "./types";

const entry = (id: string, ms: number): LogEntry => ({
  id, platform: "SERVER", kind: "LOG", level: 1, tag: "T", message: "m", receivedAtMs: ms,
});

// A response whose body yields the given chunks, then ends.
function sseResponse(chunks: string[]): Response {
  const encoder = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((c) => controller.enqueue(encoder.encode(c)));
      controller.close();
    },
  });
  return new Response(body, { status: 200, headers: { "Content-Type": "text/event-stream" } });
}

const logEvent = (e: LogEntry) => `event: log\nid: ${e.id}\ndata: ${JSON.stringify(e)}\n\n`;

afterEach(() => vi.unstubAllGlobals());

describe("openLogStream", () => {
  it("delivers entries, reports dropped ranges and sends the filter and token", async () => {
    const fetchMock = vi.fn(async () =>
      sseResponse([logEvent(entry("srv-1", 100)).slice(0, 20), logEvent(entry("srv-1", 100)).slice(20),
        'event: dropped\ndata: {"count":3,"fromMs":100,"toMs":900}\n\n', logEvent(entry("srv-2", 900))]),
    );
    vi.stubGlobal("fetch", fetchMock);
    const entries: string[] = [];
    const gaps: [number, number][] = [];

    const stream = openLogStream({
      token: "tok",
      filter: { ...NO_FILTER, platforms: ["SERVER"], levels: [0, 1] },
      onEntry: (e) => entries.push(e.id),
      onGap: (from, to) => gaps.push([from, to]),
      onStatus: () => {},
      backoffMs: () => 60_000,
    });
    await vi.waitFor(() => expect(entries).toEqual(["srv-1", "srv-2"]));
    stream.close();

    expect(gaps).toEqual([[100, 900]]);
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("/api/admin/logs/stream?platform=SERVER&level=0&level=1");
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer tok");
  });

  it("reconnects after the stream ends and reports the time it was down as a gap", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(sseResponse([logEvent(entry("srv-1", 100))]))
      .mockResolvedValue(sseResponse([]));
    vi.stubGlobal("fetch", fetchMock);
    const gaps: [number, number][] = [];
    const statuses: StreamStatus[] = [];

    const stream = openLogStream({
      token: "tok",
      filter: NO_FILTER,
      onEntry: () => {},
      onGap: (from, to) => gaps.push([from, to]),
      onStatus: (s) => statuses.push(s),
      backoffMs: () => 1,
    });
    await vi.waitFor(() => expect(gaps.length).toBeGreaterThan(0));
    stream.close();

    expect(gaps[0][0]).toBe(100);
    expect(statuses).toContain("reconnecting");
    expect(statuses.at(-1)).toBe("closed");
  });

  it("stops for good on 401", async () => {
    const fetchMock = vi.fn(async () => new Response("", { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);
    const statuses: StreamStatus[] = [];

    openLogStream({ token: "expired", filter: NO_FILTER, onEntry: () => {}, onGap: () => {}, onStatus: (s) => statuses.push(s), backoffMs: () => 1 });
    await vi.waitFor(() => expect(statuses.at(-1)).toBe("closed"));
    await new Promise((r) => setTimeout(r, 20));

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
