import type { StreamStatus } from "../livelog/logStream";

interface Props {
  status: StreamStatus;
  paused: boolean;
  pendingCount: number;
  onPause: () => void;
  onResume: () => void;
}

const STATUS_LABELS: Record<StreamStatus, string> = {
  connecting: "Connecting…",
  live: "Live",
  reconnecting: "Reconnecting…",
  closed: "Disconnected",
};

export function LiveControls({ status, paused, pendingCount, onPause, onResume }: Props) {
  return (
    <div className="live-controls">
      <span className={`live-status status-${status}`} role="status">
        <span className="live-dot" aria-hidden="true" />
        {paused ? "Paused" : STATUS_LABELS[status]}
      </span>
      {paused ? (
        <button type="button" className="live-button" onClick={onResume}>
          ▶ Resume{pendingCount > 0 ? ` (${pendingCount} new)` : ""}
        </button>
      ) : (
        <button type="button" className="live-button" onClick={onPause}>
          ⏸ Pause
        </button>
      )}
    </div>
  );
}
