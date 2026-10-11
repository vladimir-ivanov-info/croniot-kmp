import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router";
import { filterFromSearchParams, filterToSearchParams, type LogFilter } from "./filters";

// The viewer's filter lives in the URL query string.
export function useUrlFilter(): [LogFilter, (next: LogFilter) => void] {
  const [params, setParams] = useSearchParams();
  const key = params.toString();
  const filter = useMemo(() => filterFromSearchParams(new URLSearchParams(key)), [key]);
  const setFilter = useCallback(
    (next: LogFilter) => setParams(filterToSearchParams(next), { replace: false }),
    [setParams]
  );
  return [filter, setFilter];
}
