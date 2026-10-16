import { formatAgo } from "../livelog/format";

interface Props {
  online?: boolean | null;
  sinceMs?: number | null;
}

// Online / Offline / Unknown, from the device's MQTT status topic.
export function DeviceStatusBadge({ online, sinceMs }: Props) {
  const state = online == null ? "unknown" : online ? "online" : "offline";
  const label = state === "unknown" ? "Unknown" : state === "online" ? "Online" : "Offline";
  return (
    <span className={`status-badge status-badge-${state}`} title={sinceMs ? new Date(sinceMs).toLocaleString() : undefined}>
      <span className="status-badge-dot" aria-hidden="true" />
      {label}
      {sinceMs != null && state !== "unknown" && <span className="status-badge-since"> · {formatAgo(sinceMs)}</span>}
    </span>
  );
}
