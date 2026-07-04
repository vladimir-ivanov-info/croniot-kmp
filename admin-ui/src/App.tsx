import { useState } from "react";
import { LoginPage } from "./pages/LoginPage";
import { FlagsPage } from "./pages/FlagsPage";

const TOKEN_KEY = "croniot_admin_token";

export default function App() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_KEY)
  );

  function handleLogin(t: string) {
    localStorage.setItem(TOKEN_KEY, t);
    setToken(t);
  }

  function handleLogout() {
    localStorage.removeItem(TOKEN_KEY);
    setToken(null);
  }

  if (!token) return <LoginPage onLogin={handleLogin} />;
  return <FlagsPage token={token} onLogout={handleLogout} />;
}
