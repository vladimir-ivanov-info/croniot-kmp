import { useEffect, useId, useState } from "react";
import type { PickerOption } from "../livelog/directory";
import { isSelected, SERIOUS_LEVELS, toggleValue, EMPTY_FILTER, type LogFilter } from "../livelog/filters";
import { PLATFORM_LABELS } from "../livelog/format";
import { KINDS, LEVEL_NAMES, LEVELS, PLATFORMS } from "../livelog/types";
import "./FilterBar.css";

interface Props {
  filter: LogFilter;
  onChange: (next: LogFilter) => void;
  deviceOptions: PickerOption[];
  ownerOptions: PickerOption[];
  // Hidden on pages that already fix the device or owner.
  showDevicePicker?: boolean;
  showOwnerPicker?: boolean;
}

const TEXT_DEBOUNCE_MS = 250;

export function FilterBar({ filter, onChange, deviceOptions, ownerOptions, showDevicePicker = true, showOwnerPicker = true }: Props) {
  const [text, setText] = useState(filter.text);
  // Follow the URL when it changes from outside (Back button, Clear).
  const [urlText, setUrlText] = useState(filter.text);
  if (filter.text !== urlText) {
    setUrlText(filter.text);
    setText(filter.text);
  }

  // Typing updates the URL (and the list) a moment after the user stops.
  useEffect(() => {
    if (text === filter.text) return;
    const timer = setTimeout(() => onChange({ ...filter, text }), TEXT_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [text, filter, onChange]);

  const seriousOnly =
    filter.levels.length === SERIOUS_LEVELS.length && SERIOUS_LEVELS.every((l) => filter.levels.includes(l));
  const isEmpty = JSON.stringify({ ...filter, text }) === JSON.stringify(EMPTY_FILTER);

  return (
    <div className="filter-bar" role="group" aria-label="Filters">
      <ChipGroup label="Platform">
        {PLATFORMS.map((p) => (
          <Chip key={p} pressed={isSelected(filter.platforms, p)} onClick={() => onChange({ ...filter, platforms: toggleValue(filter.platforms, p, PLATFORMS) })}>
            {PLATFORM_LABELS[p]}
          </Chip>
        ))}
      </ChipGroup>

      <ChipGroup label="Type">
        {KINDS.map((k) => (
          <Chip key={k} pressed={isSelected(filter.kinds, k)} onClick={() => onChange({ ...filter, kinds: toggleValue(filter.kinds, k, KINDS) })}>
            {k === "LOG" ? "Logs" : "Events"}
          </Chip>
        ))}
      </ChipGroup>

      <ChipGroup label="Level">
        {LEVELS.map((l) => (
          <Chip key={l} className={`chip-level-${l}`} pressed={isSelected(filter.levels, l)} onClick={() => onChange({ ...filter, levels: toggleValue(filter.levels, l, LEVELS) })}>
            {LEVEL_NAMES[l]}
          </Chip>
        ))}
        <Chip pressed={seriousOnly} onClick={() => onChange({ ...filter, levels: seriousOnly ? [] : [...SERIOUS_LEVELS] })}>
          Serious only
        </Chip>
      </ChipGroup>

      {showDevicePicker && (
        <MultiPicker label="Device" options={deviceOptions} chosen={filter.devices} onChange={(devices) => onChange({ ...filter, devices })} />
      )}
      {showOwnerPicker && (
        <MultiPicker label="Owner" options={ownerOptions} chosen={filter.owners} onChange={(owners) => onChange({ ...filter, owners })} />
      )}
      <MultiPicker label="Tag" options={[]} chosen={filter.tags} onChange={(tags) => onChange({ ...filter, tags })} freeText />

      <label className="filter-search">
        <span className="visually-hidden">Search</span>
        <input type="search" placeholder="Search text…" value={text} onChange={(e) => setText(e.target.value)} />
      </label>

      {!isEmpty && (
        <button type="button" className="filter-clear" onClick={() => { setText(""); onChange(EMPTY_FILTER); }}>
          Clear filters
        </button>
      )}
    </div>
  );
}

function ChipGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="chip-group" role="group" aria-label={label}>
      <span className="chip-group-label">{label}</span>
      {children}
    </div>
  );
}

function Chip({ pressed, onClick, children, className = "" }: { pressed: boolean; onClick: () => void; children: React.ReactNode; className?: string }) {
  return (
    <button type="button" className={`chip ${className}`} aria-pressed={pressed} onClick={onClick}>
      {children}
    </button>
  );
}

// Several values chosen from `options` (or typed, with freeText), shown as
// removable chips.
function MultiPicker({ label, options, chosen, onChange, freeText = false }: {
  label: string;
  options: PickerOption[];
  chosen: string[];
  onChange: (next: string[]) => void;
  freeText?: boolean;
}) {
  const [draft, setDraft] = useState("");
  const listId = useId();
  const labelOf = (value: string) => options.find((o) => o.value === value)?.label ?? value;

  const add = (raw: string) => {
    const typed = raw.trim();
    if (!typed) return;
    const match = options.find((o) => o.label === typed || o.value === typed);
    const value = match?.value ?? (freeText ? typed : null);
    if (value && !chosen.includes(value)) onChange([...chosen, value]);
    setDraft("");
  };

  return (
    <div className="picker">
      <label>
        <span className="chip-group-label">{label}</span>
        <input
          list={options.length > 0 ? listId : undefined}
          value={draft}
          placeholder={freeText ? "type + Enter" : "add…"}
          onChange={(e) => {
            const value = e.target.value;
            // Picking from the datalist fills the whole label at once.
            if (options.some((o) => o.label === value)) add(value);
            else setDraft(value);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              add(draft);
            }
          }}
        />
      </label>
      {options.length > 0 && (
        <datalist id={listId}>
          {options.map((o) => (
            <option key={o.value} value={o.label} />
          ))}
        </datalist>
      )}
      {chosen.map((value) => (
        <span key={value} className="picker-chip">
          {labelOf(value)}
          <button type="button" aria-label={`Remove ${labelOf(value)}`} onClick={() => onChange(chosen.filter((v) => v !== value))}>
            ×
          </button>
        </span>
      ))}
    </div>
  );
}
