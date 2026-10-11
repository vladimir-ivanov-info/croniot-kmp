import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, TOKEN_KEY } from "../auth/AuthProvider";
import { ServerLogLevelControl } from "./ServerLogLevelControl";

afterEach(() => vi.unstubAllGlobals());

describe("ServerLogLevelControl", () => {
  it("shows the current level and raises it temporarily", async () => {
    localStorage.setItem(TOKEN_KEY, "tok");
    const revertsAt = new Date(2026, 9, 12, 11, 30).getTime();
    const fetchMock = vi.fn(async (_url: string, init?: RequestInit) =>
      init?.method === "PUT"
        ? new Response(JSON.stringify({ root: "DEBUG", overrides: { ROOT: "DEBUG" }, revertsAtMs: revertsAt }), { status: 200 })
        : new Response(JSON.stringify({ root: "INFO", overrides: {}, revertsAtMs: null }), { status: 200 })
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<AuthProvider><ServerLogLevelControl /></AuthProvider>);
    const select = await screen.findByLabelText("Server level");
    expect(select).toHaveValue("INFO");

    await userEvent.selectOptions(select, "DEBUG");

    expect(await screen.findByText(/until/)).toBeInTheDocument();
    expect(screen.getByLabelText("Server level")).toHaveValue("DEBUG");
    const put = fetchMock.mock.calls.find((c) => c[1]?.method === "PUT")!;
    expect(JSON.parse(String(put[1]!.body))).toEqual({ level: "DEBUG", ttlMinutes: 30 });
  });

  it("renders nothing if the level cannot be read", async () => {
    localStorage.setItem(TOKEN_KEY, "tok");
    vi.stubGlobal("fetch", vi.fn(async () => new Response("", { status: 500 })));

    const { container } = render(<AuthProvider><ServerLogLevelControl /></AuthProvider>);
    await new Promise((r) => setTimeout(r, 20));

    expect(container).toBeEmptyDOMElement();
  });
});
