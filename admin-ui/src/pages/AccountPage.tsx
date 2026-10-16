import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { useAuth } from "../auth/useAuth";
import { DeviceStatusBadge } from "../components/DeviceStatusBadge";
import { FilteredLogViewer } from "../components/FilteredLogViewer";
import { fetchAccount, type AccountDetail } from "../livelog/directory";
import { formatAgo } from "../livelog/format";
import "./EntityPage.css";

// Route wrapper: a new account id means a fresh page (no stale state).
export function AccountPageRoute() {
  const { uuid = "" } = useParams();
  return <AccountPage key={uuid} uuid={uuid} />;
}

// One owner: who they are, their devices, and the logs of all of them.
export function AccountPage({ uuid }: { uuid: string }) {
  const { token } = useAuth();
  const [detail, setDetail] = useState<AccountDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const abort = new AbortController();
    fetchAccount(token, uuid, abort.signal)
      .then(setDetail)
      .catch((e: unknown) => {
        if (!abort.signal.aborted) setError(e instanceof Error && e.message.includes("404") ? "Account not found." : "Could not load the account.");
      });
    return () => abort.abort();
  }, [token, uuid]);

  const owner = detail?.owner;

  return (
    <div className="entity-page">
      <header className="entity-header">
        <div className="entity-heading">
          <h1>{owner ? owner.nickname || owner.email : uuid}</h1>
          {owner && <span className="muted">{owner.email}</span>}
        </div>
        {error && <p className="entity-error">{error}</p>}
        {detail && (
          detail.devices.length === 0 ? (
            <p className="muted">No devices.</p>
          ) : (
            <table className="entity-devices">
              <thead>
                <tr>
                  <th scope="col">Device</th>
                  <th scope="col">Status</th>
                  <th scope="col">Errors (24 h)</th>
                  <th scope="col">Last activity</th>
                </tr>
              </thead>
              <tbody>
                {detail.devices.map((d) => (
                  <tr key={d.uuid}>
                    <td>
                      <Link to={`/devices/${encodeURIComponent(d.uuid)}`}>{d.name || d.uuid}</Link>
                      {d.name && <span className="muted mono"> {d.uuid}</span>}
                    </td>
                    <td><DeviceStatusBadge online={d.online} sinceMs={d.statusChangedAtMs} /></td>
                    <td className={d.errors24h > 0 ? "has-errors" : undefined}>{d.errors24h}</td>
                    <td>{d.lastActivityMs ? formatAgo(d.lastActivityMs) : "never"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        )}
      </header>
      <div className="entity-logs">
        <FilteredLogViewer title={<h2 className="entity-logs-title">Logs of all their devices</h2>} fixedOwner={uuid} />
      </div>
    </div>
  );
}
