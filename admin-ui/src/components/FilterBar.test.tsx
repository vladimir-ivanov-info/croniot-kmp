import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { EMPTY_FILTER } from "../livelog/filters";
import { FilterBar } from "./FilterBar";

const devices = [{ value: "dev-1", label: "Riego (dev-1)" }];

describe("FilterBar", () => {
  it("shows every chip selected by default", () => {
    render(<FilterBar filter={EMPTY_FILTER} onChange={() => {}} deviceOptions={devices} ownerOptions={[]} />);

    for (const name of ["SRV", "IoT", "Android", "iOS", "Logs", "Events", "Error", "Trace"]) {
      expect(screen.getByRole("button", { name })).toHaveAttribute("aria-pressed", "true");
    }
    expect(screen.queryByRole("button", { name: "Clear filters" })).not.toBeInTheDocument();
  });

  it("turns one platform off and keeps the rest", async () => {
    const onChange = vi.fn();
    render(<FilterBar filter={EMPTY_FILTER} onChange={onChange} deviceOptions={devices} ownerOptions={[]} />);

    await userEvent.click(screen.getByRole("button", { name: "SRV" }));

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_FILTER, platforms: ["IOT", "ANDROID", "IOS"] });
  });

  it("'Serious only' keeps Error and Warn, and toggles back to all", async () => {
    const onChange = vi.fn();
    const { rerender } = render(<FilterBar filter={EMPTY_FILTER} onChange={onChange} deviceOptions={devices} ownerOptions={[]} />);

    await userEvent.click(screen.getByRole("button", { name: "Serious only" }));
    expect(onChange).toHaveBeenLastCalledWith({ ...EMPTY_FILTER, levels: [0, 1] });

    rerender(<FilterBar filter={{ ...EMPTY_FILTER, levels: [0, 1] }} onChange={onChange} deviceOptions={devices} ownerOptions={[]} />);
    expect(screen.getByRole("button", { name: "Info" })).toHaveAttribute("aria-pressed", "false");
    await userEvent.click(screen.getByRole("button", { name: "Serious only" }));
    expect(onChange).toHaveBeenLastCalledWith(EMPTY_FILTER);
  });

  it("adds a device from its suggestion and removes it with ×", async () => {
    const onChange = vi.fn();
    const { rerender } = render(<FilterBar filter={EMPTY_FILTER} onChange={onChange} deviceOptions={devices} ownerOptions={[]} />);

    await userEvent.type(screen.getByLabelText("Device"), "dev-1{Enter}");
    expect(onChange).toHaveBeenLastCalledWith({ ...EMPTY_FILTER, devices: ["dev-1"] });

    rerender(<FilterBar filter={{ ...EMPTY_FILTER, devices: ["dev-1"] }} onChange={onChange} deviceOptions={devices} ownerOptions={[]} />);
    await userEvent.click(screen.getByRole("button", { name: "Remove Riego (dev-1)" }));
    expect(onChange).toHaveBeenLastCalledWith(EMPTY_FILTER);
  });

  it("applies the search text after the user stops typing", async () => {
    const onChange = vi.fn();
    render(<FilterBar filter={EMPTY_FILTER} onChange={onChange} deviceOptions={devices} ownerOptions={[]} />);

    await userEvent.type(screen.getByRole("searchbox"), "fail");

    await vi.waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ ...EMPTY_FILTER, text: "fail" }));
  });
});
