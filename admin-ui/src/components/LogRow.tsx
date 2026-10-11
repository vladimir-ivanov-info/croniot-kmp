import { Link } from "react-router";
import { deviceLabel, formatTime, ownerLabel, PLATFORM_LABELS } from "../livelog/format";
import { severityOf } from "../livelog/severity";
import type { LogEntry } from "../livelog/types";

interface Props {
  entry: LogEntry;
  expanded: boolean;
  onToggle: (id: string) => void;
  // Text search term to mark inside the message.
  highlight?: string;
}

function marked(text: string, term: string | undefined): React.ReactNode {
  const needle = term?.trim().toLowerCase();
  if (!needle) return text;
  const parts: React.ReactNode[] = [];
  const lower = text.toLowerCase();
  let from = 0;
  for (let at = lower.indexOf(needle); at !== -1; at = lower.indexOf(needle, from)) {
    if (at > from) parts.push(text.slice(from, at));
    parts.push(<mark key={at}>{text.slice(at, at + needle.length)}</mark>);
    from = at + needle.length;
  }
  if (from < text.length) parts.push(text.slice(from));
  return parts;
}

// One line of the viewer. Clicking it (or its ▸ button) shows the fields
// that don't fit on one line (boot/seq/uptime for devices,
// thread/trace/exception for the server). Device and owner are links to
// their pages - kept outside the expand button, since a link inside a
// button is invalid HTML.
export function LogRow({ entry, expanded, onToggle, highlight }: Props) {
  const severity = severityOf(entry.level);
  const device = deviceLabel(entry);
  const owner = ownerLabel(entry);

  return (
    <div className={`log-row ${severity.className}`} data-testid="log-row">
      <div className="log-row-main" onClick={() => onToggle(entry.id)}>
        <button
          type="button"
          className="log-expand"
          aria-expanded={expanded}
          aria-label={expanded ? "Hide details" : "Show details"}
          onClick={(e) => {
            e.stopPropagation();
            onToggle(entry.id);
          }}
        >
          {expanded ? "▾" : "▸"}
        </button>
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
        <span className="log-who" onClick={(e) => e.stopPropagation()}>
          {device && entry.device && (
            <Link className="log-device" to={`/devices/${encodeURIComponent(entry.device.uuid)}`}>{device}</Link>
          )}
          {owner && entry.owner && (
            <Link className="log-owner" to={`/accounts/${encodeURIComponent(entry.owner.uuid)}`}>{owner}</Link>
          )}
        </span>
        <span className="log-tag">{entry.tag}</span>
        <span className="log-message">{marked(entry.message, highlight)}</span>
      </div>
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
