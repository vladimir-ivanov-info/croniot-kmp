import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, TOKEN_KEY } from "../auth/AuthProvider";
import type { DeviceDetail } from "../livelog/directory";
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

function renderDevice(deviceResponse: Response) {
  localStorage.setItem(TOKEN_KEY, "tok");
  vi.stubGlobal("fetch", vi.fn(async (url: string) => {
    if (url.startsWith("/api/admin/devices/")) return deviceResponse;
    if (url.startsWith("/api/admin/logs/stream")) return new Response("", { status: 200 });
    return new Response("[]", { status: 200 });
  }));
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={["/devices/watering_system_3"]}>
        <Routes>
          <Route path="/devices/:uuid" element={<DevicePageRoute />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
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

  it("says so when the device does not exist", async () => {
    renderDevice(new Response("", { status: 404 }));

    expect(await screen.findByText("Device not found.")).toBeInTheDocument();
  });
});
