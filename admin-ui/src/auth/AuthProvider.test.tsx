import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiFetch } from "../api/client";
import { AuthProvider, REFRESH_TOKEN_KEY, TOKEN_KEY } from "./AuthProvider";
import { useAuth } from "./useAuth";

function Probe() {
  const { token } = useAuth();
  return <p>{token ?? "signed-out"}</p>;
}

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status });

beforeEach(() => {
  localStorage.setItem(TOKEN_KEY, "old-access");
  localStorage.setItem(REFRESH_TOKEN_KEY, "old-refresh");
});

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
});

describe("AuthProvider session renewal", () => {
  it("renews an expired token with the refresh token and stores the rotated pair", async () => {
    const fetchMock = vi.fn(async (path: string, init?: RequestInit) => {
      if (path === "/api/token/refresh") {
        expect(JSON.parse(init?.body as string)).toEqual({ refreshToken: "old-refresh" });
        return json({ result: { success: true }, token: "new-access", refreshToken: "new-refresh" });
      }
      const bearer = new Headers(init?.headers).get("Authorization");
      return bearer === "Bearer new-access" ? json({ ok: true }) : json({}, 401);
    });
    vi.stubGlobal("fetch", fetchMock);
    render(<AuthProvider><Probe /></AuthProvider>);

    const res = await apiFetch("/api/x", "old-access");

    expect(res.status).toBe(200);
    expect(localStorage.getItem(TOKEN_KEY)).toBe("new-access");
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe("new-refresh");
    expect(screen.getByText("old-access")).toBeInTheDocument();
  });

  it("shares one renewal between parallel requests", async () => {
    let refreshCalls = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (path: string, init?: RequestInit) => {
        if (path === "/api/token/refresh") {
          refreshCalls += 1;
          return json({ result: { success: true }, token: "new-access", refreshToken: "new-refresh" });
        }
        const bearer = new Headers(init?.headers).get("Authorization");
        return bearer === "Bearer new-access" ? json({}) : json({}, 401);
      })
    );
    render(<AuthProvider><Probe /></AuthProvider>);

    const results = await Promise.all([apiFetch("/api/a", null), apiFetch("/api/b", null), apiFetch("/api/c", null)]);

    expect(results.map((r) => r.status)).toEqual([200, 200, 200]);
    expect(refreshCalls).toBe(1);
  });

  it("signs out when the refresh token is rejected", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => json({}, 401)));
    render(<AuthProvider><Probe /></AuthProvider>);

    await expect(apiFetch("/api/x", null)).rejects.toThrow("Unauthorized");

    await waitFor(() => expect(screen.getByText("signed-out")).toBeInTheDocument());
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
  });
});
