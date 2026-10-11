import { useVirtualizer } from "@tanstack/react-virtual";
import { useCallback, useRef, useState } from "react";
import type { StreamStatus } from "../livelog/logStream";
import type { LogEntry } from "../livelog/types";
import { LiveControls } from "./LiveControls";
import { LogRow } from "./LogRow";
import "./LogViewer.css";

interface Props {
  entries: LogEntry[];
  status: StreamStatus;
  paused: boolean;
  pendingCount: number;
  onPause: () => void;
  onResume: () => void;
  // Called when the user scrolls near the oldest loaded entry.
  onNearEnd?: () => void;
  toolbar?: React.ReactNode;
  emptyText?: string;
}

// Scrolling this far from the top (where new entries arrive) pauses the
// list, like a terminal; scrolling back resumes it - unless the user
// paused with the button.
const AUTO_PAUSE_PX = 48;
const AUTO_RESUME_PX = 8;
const NEAR_END_ROWS = 30;

export function LogViewer({ entries, status, paused, pendingCount, onPause, onResume, onNearEnd, toolbar, emptyText }: Props) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const pausedByScroll = useRef(false);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  // TanStack Virtual returns non-memoizable functions, so React Compiler
  // skips this component; the virtualizer is not passed to memoized
  // children, which is the case the warning is about.
  // eslint-disable-next-line react-hooks/incompatible-library
  const virtualizer = useVirtualizer({
    count: entries.length,
    getScrollElement: () => scrollRef.current,
    estimateSize: () => 30,
    overscan: 20,
    getItemKey: (index) => entries[index].id,
  });

  const toggle = useCallback((id: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const resume = () => {
    pausedByScroll.current = false;
    onResume();
    scrollRef.current?.scrollTo({ top: 0 });
  };

  const pause = () => {
    pausedByScroll.current = false;
    onPause();
  };

  const onScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    if (!paused && el.scrollTop > AUTO_PAUSE_PX) {
      pausedByScroll.current = true;
      onPause();
    } else if (paused && pausedByScroll.current && el.scrollTop < AUTO_RESUME_PX) {
      pausedByScroll.current = false;
      onResume();
    }
    const items = virtualizer.getVirtualItems();
    const last = items[items.length - 1];
    if (onNearEnd && last && last.index >= entries.length - NEAR_END_ROWS) onNearEnd();
  };

  return (
    <section className="log-viewer" aria-label="Log entries">
      <div className="log-viewer-bar">
        {toolbar}
        <LiveControls status={status} paused={paused} pendingCount={pendingCount} onPause={pause} onResume={resume} />
      </div>
      {paused && pendingCount > 0 && (
        <button type="button" className="log-viewer-pending" onClick={resume}>
          ▲ {pendingCount} new {pendingCount === 1 ? "entry" : "entries"} (paused) — click to resume
        </button>
      )}
      <div className="log-viewer-scroll" ref={scrollRef} onScroll={onScroll}>
        {entries.length === 0 ? (
          <p className="log-viewer-empty">{emptyText ?? "No log entries yet."}</p>
        ) : (
          <div className="log-viewer-inner" style={{ height: virtualizer.getTotalSize() }}>
            {virtualizer.getVirtualItems().map((item) => {
              const entry = entries[item.index];
              return (
                <div
                  key={item.key}
                  data-index={item.index}
                  ref={virtualizer.measureElement}
                  className="log-viewer-item"
                  style={{ transform: `translateY(${item.start}px)` }}
                >
                  <LogRow entry={entry} expanded={expanded.has(entry.id)} onToggle={toggle} />
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
