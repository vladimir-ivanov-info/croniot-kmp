import { describe, expect, it } from "vitest";
import { formatTime } from "./format";
import { isSerious, severityOf } from "./severity";

describe("severityOf", () => {
  it("gives every level its own label, icon and class", () => {
    expect(severityOf(0)).toEqual({ label: "Error", icon: "⛔", className: "sev-error" });
    expect(severityOf(1)).toMatchObject({ label: "Warn", className: "sev-warn" });
    expect(severityOf(2).className).toBe("sev-info");
    expect(severityOf(4).className).toBe("sev-trace");
  });

  it("clamps out-of-range levels instead of failing", () => {
    expect(severityOf(-1).label).toBe("Error");
    expect(severityOf(9).label).toBe("Trace");
  });

  it("treats only Error and Warn as serious", () => {
    expect([0, 1, 2, 3, 4].map(isSerious)).toEqual([true, true, false, false, false]);
  });
});

describe("formatTime", () => {
  it("shows only the time for today and adds the date otherwise", () => {
    const now = new Date(2026, 9, 12, 10, 0, 0);
    const today = new Date(2026, 9, 12, 9, 5, 7, 42).getTime();
    const before = new Date(2026, 9, 11, 23, 59, 59, 1).getTime();

    expect(formatTime(today, now)).toBe("09:05:07.042");
    expect(formatTime(before, now)).toBe("2026-10-11 23:59:59.001");
  });
});
