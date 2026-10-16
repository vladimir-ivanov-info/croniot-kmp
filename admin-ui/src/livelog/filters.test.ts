import { describe, expect, it } from "vitest";
import { ownerOptions } from "./directory";
import { EMPTY_FILTER, filterFromSearchParams, filterToSearchParams, isSelected, matchesClient, toggleValue } from "./filters";
import { PLATFORMS, type LogEntry } from "./types";

const entry: LogEntry = {
  id: "log-1", platform: "IOT", kind: "LOG", level: 1, tag: "Uplink", message: "Publish FAILED",
  receivedAtMs: 1, device: { uuid: "dev-1", name: "Riego" }, owner: { uuid: "acc-1", nickname: "vladimir", email: "v@x.com" },
};

describe("toggleValue / isSelected", () => {
  it("treats nothing chosen as everything selected", () => {
    expect(isSelected([], "IOT")).toBe(true);
  });

  it("deselecting one value from 'all' keeps the others", () => {
    expect(toggleValue([], "IOT", PLATFORMS)).toEqual(["SERVER", "ANDROID", "IOS"]);
  });

  it("selecting the last missing value goes back to 'all' (empty)", () => {
    expect(toggleValue(["SERVER", "ANDROID", "IOS"], "IOT", PLATFORMS)).toEqual([]);
  });

  it("allows several values at once", () => {
    const next = toggleValue(toggleValue([], "ANDROID", PLATFORMS), "IOS", PLATFORMS);
    expect(next).toEqual(["SERVER", "IOT"]);
  });
});

describe("matchesClient", () => {
  it("matches everything with an empty filter", () => {
    expect(matchesClient(entry, EMPTY_FILTER)).toBe(true);
  });

  it("searches text case-insensitively in message, tag, device and owner", () => {
    expect(matchesClient(entry, { ...EMPTY_FILTER, text: "publish failed" })).toBe(true);
    expect(matchesClient(entry, { ...EMPTY_FILTER, text: "riego" })).toBe(true);
    expect(matchesClient(entry, { ...EMPTY_FILTER, text: "vladimir" })).toBe(true);
    expect(matchesClient(entry, { ...EMPTY_FILTER, text: "nope" })).toBe(false);
  });

  it("filters by any of several tags", () => {
    expect(matchesClient(entry, { ...EMPTY_FILTER, tags: ["Sensor", "Uplink"] })).toBe(true);
    expect(matchesClient(entry, { ...EMPTY_FILTER, tags: ["Sensor"] })).toBe(false);
  });
});

describe("URL round trip", () => {
  it("survives a round trip and drops unknown values", () => {
    const filter = { ...EMPTY_FILTER, platforms: ["IOT" as const], levels: [0, 1], devices: ["dev-1"], tags: ["Uplink"], text: "fail" };

    expect(filterFromSearchParams(filterToSearchParams(filter))).toEqual(filter);
    expect(filterFromSearchParams(new URLSearchParams("platform=TOASTER&level=9&level=0"))).toEqual({ ...EMPTY_FILTER, levels: [0] });
  });

  it("keeps an empty filter out of the URL", () => {
    expect(filterToSearchParams(EMPTY_FILTER).toString()).toBe("");
  });
});

describe("ownerOptions", () => {
  it("lists each owner once", () => {
    const owner = { uuid: "acc-1", nickname: "v", email: "v@x.com" };
    const options = ownerOptions([
      { uuid: "d1", owner, errors24h: 0 },
      { uuid: "d2", owner, errors24h: 0 },
    ]);
    expect(options).toEqual([{ value: "acc-1", label: "v <v@x.com>" }]);
  });
});
