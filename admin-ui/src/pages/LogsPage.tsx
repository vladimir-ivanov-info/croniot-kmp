import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { FilterBar } from "../components/FilterBar";
import { LogViewer } from "../components/LogViewer";
import { deviceOptions, fetchDevices, ownerOptions, type DeviceSummary } from "../livelog/directory";
import { matchesClient, serverPart } from "../livelog/filters";
import { useLiveLogs } from "../livelog/useLiveLogs";
import { useUrlFilter } from "../livelog/useUrlFilter";
import "./LogsPage.css";

// Every platform's logs and events, live, filtered by the URL query.
export function LogsPage() {
  const { token } = useAuth();
  const [filter, setFilter] = useUrlFilter();
  const server = useMemo(() => serverPart(filter), [filter]);
  const live = useLiveLogs(token, server);
  const [devices, setDevices] = useState<DeviceSummary[]>([]);

  useEffect(() => {
    const abort = new AbortController();
    fetchDevices(token, abort.signal).then(setDevices).catch(() => {
      // Without the list the pickers only lose their suggestions.
    });
    return () => abort.abort();
  }, [token]);

  const visible = useMemo(() => live.entries.filter((e) => matchesClient(e, filter)), [live.entries, filter]);

  return (
    <div className="logs-page">
      <LogViewer
        entries={visible}
        status={live.status}
        paused={live.paused}
        pendingCount={live.pendingCount}
        onPause={live.pause}
        onResume={live.resume}
        onNearEnd={live.loadOlder}
        loadingOlder={live.loadingOlder}
        reachedStart={live.reachedStart}
        highlight={filter.text}
        toolbar={
          <>
            <h1 className="logs-page-title">Logs</h1>
            <FilterBar filter={filter} onChange={setFilter} deviceOptions={deviceOptions(devices)} ownerOptions={ownerOptions(devices)} />
          </>
        }
        emptyText={live.entries.length === 0 ? "Waiting for log entries…" : "No entries match the filters."}
      />
    </div>
  );
}
