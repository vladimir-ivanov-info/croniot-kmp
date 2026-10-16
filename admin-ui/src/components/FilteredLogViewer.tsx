import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { deviceOptions, fetchDevices, ownerOptions, type DeviceSummary } from "../livelog/directory";
import { matchesClient, serverPart } from "../livelog/filters";
import { useLiveLogs } from "../livelog/useLiveLogs";
import { useUrlFilter } from "../livelog/useUrlFilter";
import { FilterBar } from "./FilterBar";
import { LogViewer } from "./LogViewer";

interface Props {
  title: React.ReactNode;
  // Pages about one device or one owner pin it here; the user's own
  // filters (from the URL) apply on top.
  fixedDevice?: string;
  fixedOwner?: string;
}

// Live + stored logs with the filter bar - the Logs page, and the log part
// of the device and owner pages.
export function FilteredLogViewer({ title, fixedDevice, fixedOwner }: Props) {
  const { token } = useAuth();
  const [filter, setFilter] = useUrlFilter();
  const server = useMemo(() => {
    const base = serverPart(filter);
    return {
      ...base,
      devices: fixedDevice ? [fixedDevice] : base.devices,
      owners: fixedOwner ? [fixedOwner] : base.owners,
    };
  }, [filter, fixedDevice, fixedOwner]);
  const live = useLiveLogs(token, server);
  const [devices, setDevices] = useState<DeviceSummary[]>([]);

  useEffect(() => {
    if (fixedDevice) return;
    const abort = new AbortController();
    fetchDevices(token, abort.signal).then(setDevices).catch(() => {
      // Without the list the pickers only lose their suggestions.
    });
    return () => abort.abort();
  }, [token, fixedDevice]);

  const visibleDevices = fixedOwner ? devices.filter((d) => d.owner.uuid === fixedOwner) : devices;
  const visible = useMemo(() => live.entries.filter((e) => matchesClient(e, filter)), [live.entries, filter]);

  return (
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
          {title}
          <FilterBar
            filter={filter}
            onChange={setFilter}
            deviceOptions={deviceOptions(visibleDevices)}
            ownerOptions={ownerOptions(visibleDevices)}
            showDevicePicker={!fixedDevice}
            showOwnerPicker={!fixedDevice && !fixedOwner}
          />
        </>
      }
      emptyText={live.entries.length === 0 ? "Waiting for log entries…" : "No entries match the filters."}
    />
  );
}
