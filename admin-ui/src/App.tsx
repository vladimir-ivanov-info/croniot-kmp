import { BrowserRouter, Navigate, Route, Routes } from "react-router";
import { AuthProvider } from "./auth/AuthProvider";
import { useAuth } from "./auth/useAuth";
import { Layout } from "./components/Layout";
import { AccountPageRoute } from "./pages/AccountPage";
import { DevicePageRoute } from "./pages/DevicePage";
import { FlagsPage } from "./pages/FlagsPage";
import { LoginPage } from "./pages/LoginPage";
import { LogsPage } from "./pages/LogsPage";

export function AppRoutes() {
  const { token, login } = useAuth();
  if (!token) return <LoginPage onLogin={login} />;

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/logs" replace />} />
        <Route path="logs" element={<LogsPage />} />
        <Route path="devices/:uuid" element={<DevicePageRoute />} />
        <Route path="accounts/:uuid" element={<AccountPageRoute />} />
        <Route path="flags" element={<FlagsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}
