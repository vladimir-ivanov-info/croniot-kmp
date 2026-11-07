import { useEffect, useRef, useState } from "react";
import { useAuth } from "../auth/useAuth";
import {
  cancelUpdate,
  fetchFirmwareOverview,
  requestUpdate,
  uploadFirmware,
  type FirmwareOverview,
} from "./api";
import { canCancel, isFirmwareFile } from "./format";

// While an update runs the device reports every 10 % of the download, so
// a short poll keeps the bar moving; otherwise a slow one still notices
// the device's new firmware_info after a reboot.
export const ACTIVE_POLL_MS = 2000;
export const IDLE_POLL_MS = 15000;

export interface UploadState {
  fileName: string;
  fraction: number;
}

export interface FirmwareUpdate {
  overview: FirmwareOverview | null;
  // When `overview` was fetched (ms since epoch), to judge how recent a
  // finished update is without reading the clock while rendering.
  fetchedAtMs: number;
  error: string | null;
  upload: UploadState | null;
  busy: boolean;
  start: (file: File) => Promise<void>;
  cancel: () => Promise<void>;
  dismissError: () => void;
}

// Everything the device page needs to update a device by dropping a .bin:
// upload (with progress), ask the device to install it, then follow it.
export function useFirmwareUpdate(uuid: string): FirmwareUpdate {
  const { token } = useAuth();
  const [overview, setOverview] = useState<FirmwareOverview | null>(null);
  const [fetchedAtMs, setFetchedAtMs] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [upload, setUpload] = useState<UploadState | null>(null);
  const [requesting, setRequesting] = useState(false);
  const [refreshTick, setRefreshTick] = useState(0);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const active = Boolean(overview?.current && !overview.current.finished);

  useEffect(() => {
    const abort = new AbortController();
    let timer: ReturnType<typeof setTimeout> | undefined;
    const load = () => {
      fetchFirmwareOverview(token, uuid, abort.signal)
        .then((o) => {
          setOverview(o);
          setFetchedAtMs(Date.now());
          timer = setTimeout(
            load,
            o.current && !o.current.finished ? ACTIVE_POLL_MS : IDLE_POLL_MS,
          );
        })
        .catch(() => {
          if (abort.signal.aborted) return;
          timer = setTimeout(load, IDLE_POLL_MS);
        });
    };
    load();
    return () => {
      abort.abort();
      if (timer) clearTimeout(timer);
    };
  }, [token, uuid, refreshTick]);

  const refresh = () => setRefreshTick((t) => t + 1);

  const start = async (file: File) => {
    setError(null);
    if (!isFirmwareFile(file)) {
      setError(
        `${file.name} is not a firmware image: drop the .bin from the project's build folder.`,
      );
      return;
    }
    if (upload || requesting) return;
    const current = overview?.current;
    if (current && !current.finished && !canCancel(current.status)) {
      setError(`Wait for the update to ${current.version} to finish.`);
      return;
    }
    setUpload({ fileName: file.name, fraction: 0 });
    try {
      const release = await uploadFirmware(token, file, (fraction) => {
        if (mounted.current) setUpload({ fileName: file.name, fraction });
      });
      if (!mounted.current) return;
      setUpload(null);
      setRequesting(true);
      await requestUpdate(token, uuid, release.id);
    } catch (e) {
      if (mounted.current) setError(e instanceof Error ? e.message : String(e));
    } finally {
      if (mounted.current) {
        setUpload(null);
        setRequesting(false);
        refresh();
      }
    }
  };

  const cancel = async () => {
    setError(null);
    try {
      await cancelUpdate(token, uuid);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      refresh();
    }
  };

  return {
    overview,
    fetchedAtMs,
    error,
    upload,
    busy: Boolean(upload) || requesting || active,
    start,
    cancel,
    dismissError: () => setError(null),
  };
}
