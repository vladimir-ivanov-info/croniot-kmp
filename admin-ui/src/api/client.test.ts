import { afterEach, describe, expect, it, vi } from "vitest";
import { apiFetch, apiGetJson, setAuthSession, setUnauthorizedHandler, UnauthorizedError } from "./client";

function mockFetch(status: number, body: unknown = {}) {
  const fn = vi.fn(async () => new Response(JSON.stringify(body), { status }));
  vi.stubGlobal("fetch", fn);
  return fn;
}

afterEach(() => {
  vi.unstubAllGlobals();
  setUnauthorizedHandler(() => {});
  setAuthSession(null);
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

describe("apiFetch with a session", () => {
  const auth = (headers: unknown) => new Headers(headers as HeadersInit).get("Authorization");

  it("renews the token on 401 and retries the request once", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response("", { status: 401 }))
      .mockResolvedValueOnce(new Response("ok", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    const refresh = vi.fn(async () => "tok-2");
    setAuthSession({ getToken: () => "tok-1", refresh });

    const res = await apiFetch("/api/x", "stale");

    expect(res.status).toBe(200);
    expect(refresh).toHaveBeenCalledOnce();
    expect(auth(fetchMock.mock.calls[0][1].headers)).toBe("Bearer tok-1");
    expect(auth(fetchMock.mock.calls[1][1].headers)).toBe("Bearer tok-2");
    expect(handler).not.toHaveBeenCalled();
  });

  it("signs out when the session can't be renewed", async () => {
    mockFetch(401);
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    setAuthSession({ getToken: () => "tok-1", refresh: async () => null });

    await expect(apiFetch("/api/x", null)).rejects.toBeInstanceOf(UnauthorizedError);
    expect(handler).toHaveBeenCalledOnce();
  });

  it("signs out when the retry is still unauthorized", async () => {
    const fetchMock = mockFetch(401);
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    setAuthSession({ getToken: () => "tok-1", refresh: async () => "tok-2" });

    await expect(apiFetch("/api/x", null)).rejects.toBeInstanceOf(UnauthorizedError);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(handler).toHaveBeenCalledOnce();
  });

  it("reuses a token another request already renewed instead of refreshing again", async () => {
    let current = "tok-1";
    const fetchMock = vi.fn(async (_path: string, init?: RequestInit) => {
      if (new Headers(init?.headers).get("Authorization") === "Bearer tok-1") {
        current = "tok-2"; // another request renews while this one is in flight
        return new Response("", { status: 401 });
      }
      return new Response("ok", { status: 200 });
    });
    vi.stubGlobal("fetch", fetchMock);
    const refresh = vi.fn(async () => "never");
    setAuthSession({ getToken: () => current, refresh });

    const res = await apiFetch("/api/x", null);

    expect(res.status).toBe(200);
    expect(refresh).not.toHaveBeenCalled();
  });

  it("does not sign out on a network error while renewing", async () => {
    mockFetch(401);
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    setAuthSession({
      getToken: () => "tok-1",
      refresh: async () => {
        throw new Error("offline");
      },
    });

    await expect(apiFetch("/api/x", null)).rejects.toThrow("offline");
    expect(handler).not.toHaveBeenCalled();
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
