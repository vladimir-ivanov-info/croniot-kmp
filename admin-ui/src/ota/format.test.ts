import { describe, expect, it } from "vitest";
import { canCancel, formatBuildTime, formatBytes, formatDuration, isFirmwareFile, stepOf } from "./format";

describe("formatBytes", () => {
  it("picks a readable unit", () => {
    expect(formatBytes(512)).toBe("512 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(2010640)).toBe("1.92 MB");
    expect(formatBytes(null)).toBe("—");
  });
});

describe("formatBuildTime", () => {
  it("keeps the build machine's time as written", () => {
    expect(formatBuildTime("2026-11-07T19:50:09")).toBe("2026-11-07 19:50:09");
    expect(formatBuildTime(undefined)).toBe("—");
  });
});

describe("formatDuration", () => {
  it("measures from request to finish", () => {
    expect(formatDuration("2026-11-07T10:00:00Z", "2026-11-07T10:00:42Z")).toBe("42 s");
    expect(formatDuration("2026-11-07T10:00:00Z", "2026-11-07T10:03:05Z")).toBe("3 min 5 s");
    expect(formatDuration("2026-11-07T10:00:00Z", null)).toBe("—");
  });
});

describe("stepOf", () => {
  it("follows a normal update", () => {
    expect(stepOf({ status: "requested", progress: 0 })).toEqual({ index: 1, failed: false });
    expect(stepOf({ status: "downloading", progress: 40 })).toEqual({ index: 2, failed: false });
    expect(stepOf({ status: "validating", progress: 100 })).toEqual({ index: 4, failed: false });
    expect(stepOf({ status: "confirmed", progress: 100 })).toEqual({ index: 5, failed: false });
  });

  it("marks where a failed update stopped", () => {
    expect(stepOf({ status: "failed", progress: 30 })).toEqual({ index: 2, failed: true });
    expect(stepOf({ status: "failed", progress: 100 })).toEqual({ index: 3, failed: true });
    expect(stepOf({ status: "rolled_back", progress: 100 })).toEqual({ index: 4, failed: true });
    expect(stepOf({ status: "cancelled", progress: 0 })).toEqual({ index: 1, failed: true });
    expect(stepOf({ status: "ignored", progress: 0 })).toEqual({ index: 1, failed: true });
  });
});

describe("canCancel", () => {
  it("only before the device installs", () => {
    expect(canCancel("downloading")).toBe(true);
    expect(canCancel("installed")).toBe(false);
    expect(canCancel("validating")).toBe(false);
  });
});

describe("isFirmwareFile", () => {
  it("wants a .bin", () => {
    expect(isFirmwareFile(new File([""], "croniot-watering-system.bin"))).toBe(true);
    expect(isFirmwareFile(new File([""], "croniot-watering-system.elf"))).toBe(false);
  });
});
