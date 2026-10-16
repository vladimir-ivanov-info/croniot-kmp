import { FilteredLogViewer } from "../components/FilteredLogViewer";
import { ServerLogLevelControl } from "../components/ServerLogLevelControl";
import "./LogsPage.css";

// Every platform's logs and events, live, filtered by the URL query.
export function LogsPage() {
  return (
    <div className="logs-page">
      <FilteredLogViewer
        title={
          <div className="logs-page-heading">
            <h1 className="logs-page-title">Logs</h1>
            <ServerLogLevelControl />
          </div>
        }
      />
    </div>
  );
}
