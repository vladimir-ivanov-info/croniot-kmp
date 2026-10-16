import { describe, expect, it } from "vitest";
import { initialLiveLogsState, liveLogsReducer, mergeEntries } from "./liveLogsState";
import type { LogEntry } from "./types";

const entry = (id: string, ms: number): LogEntry => ({
  id, platform: "IOT", kind: "LOG", level: 2, tag: "T", message: id, receivedAtMs: ms,
});

describe("mergeEntries", () => {
  it("keeps newest first and breaks ties by id descending", () => {
    const merged = mergeEntries([entry("log-1", 10)], [entry("log-3", 30), entry("log-2", 10)]);

    expect(merged.map((e) => e.id)).toEqual(["log-3", "log-2", "log-1"]);
  });

  it("keeps an entry seen live and again in history only once", () => {
    const merged = mergeEntries([entry("log-1", 10)], [entry("log-1", 10), entry("log-1", 10)]);

    expect(merged).toHaveLength(1);
  });

  it("drops the oldest beyond the cap", () => {
    const merged = mergeEntries([entry("a", 1), entry("b", 2)], [entry("c", 3)], 2);

    expect(merged.map((e) => e.id)).toEqual(["c", "b"]);
  });
});

describe("liveLogsReducer", () => {
  it("shows live entries right away when not paused", () => {
    const state = liveLogsReducer(initialLiveLogsState, { type: "receive", entries: [entry("a", 1)] });

    expect(state.entries.map((e) => e.id)).toEqual(["a"]);
    expect(state.pending).toEqual([]);
  });

  it("holds live entries while paused and shows them all on resume", () => {
    let state = liveLogsReducer(initialLiveLogsState, { type: "receive", entries: [entry("a", 1)] });
    state = liveLogsReducer(state, { type: "pause" });
    state = liveLogsReducer(state, { type: "receive", entries: [entry("b", 2), entry("a", 1)] });

    expect(state.entries.map((e) => e.id)).toEqual(["a"]);
    expect(state.pending.map((e) => e.id)).toEqual(["b"]); // "a" is already shown

    state = liveLogsReducer(state, { type: "resume" });
    expect(state.entries.map((e) => e.id)).toEqual(["b", "a"]);
    expect(state.pending).toEqual([]);
    expect(state.paused).toBe(false);
  });

  it("adds older history even while paused", () => {
    let state = liveLogsReducer(initialLiveLogsState, { type: "receive", entries: [entry("new", 10)] });
    state = liveLogsReducer(state, { type: "pause" });
    state = liveLogsReducer(state, { type: "history", entries: [entry("old", 1)], reachedStart: false });

    expect(state.entries.map((e) => e.id)).toEqual(["new", "old"]);
  });

  it("starts empty again on reset", () => {
    const state = liveLogsReducer(
      { entries: [entry("a", 1)], pending: [entry("b", 2)], paused: true, loadingOlder: true, reachedStart: true },
      { type: "reset" }
    );

    expect(state).toEqual(initialLiveLogsState);
  });
});
