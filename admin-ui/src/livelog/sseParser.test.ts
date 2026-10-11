import { describe, expect, it } from "vitest";
import { SseParser } from "./sseParser";

describe("SseParser", () => {
  it("parses complete events with event, data and id", () => {
    const parser = new SseParser();

    const events = parser.push('event: log\nid: log-1\ndata: {"a":1}\n\n');

    expect(events).toEqual([{ event: "log", id: "log-1", data: '{"a":1}' }]);
  });

  it("keeps partial input until the event is complete, whatever the split", () => {
    const text = 'event: log\r\nid: log-2\r\ndata: {"b":2}\r\n\r\nevent: dropped\r\ndata: {"count":3}\r\n\r\n';
    for (let size = 1; size <= text.length; size++) {
      const parser = new SseParser();
      const events = [];
      for (let i = 0; i < text.length; i += size) events.push(...parser.push(text.slice(i, i + size)));

      expect(events.map((e) => e.event)).toEqual(["log", "dropped"]);
      expect(events[1].data).toBe('{"count":3}');
    }
  });

  it("ignores heartbeat comments and blank events", () => {
    const parser = new SseParser();

    expect(parser.push(": heartbeat\n\n\n")).toEqual([]);
  });

  it("joins multi-line data and defaults the event name", () => {
    const parser = new SseParser();

    expect(parser.push("data: one\ndata: two\n\n")).toEqual([{ event: "message", data: "one\ntwo", id: undefined }]);
  });
});
