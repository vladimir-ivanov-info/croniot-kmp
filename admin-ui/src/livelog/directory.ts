import { apiGetJson } from "../api/client";
import type { LogOwnerRef } from "./types";

// Mirrors the server's AdminDeviceSummaryDto / AdminDeviceDetailDto /
// AdminAccountDetailDto (com.server.croniot.admin).
export interface DeviceSummary {
  uuid: string;
  name?: string | null;
  owner: LogOwnerRef;
  online?: boolean | null;
  bootId?: number | null;
  statusChangedAtMs?: number | null;
  lastActivityMs?: number | null;
  errors24h: number;
}

export interface DeviceDetail {
  summary: DeviceSummary;
  description?: string | null;
  levelCounts24h: Record<string, number>;
  recordsReceived24h: number;
  recordsDuplicated24h: number;
}

export interface AccountDetail {
  owner: LogOwnerRef;
  devices: DeviceSummary[];
}

export const fetchDevices = (token: string | null, signal?: AbortSignal) =>
  apiGetJson<DeviceSummary[]>("/api/admin/devices", token, signal);

export const fetchDevice = (token: string | null, uuid: string, signal?: AbortSignal) =>
  apiGetJson<DeviceDetail>(`/api/admin/devices/${encodeURIComponent(uuid)}`, token, signal);

export const fetchAccount = (token: string | null, uuid: string, signal?: AbortSignal) =>
  apiGetJson<AccountDetail>(`/api/admin/accounts/${encodeURIComponent(uuid)}`, token, signal);

// Suggestions for the device and owner filters.
export interface PickerOption {
  value: string;
  label: string;
}

export function deviceOptions(devices: DeviceSummary[]): PickerOption[] {
  return devices.map((d) => ({ value: d.uuid, label: d.name ? `${d.name} (${d.uuid})` : d.uuid }));
}

export function ownerOptions(devices: DeviceSummary[]): PickerOption[] {
  const owners = new Map(devices.map((d) => [d.owner.uuid, d.owner]));
  return [...owners.values()].map((o) => ({ value: o.uuid, label: o.nickname ? `${o.nickname} <${o.email}>` : o.email }));
}
