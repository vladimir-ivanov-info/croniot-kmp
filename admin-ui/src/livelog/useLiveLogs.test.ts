import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { LogEntry } from "./types";
import { NO_FILTER } from "./types";
import { HISTORY_PAGE_SIZE, useLiveLogs } from "./useLiveLogs";

const entry = (n: number): LogEntry => ({
  id: `log-${String(n).padStart(4, "0")}`, platform: "IOT", kind: "LOG", level: 2, tag: "T", message: `m${n}`, receivedAtMs: n,
});

// History: a full first page (newest first), then a short older page.
// Stream: ends immediately (the hook just reconnects in the background).
function mockServer() {
  const firstPage = Array.from({ length: HISTORY_PAGE_SIZE }, (_, i) => entry(1000 - i));
  const olderPage = [entry(10), entry(9)];
  const fetchMock = vi.fn(async (url: string) => {
    if (url.startsWith("/api/admin/logs/stream")) return new Response("", { status: 200 });
    const body = url.includes("before=") ? olderPage : firstPage;
    return new Response(JSON.stringify(body), { status: 200 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe("useLiveLogs history", () => {
  it("loads the newest stored page on open, then older pages from the last entry", async () => {
    const fetchMock = mockServer();
    const { result, unmount } = renderHook(() => useLiveLogs("tok", NO_FILTER));

    await waitFor(() => expect(result.current.entries).toHaveLength(HISTORY_PAGE_SIZE));
    expect(result.current.reachedStart).toBe(false);

    act(() => result.current.loadOlder());
    await waitFor(() => expect(result.current.entries).toHaveLength(HISTORY_PAGE_SIZE + 2));

    const olderCall = fetchMock.mock.calls.map((c) => c[0] as string).find((u) => u.includes("before="));
    const oldestOfFirstPage = entry(1000 - HISTORY_PAGE_SIZE + 1);
    expect(olderCall).toContain(`before=${oldestOfFirstPage.receivedAtMs}`);
    expect(olderCall).toContain(`beforeId=${oldestOfFirstPage.id}`);
    expect(result.current.reachedStart).toBe(true); // short page = nothing older
    expect(result.current.entries.at(-1)?.id).toBe("log-0009");
    unmount();
  });
});
