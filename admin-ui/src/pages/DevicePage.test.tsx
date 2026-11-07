import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, TOKEN_KEY } from "../auth/AuthProvider";
import type { DeviceDetail } from "../livelog/directory";
import type { FirmwareOverview } from "../ota/api";
import { DevicePageRoute } from "./DevicePage";

const detail: DeviceDetail = {
  summary: {
    uuid: "watering_system_3", name: "Riego", owner: { uuid: "acc-1", nickname: "vladimir", email: "v@example.com" },
    online: true, bootId: 96, statusChangedAtMs: Date.now() - 120_000, lastActivityMs: Date.now(), errors24h: 2,
  },
  description: "Huerto",
  levelCounts24h: { "0": 2, "2": 40 },
  recordsReceived24h: 200,
  recordsDuplicated24h: 25,
};

const noFirmware: FirmwareOverview = { installed: null, current: null, history: [] };

type FetchFn = (url: string, init?: RequestInit) => Promise<Response>;

function renderDevice(deviceResponse: Response, firmware: () => FirmwareOverview = () => noFirmware, extra?: FetchFn) {
  localStorage.setItem(TOKEN_KEY, "tok");
  const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    const handled = extra ? await extra(url, init) : undefined;
    if (handled) return handled;
    if (url.endsWith("/firmware")) return new Response(JSON.stringify(firmware()), { status: 200 });
    if (url.startsWith("/api/admin/devices/")) return deviceResponse;
    if (url.startsWith("/api/admin/logs/stream")) return new Response("", { status: 200 });
    return new Response("[]", { status: 200 });
  });
  vi.stubGlobal("fetch", fetchMock);
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={["/devices/watering_system_3"]}>
        <Routes>
          <Route path="/devices/:uuid" element={<DevicePageRoute />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
  return fetchMock;
}

// Stands in for the browser's XMLHttpRequest: reports half the file at
// once, and answers like the server's upload endpoint when the test calls
// finish(), so the test sees the half-way state for sure.
class FakeUpload {
  static sent: Blob[] = [];
  static finish: () => void = () => {};
  status = 0;
  responseText = "";
  upload: { onprogress: ((e: { lengthComputable: boolean; loaded: number; total: number }) => void) | null } = { onprogress: null };
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onabort: (() => void) | null = null;
  open() {}
  setRequestHeader() {}
  abort() {}
  send(body: Blob) {
    FakeUpload.sent.push(body);
    FakeUpload.finish = () => {
      this.status = 201;
      this.responseText = JSON.stringify({ id: 12, project: "croniot-watering-system", version: "v0.12.0", sizeBytes: 100, sha256: "a", createdAt: "2026-11-07T19:00:00Z" });
      this.onload?.();
    };
    setTimeout(() => this.upload.onprogress?.({ lengthComputable: true, loaded: 50, total: 100 }), 0);
  }
}

const installed: FirmwareOverview = {
  installed: {
    project: "croniot-watering-system", version: "v0.11.1", builtAt: "2026-11-07T19:50:09", idfVersion: "v5.5.3",
    sizeBytes: 2010640, slot: "ota_0", reportedAt: "2026-11-07T19:55:00Z", release: null,
  },
  current: null,
  history: [
    {
      requestId: "r0", releaseId: 11, version: "v0.11.1", fromVersion: "v0.11.0", status: "confirmed", finished: true,
      progress: 100, detail: null, requestedAt: "2026-11-01T10:00:00Z", updatedAt: "2026-11-01T10:03:05Z", finishedAt: "2026-11-01T10:03:05Z",
    },
  ],
};

function dropFile(file: File) {
  fireEvent.drop(screen.getByTestId("device-page"), { dataTransfer: { types: ["Files"], files: [file] } });
}

afterEach(() => vi.unstubAllGlobals());

describe("DevicePage", () => {
  it("shows the device, its owner, status, counts and duplicate ratio", async () => {
    renderDevice(new Response(JSON.stringify(detail), { status: 200 }));

    expect(await screen.findByRole("heading", { name: "Riego" })).toBeInTheDocument();
    expect(screen.getByText("Online")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "vladimir" })).toHaveAttribute("href", "/accounts/acc-1");
    expect(screen.getByText("Error 2")).toBeInTheDocument();
    expect(screen.getByText("Info 40")).toBeInTheDocument();
    expect(screen.getByText("12.5 % (25 of 200 records)")).toBeInTheDocument();
    // The log viewer is pinned to this device: no device picker.
    expect(screen.queryByLabelText("Device")).not.toBeInTheDocument();
  });

  it("shows the installed firmware and the update history", async () => {
    renderDevice(new Response(JSON.stringify(detail), { status: 200 }), () => installed);

    expect(await screen.findByText("v0.11.1", { selector: "dd" })).toBeInTheDocument();
    expect(screen.getByText("2026-11-07 19:50:09")).toBeInTheDocument();
    expect(screen.getByText("1.92 MB")).toBeInTheDocument();
    expect(screen.getByText("ota_0")).toBeInTheDocument();
    expect(screen.getByText("Update history (1)")).toBeInTheDocument();
    expect(screen.getByText("3 min 5 s")).toBeInTheDocument();
  });

  it("uploads a dropped .bin with progress, asks the device to install it and follows the download", async () => {
    vi.stubGlobal("XMLHttpRequest", FakeUpload);
    FakeUpload.sent = [];
    let requested = false;
    const fetchMock = renderDevice(
      new Response(JSON.stringify(detail), { status: 200 }),
      () =>
        requested
          ? {
              ...installed,
              current: {
                requestId: "r1", releaseId: 12, version: "v0.12.0", fromVersion: "v0.11.1", status: "downloading", finished: false,
                progress: 40, detail: null, requestedAt: "2026-11-07T20:00:00Z", updatedAt: "2026-11-07T20:00:10Z", finishedAt: null,
              },
            }
          : installed,
      async (url, init) => {
        if (url.endsWith("/ota") && init?.method === "PUT") {
          requested = true;
          return new Response("{}", { status: 200 });
        }
        return undefined as unknown as Response;
      }
    );
    await screen.findByText("v0.11.1", { selector: "dd" });

    dropFile(new File([new Uint8Array(100)], "croniot-watering-system.bin"));

    expect(await screen.findByText("Uploading croniot-watering-system.bin")).toBeInTheDocument();
    expect(await screen.findByText("50 %")).toBeInTheDocument();
    act(() => FakeUpload.finish());
    // Upload done, update requested, overview fetched again.
    expect(await screen.findByText("Downloading", {}, { timeout: 3000 })).toBeInTheDocument();
    expect(screen.getByText("40 %")).toBeInTheDocument();
    expect(FakeUpload.sent).toHaveLength(1);
    const put = fetchMock.mock.calls.find(([u, i]) => u.endsWith("/ota") && i?.method === "PUT");
    expect(JSON.parse(String(put?.[1]?.body))).toEqual({ releaseId: 12 });
  });

  it("refuses a file that is not a .bin", async () => {
    renderDevice(new Response(JSON.stringify(detail), { status: 200 }), () => installed);
    await screen.findByText("v0.11.1", { selector: "dd" });

    dropFile(new File(["x"], "notes.txt"));

    expect(await screen.findByRole("alert")).toHaveTextContent("notes.txt is not a firmware image");
  });

  it("says so when the device does not exist", async () => {
    renderDevice(new Response("", { status: 404 }));

    expect(await screen.findByText("Device not found.")).toBeInTheDocument();
  });
});
