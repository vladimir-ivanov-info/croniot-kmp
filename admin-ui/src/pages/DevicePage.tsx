import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { useAuth } from "../auth/useAuth";
import { DeviceStatusBadge } from "../components/DeviceStatusBadge";
import { FilteredLogViewer } from "../components/FilteredLogViewer";
import { fetchDevice, type DeviceDetail } from "../livelog/directory";
import { duplicateRatio, formatAgo } from "../livelog/format";
import { severityOf } from "../livelog/severity";
import { LEVELS } from "../livelog/types";
import "./EntityPage.css";

// Route wrapper: a new device id means a fresh page (no stale state).
export function DevicePageRoute() {
  const { uuid = "" } = useParams();
  return <DevicePage key={uuid} uuid={uuid} />;
}

export function DevicePage({ uuid }: { uuid: string }) {
  const { token } = useAuth();
  const [detail, setDetail] = useState<DeviceDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const abort = new AbortController();
    fetchDevice(token, uuid, abort.signal)
      .then(setDetail)
      .catch((e: unknown) => {
        if (!abort.signal.aborted) setError(e instanceof Error && e.message.includes("404") ? "Device not found." : "Could not load the device.");
      });
    return () => abort.abort();
  }, [token, uuid]);

  const s = detail?.summary;
  const ratio = detail ? duplicateRatio(detail.recordsReceived24h, detail.recordsDuplicated24h) : null;

  return (
    <div className="entity-page">
      <header className="entity-header">
        <div className="entity-heading">
          <h1>{s?.name || uuid}</h1>
          {s && <DeviceStatusBadge online={s.online} sinceMs={s.statusChangedAtMs} />}
        </div>
        {error && <p className="entity-error">{error}</p>}
        {detail && s && (
          <dl className="entity-facts">
            <div><dt>Device id</dt><dd className="mono">{s.uuid}</dd></div>
            <div>
              <dt>Owner</dt>
              <dd><Link to={`/accounts/${encodeURIComponent(s.owner.uuid)}`}>{s.owner.nickname || s.owner.email}</Link> <span className="muted">{s.owner.email}</span></dd>
            </div>
            <div><dt>Last activity</dt><dd>{s.lastActivityMs ? formatAgo(s.lastActivityMs) : "never"}</dd></div>
            <div><dt>Boot</dt><dd>{s.bootId ?? "—"}</dd></div>
            <div>
              <dt>Last 24 h</dt>
              <dd className="level-counts">
                {LEVELS.map((l) => {
                  const count = detail.levelCounts24h[String(l)] ?? 0;
                  const sev = severityOf(l);
                  return (
                    <span key={l} className={`level-count ${sev.className}${count === 0 ? " is-zero" : ""}`}>
                      {sev.label} {count}
                    </span>
                  );
                })}
              </dd>
            </div>
            <div>
              <dt>Duplicates (24 h)</dt>
              <dd>{ratio ? `${ratio} (${detail.recordsDuplicated24h} of ${detail.recordsReceived24h} records)` : "—"}</dd>
            </div>
            {detail.description && <div><dt>Description</dt><dd>{detail.description}</dd></div>}
          </dl>
        )}
      </header>
      <div className="entity-logs">
        <FilteredLogViewer title={<h2 className="entity-logs-title">Logs</h2>} fixedDevice={uuid} />
      </div>
    </div>
  );
}
