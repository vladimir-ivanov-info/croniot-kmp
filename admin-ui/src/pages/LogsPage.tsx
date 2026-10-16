import { FilteredLogViewer } from "../components/FilteredLogViewer";
import "./LogsPage.css";

// Every platform's logs and events, live, filtered by the URL query.
export function LogsPage() {
  return (
    <div className="logs-page">
      <FilteredLogViewer title={<h1 className="logs-page-title">Logs</h1>} />
    </div>
  );
}
