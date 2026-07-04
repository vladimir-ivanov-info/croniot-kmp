export async function login(email: string, password: string): Promise<string> {
  const res = await fetch("/api/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email,
      password,
      deviceUuid: "admin-web",
      deviceToken: null,
      deviceProperties: {},
    }),
  });

  if (!res.ok) throw new Error("Login failed");

  const data = await res.json();
  if (!data.result?.success || !data.token) throw new Error("Invalid credentials");

  return data.token as string;
}
