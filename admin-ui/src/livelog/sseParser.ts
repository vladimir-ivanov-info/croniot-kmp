// Incremental Server-Sent Events parser (the subset of the spec the
// server uses: event, data, id, and ":" comment lines for heartbeats).
// Network chunks can split anywhere - mid-line, mid-event, even between
// "\r" and "\n" - so it keeps whatever is unfinished for the next push().

export interface SseEvent {
  event: string;
  data: string;
  id?: string;
}

export class SseParser {
  private buffer = "";
  private event = "";
  private data: string[] = [];
  private id: string | undefined;

  push(chunk: string): SseEvent[] {
    this.buffer += chunk;
    const events: SseEvent[] = [];

    for (;;) {
      const match = /\r\n|\r|\n/.exec(this.buffer);
      if (!match) break;
      // A lone "\r" at the very end may be the first half of "\r\n".
      if (match[0] === "\r" && match.index === this.buffer.length - 1) break;

      const line = this.buffer.slice(0, match.index);
      this.buffer = this.buffer.slice(match.index + match[0].length);

      if (line === "") {
        if (this.data.length > 0) {
          events.push({ event: this.event || "message", data: this.data.join("\n"), id: this.id });
        }
        this.event = "";
        this.data = [];
        continue;
      }
      if (line.startsWith(":")) continue;

      const colon = line.indexOf(":");
      const field = colon === -1 ? line : line.slice(0, colon);
      let value = colon === -1 ? "" : line.slice(colon + 1);
      if (value.startsWith(" ")) value = value.slice(1);

      if (field === "event") this.event = value;
      else if (field === "data") this.data.push(value);
      else if (field === "id") this.id = value;
    }
    return events;
  }
}
