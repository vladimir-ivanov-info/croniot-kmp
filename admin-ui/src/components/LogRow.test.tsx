import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import type { LogEntry } from "../livelog/types";
import { LiveControls } from "./LiveControls";
import { LogRow } from "./LogRow";

const deviceError: LogEntry = {
  id: "evt-7", platform: "IOT", kind: "EVENT", level: 0, tag: "Uplink", message: "publish failed",
  receivedAtMs: Date.now(), device: { uuid: "watering_system_3", name: "Riego" },
  owner: { uuid: "acc-1", nickname: "vladimir", email: "v@example.com" }, bootId: 96, seq: 12, uptimeMs: 4500,
};

describe("LogRow", () => {
  it("marks an error with its class, icon and a readable label", () => {
    render(<LogRow entry={deviceError} expanded={false} onToggle={() => {}} />, { wrapper: MemoryRouter });

    expect(screen.getByTestId("log-row")).toHaveClass("sev-error");
    expect(screen.getByText("Error")).toBeInTheDocument();
    expect(screen.getByText("EVENT")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Riego" })).toHaveAttribute("href", "/devices/watering_system_3");
    expect(screen.getByRole("link", { name: "vladimir" })).toHaveAttribute("href", "/accounts/acc-1");
    expect(screen.getByText("publish failed")).toBeInTheDocument();
  });

  it("asks to expand on click and shows the device fields when expanded", async () => {
    const onToggle = vi.fn();
    const { rerender } = render(<LogRow entry={deviceError} expanded={false} onToggle={onToggle} />, { wrapper: MemoryRouter });

    await userEvent.click(screen.getByRole("button", { name: "Show details" }));
    expect(onToggle).toHaveBeenCalledWith("evt-7");

    rerender(<LogRow entry={deviceError} expanded={true} onToggle={onToggle} />);
    expect(screen.getByText("96")).toBeInTheDocument();
    expect(screen.getByText("4.5 s")).toBeInTheDocument();
  });

  it("shows server lines without device fields and with the exception when expanded", () => {
    const serverLine: LogEntry = {
      id: "srv-1", platform: "SERVER", kind: "LOG", level: 3, tag: "Application", message: "debug detail",
      receivedAtMs: Date.now(), thread: "worker-1", throwable: "java.lang.IllegalStateException: boom",
    };

    render(<LogRow entry={serverLine} expanded={true} onToggle={() => {}} />, { wrapper: MemoryRouter });

    expect(screen.getByTestId("log-row")).toHaveClass("sev-debug");
    expect(screen.getByText("SRV")).toBeInTheDocument();
    expect(screen.queryByText("EVENT")).not.toBeInTheDocument();
    expect(screen.getByText("java.lang.IllegalStateException: boom")).toBeInTheDocument();
  });
});

describe("LiveControls", () => {
  it("offers Pause while live and Resume with the pending count while paused", async () => {
    const onPause = vi.fn();
    const onResume = vi.fn();
    const { rerender } = render(
      <LiveControls status="live" paused={false} pendingCount={0} onPause={onPause} onResume={onResume} />
    );

    expect(screen.getByRole("status")).toHaveTextContent("Live");
    await userEvent.click(screen.getByRole("button", { name: /pause/i }));
    expect(onPause).toHaveBeenCalled();

    rerender(<LiveControls status="live" paused={true} pendingCount={37} onPause={onPause} onResume={onResume} />);
    expect(screen.getByRole("status")).toHaveTextContent("Paused");
    await userEvent.click(screen.getByRole("button", { name: /resume \(37 new\)/i }));
    expect(onResume).toHaveBeenCalled();
  });
});
