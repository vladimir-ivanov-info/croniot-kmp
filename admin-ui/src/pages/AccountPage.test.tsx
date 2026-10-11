import { render, screen, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, TOKEN_KEY } from "../auth/AuthProvider";
import type { AccountDetail } from "../livelog/directory";
import { AccountPageRoute } from "./AccountPage";

const owner = { uuid: "acc-1", nickname: "vladimir", email: "v@example.com" };
const account: AccountDetail = {
  owner,
  devices: [
    { uuid: "dev-1", name: "Riego", owner, online: true, statusChangedAtMs: Date.now(), lastActivityMs: Date.now(), errors24h: 3 },
    { uuid: "dev-2", name: null, owner, online: null, errors24h: 0 },
  ],
};

function renderAccount(accountResponse: Response) {
  localStorage.setItem(TOKEN_KEY, "tok");
  const fetchMock = vi.fn(async (url: string) => {
    if (url.startsWith("/api/admin/accounts/")) return accountResponse;
    if (url.startsWith("/api/admin/logs/stream")) return new Response("", { status: 200 });
    return new Response("[]", { status: 200 });
  });
  vi.stubGlobal("fetch", fetchMock);
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={["/accounts/acc-1"]}>
        <Routes>
          <Route path="/accounts/:uuid" element={<AccountPageRoute />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe("AccountPage", () => {
  it("lists the owner's devices with status and errors, linking to each", async () => {
    renderAccount(new Response(JSON.stringify(account), { status: 200 }));

    expect(await screen.findByRole("heading", { name: "vladimir" })).toBeInTheDocument();
    const rows = screen.getAllByRole("row");
    expect(within(rows[1]).getByRole("link", { name: "Riego" })).toHaveAttribute("href", "/devices/dev-1");
    expect(within(rows[1]).getByText("Online")).toBeInTheDocument();
    expect(within(rows[1]).getByText("3")).toHaveClass("has-errors");
    expect(within(rows[2]).getByRole("link", { name: "dev-2" })).toBeInTheDocument();
    expect(within(rows[2]).getByText("Unknown")).toBeInTheDocument();
  });

  it("pins the log viewer to the owner", async () => {
    const fetchMock = renderAccount(new Response(JSON.stringify(account), { status: 200 }));

    await screen.findByRole("heading", { name: "vladimir" });

    const urls = fetchMock.mock.calls.map((c) => c[0] as string);
    expect(urls.some((u) => u.startsWith("/api/admin/logs/stream") && u.includes("owner=acc-1"))).toBe(true);
    expect(screen.queryByLabelText("Owner")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Device")).toBeInTheDocument();
  });

  it("says so when the account does not exist", async () => {
    renderAccount(new Response("", { status: 404 }));

    expect(await screen.findByText("Account not found.")).toBeInTheDocument();
  });
});
