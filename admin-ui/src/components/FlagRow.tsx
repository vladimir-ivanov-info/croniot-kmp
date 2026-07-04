import type { FeatureFlag } from "../types";

interface Props {
  flag: FeatureFlag;
  onToggle: (name: string, enabled: boolean) => void;
  disabled: boolean;
}

export function FlagRow({ flag, onToggle, disabled }: Props) {
  return (
    <div style={styles.row}>
      <div style={styles.info}>
        <span style={styles.name}>{flag.name}</span>
        {flag.description && <span style={styles.description}>{flag.description}</span>}
      </div>
      <label style={styles.toggle}>
        <input
          type="checkbox"
          checked={flag.enabled}
          disabled={disabled}
          onChange={(e) => onToggle(flag.name, e.target.checked)}
        />
        <span style={{ marginLeft: 8, color: flag.enabled ? "#22c55e" : "#6b7280" }}>
          {flag.enabled ? "Enabled" : "Disabled"}
        </span>
      </label>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  row: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "16px 20px",
    borderBottom: "1px solid #e5e7eb",
    gap: 16,
  },
  info: {
    display: "flex",
    flexDirection: "column",
    gap: 4,
  },
  name: {
    fontFamily: "monospace",
    fontSize: 15,
    fontWeight: 600,
    color: "#111827",
  },
  description: {
    fontSize: 13,
    color: "#6b7280",
  },
  toggle: {
    display: "flex",
    alignItems: "center",
    cursor: "pointer",
    whiteSpace: "nowrap",
    flexShrink: 0,
  },
};
