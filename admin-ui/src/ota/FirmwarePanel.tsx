import { useRef } from "react";
import type { OtaUpdate } from "./api";
import {
  STATUS_LABEL,
  STEPS,
  canCancel,
  formatBuildTime,
  formatBytes,
  formatDateTime,
  formatDuration,
  statusTone,
  stepOf,
} from "./format";
import type { FirmwareUpdate } from "./useFirmwareUpdate";
import "./FirmwarePanel.css";

// A finished update stays on screen this long, so the person who dropped
// the file sees how it ended without opening the history.
const RECENT_MS = 10 * 60 * 1000;

export function FirmwarePanel({ fw }: { fw: FirmwareUpdate }) {
  const input = useRef<HTMLInputElement>(null);
  const { overview, upload } = fw;
  const installed = overview?.installed;
  const history = overview?.history ?? [];
  const latest = history[0];
  const shown =
    overview?.current ??
    (latest?.finishedAt && fw.fetchedAtMs - new Date(latest.finishedAt).getTime() < RECENT_MS ? latest : null);

  return (
    <section className="fw-panel" aria-label="Firmware">
      <div className="fw-head">
        <h2>Firmware</h2>
        <button type="button" className="fw-pick" onClick={() => input.current?.click()} disabled={fw.busy}>
          Choose .bin…
        </button>
        <input
          ref={input}
          type="file"
          accept=".bin"
          hidden
          data-testid="firmware-input"
          onChange={(e) => {
            const file = e.target.files?.[0];
            e.target.value = "";
            if (file) void fw.start(file);
          }}
        />
        <span className="fw-hint">or drop the .bin anywhere on this page to update the device</span>
      </div>

      {installed ? (
        <dl className="entity-facts fw-facts">
          <div><dt>Version</dt><dd className="mono">{installed.version}</dd></div>
          <div><dt>Project</dt><dd>{installed.project}</dd></div>
          <div><dt>Built</dt><dd>{formatBuildTime(installed.builtAt)}</dd></div>
          <div><dt>Size</dt><dd>{formatBytes(installed.sizeBytes)}</dd></div>
          <div><dt>ESP-IDF</dt><dd>{installed.idfVersion ?? "—"}</dd></div>
          <div><dt>Partition</dt><dd>{installed.slot ?? "—"}</dd></div>
          <div>
            <dt>Uploaded</dt>
            <dd>
              {installed.release ? formatDateTime(installed.release.createdAt) : <span className="muted">not through this panel</span>}
            </dd>
          </div>
          <div><dt>Reported</dt><dd>{formatDateTime(installed.reportedAt)}</dd></div>
        </dl>
      ) : (
        <p className="muted fw-unknown">
          {overview ? "The device hasn't reported its firmware yet: it does on every boot, with firmware that has OTA support." : "Loading…"}
        </p>
      )}

      {fw.error && (
        <div className="fw-error" role="alert">
          {fw.error}
          <button type="button" className="fw-link" onClick={fw.dismissError}>Dismiss</button>
        </div>
      )}

      {upload && (
        <div className="fw-update">
          <Steps index={0} failed={false} />
          <ProgressBar fraction={upload.fraction} label={`Uploading ${upload.fileName}`} />
        </div>
      )}

      {!upload && shown && <UpdateCard update={shown} onCancel={fw.cancel} />}

      {history.length > 0 && <History updates={history} />}
    </section>
  );
}

function UpdateCard({ update, onCancel }: { update: OtaUpdate; onCancel: () => void }) {
  const step = stepOf(update);
  const tone = statusTone(update.status);
  return (
    <div className={`fw-update fw-tone-${tone}`} data-testid="firmware-update">
      <div className="fw-update-title">
        <strong>
          {update.fromVersion ? <span className="mono">{update.fromVersion}</span> : "?"} →{" "}
          <span className="mono">{update.version}</span>
        </strong>
        <span className={`fw-status fw-tone-${tone}`}>{STATUS_LABEL[update.status]}</span>
        {!update.finished && canCancel(update.status) && (
          <button type="button" className="fw-link" onClick={onCancel}>Cancel</button>
        )}
      </div>
      <Steps index={step.index} failed={step.failed} done={update.status === "confirmed"} />
      {(update.status === "downloading" || update.status === "retrying") && (
        <ProgressBar fraction={update.progress / 100} label="Device downloading" />
      )}
      {update.detail && <p className="fw-detail">{update.detail}</p>}
      {update.status === "requested" && (
        <p className="fw-detail muted">The device starts as soon as it is online.</p>
      )}
      {update.status === "validating" && (
        <p className="fw-detail muted">The device keeps it after a minute online, or goes back to the previous firmware.</p>
      )}
    </div>
  );
}

function Steps({ index, failed, done = false }: { index: number; failed: boolean; done?: boolean }) {
  return (
    <ol className="fw-steps">
      {STEPS.map((label, i) => {
        const state =
          i < index || (done && i === index) ? "done" : i === index ? (failed ? "failed" : "current") : "todo";
        return (
          <li key={label} className={`fw-step fw-step-${state}`} aria-current={i === index ? "step" : undefined}>
            {label}
          </li>
        );
      })}
    </ol>
  );
}

function ProgressBar({ fraction, label }: { fraction: number; label: string }) {
  const percent = Math.round(Math.min(Math.max(fraction, 0), 1) * 100);
  return (
    <div className="fw-progress">
      <div className="fw-progress-label">
        <span>{label}</span>
        <span>{percent} %</span>
      </div>
      <div className="fw-progress-track" role="progressbar" aria-label={label} aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
        <div className="fw-progress-fill" style={{ width: `${percent}%` }} />
      </div>
    </div>
  );
}

function History({ updates }: { updates: OtaUpdate[] }) {
  return (
    <details className="fw-history">
      <summary>Update history ({updates.length})</summary>
      <table>
        <thead>
          <tr>
            <th>Requested</th>
            <th>From</th>
            <th>To</th>
            <th>Result</th>
            <th>Took</th>
            <th>Detail</th>
          </tr>
        </thead>
        <tbody>
          {updates.map((u) => (
            <tr key={u.requestId}>
              <td>{formatDateTime(u.requestedAt)}</td>
              <td className="mono">{u.fromVersion ?? "—"}</td>
              <td className="mono">{u.version}</td>
              <td><span className={`fw-status fw-tone-${statusTone(u.status)}`}>{STATUS_LABEL[u.status]}</span></td>
              <td>{formatDuration(u.requestedAt, u.finishedAt)}</td>
              <td className="fw-history-detail">{u.detail ?? ""}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
