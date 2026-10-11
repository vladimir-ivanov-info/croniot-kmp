import { useEffect, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { getServerLogLevel, revertServerLogLevel, SERVER_LEVELS, setServerLogLevel, type ServerLogLevel } from "../livelog/serverLogLevel";

const TTL_MINUTES = 30;

const timeOf = (ms: number) => new Date(ms).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

// The server's own log level, changeable without a restart. A change lasts
// TTL_MINUTES and then the server puts the previous level back by itself.
export function ServerLogLevelControl() {
  const { token } = useAuth();
  const [state, setState] = useState<ServerLogLevel | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const abort = new AbortController();
    getServerLogLevel(token, abort.signal).then(setState).catch(() => {});
    return () => abort.abort();
  }, [token]);

  if (!state) return null;

  const run = (action: Promise<ServerLogLevel>) => {
    setBusy(true);
    action.then(setState).catch(() => {}).finally(() => setBusy(false));
  };

  return (
    <div className="server-level">
      <label>
        <span className="chip-group-label">Server level</span>
        <select value={state.root} disabled={busy} onChange={(e) => run(setServerLogLevel(token, e.target.value, TTL_MINUTES))}>
          {SERVER_LEVELS.map((l) => (
            <option key={l} value={l}>{l}</option>
          ))}
        </select>
      </label>
      {state.revertsAtMs != null && (
        <>
          <span className="server-level-until">until {timeOf(state.revertsAtMs)}</span>
          <button type="button" className="filter-clear" disabled={busy} onClick={() => run(revertServerLogLevel(token))}>
            Reset
          </button>
        </>
      )}
    </div>
  );
}
