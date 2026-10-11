import { useAuth } from "../auth/useAuth";
import { LogViewer } from "../components/LogViewer";
import { NO_FILTER } from "../livelog/types";
import { useLiveLogs } from "../livelog/useLiveLogs";
import "./LogsPage.css";

// Every platform's logs and events, live. Filters come in the next step.
export function LogsPage() {
  const { token } = useAuth();
  const live = useLiveLogs(token, NO_FILTER);

  return (
    <div className="logs-page">
      <LogViewer
        entries={live.entries}
        status={live.status}
        paused={live.paused}
        pendingCount={live.pendingCount}
        onPause={live.pause}
        onResume={live.resume}
        toolbar={<h1 className="logs-page-title">Logs</h1>}
        emptyText="Waiting for log entries…"
      />
    </div>
  );
}
