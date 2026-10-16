import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "./App";
import { AuthProvider, TOKEN_KEY } from "./auth/AuthProvider";

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>
  );
}

afterEach(() => vi.unstubAllGlobals());

describe("AppRoutes", () => {
  it("shows the login page when signed out", () => {
    renderAt("/flags");

    expect(screen.getByRole("button", { name: /sign in|log in|login/i })).toBeInTheDocument();
  });

  it("shows the layout and the flags page when signed in", async () => {
    localStorage.setItem(TOKEN_KEY, "tok");
    vi.stubGlobal("fetch", vi.fn(async () => new Response("[]", { status: 200 })));

    renderAt("/flags");

    expect(await screen.findByRole("heading", { name: "Feature flags" })).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Main" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign out" })).toBeInTheDocument();
  });

  it("opens the live logs page by default", async () => {
    localStorage.setItem(TOKEN_KEY, "tok");
    // The stream ends immediately; the page shows its empty state.
    vi.stubGlobal("fetch", vi.fn(async () => new Response("", { status: 200 })));

    renderAt("/");

    expect(await screen.findByRole("heading", { name: "Logs" })).toBeInTheDocument();
    expect(screen.getByText("Waiting for log entries…")).toBeInTheDocument();
  });
});
