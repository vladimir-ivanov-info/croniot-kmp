import { afterEach, describe, expect, it, vi } from "vitest";
import { apiFetch, apiGetJson, setUnauthorizedHandler, UnauthorizedError } from "./client";

function mockFetch(status: number, body: unknown = {}) {
  const fn = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fn);
  return fn;
}

afterEach(() => {
  vi.unstubAllGlobals();
  setUnauthorizedHandler(() => {});
});

describe("apiFetch", () => {
  it("adds the bearer token when there is one", async () => {
    const fetchMock = mockFetch(200);

    await apiFetch("/api/x", "tok-1");

    const headers = new Headers((fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1].headers);
    expect(headers.get("Authorization")).toBe("Bearer tok-1");
  });

  it("sends no Authorization header without a token", async () => {
    const fetchMock = mockFetch(200);

    await apiFetch("/api/x", null);

    const headers = new Headers((fetchMock.mock.calls[0] as unknown as [string, RequestInit])[1].headers);
    expect(headers.has("Authorization")).toBe(false);
  });

  it("calls the unauthorized handler and throws on 401", async () => {
    mockFetch(401);
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(apiFetch("/api/x", "expired")).rejects.toBeInstanceOf(UnauthorizedError);
    expect(handler).toHaveBeenCalledOnce();
  });
});

describe("apiGetJson", () => {
  it("returns the parsed body", async () => {
    mockFetch(200, [{ id: "log-1" }]);

    await expect(apiGetJson("/api/x", "t")).resolves.toEqual([{ id: "log-1" }]);
  });

  it("throws on other errors without signing out", async () => {
    mockFetch(500);
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(apiGetJson("/api/x", "t")).rejects.toThrow("500");
    expect(handler).not.toHaveBeenCalled();
  });
});
