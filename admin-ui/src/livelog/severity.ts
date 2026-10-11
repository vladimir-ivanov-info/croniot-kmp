import { LEVEL, LEVEL_NAMES } from "./types";

// How each level looks. Never colour alone: every level also has its own
// icon and a text label (for screen readers and colour-blind users).
export interface Severity {
  label: string;
  icon: string;
  className: string;
}

const SEVERITIES: Severity[] = [
  { label: LEVEL_NAMES[LEVEL.ERROR], icon: "⛔", className: "sev-error" },
  { label: LEVEL_NAMES[LEVEL.WARN], icon: "⚠", className: "sev-warn" },
  { label: LEVEL_NAMES[LEVEL.INFO], icon: "•", className: "sev-info" },
  { label: LEVEL_NAMES[LEVEL.DEBUG], icon: "·", className: "sev-debug" },
  { label: LEVEL_NAMES[LEVEL.TRACE], icon: "·", className: "sev-trace" },
];

export function severityOf(level: number): Severity {
  return SEVERITIES[Math.min(Math.max(level, 0), SEVERITIES.length - 1)];
}

// "Serious" = what the "Solo graves" shortcut keeps: Error and Warn.
export function isSerious(level: number): boolean {
  return level <= LEVEL.WARN;
}
