import { deviceLabel, formatTime, ownerLabel, PLATFORM_LABELS } from "../livelog/format";
import { severityOf } from "../livelog/severity";
import type { LogEntry } from "../livelog/types";

interface Props {
  entry: LogEntry;
  expanded: boolean;
  onToggle: (id: string) => void;
}

// One line of the viewer. Clicking it shows the fields that don't fit on
// one line (boot/seq/uptime for devices, thread/trace/exception for the
// server).
export function LogRow({ entry, expanded, onToggle }: Props) {
  const severity = severityOf(entry.level);
  const device = deviceLabel(entry);
  const owner = ownerLabel(entry);

  return (
    <div className={`log-row ${severity.className}`} data-testid="log-row">
      <button
        type="button"
        className="log-row-main"
        aria-expanded={expanded}
        onClick={() => onToggle(entry.id)}
      >
        <span className="log-sev" title={severity.label}>
          <span aria-hidden="true">{severity.icon}</span>
          <span className="visually-hidden">{severity.label}</span>
        </span>
        <time className="log-time" dateTime={new Date(entry.receivedAtMs).toISOString()}>
          {formatTime(entry.receivedAtMs)}
        </time>
        <span className={`log-platform platform-${entry.platform.toLowerCase()}`}>
          {PLATFORM_LABELS[entry.platform] ?? entry.platform}
        </span>
        {entry.kind === "EVENT" && <span className="log-chip-event">EVENT</span>}
        <span className="log-who">
          {device && <span className="log-device">{device}</span>}
          {owner && <span className="log-owner">{owner}</span>}
        </span>
        <span className="log-tag">{entry.tag}</span>
        <span className="log-message">{entry.message}</span>
      </button>
      {expanded && (
        <dl className="log-details">
          {entry.bootId != null && (<><dt>Boot</dt><dd>{entry.bootId}</dd></>)}
          {entry.seq != null && (<><dt>Seq</dt><dd>{entry.seq}</dd></>)}
          {entry.uptimeMs != null && (<><dt>Uptime</dt><dd>{(entry.uptimeMs / 1000).toFixed(1)} s</dd></>)}
          {entry.device && (<><dt>Device</dt><dd>{entry.device.uuid}</dd></>)}
          {entry.owner && (<><dt>Owner</dt><dd>{entry.owner.email}</dd></>)}
          {entry.thread && (<><dt>Thread</dt><dd>{entry.thread}</dd></>)}
          {entry.traceId && (<><dt>Trace</dt><dd>{entry.traceId}</dd></>)}
          {entry.throwable && (<><dt>Exception</dt><dd className="log-throwable">{entry.throwable}</dd></>)}
          <dt>Id</dt><dd>{entry.id}</dd>
        </dl>
      )}
    </div>
  );
}
