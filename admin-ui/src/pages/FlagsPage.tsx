import { useEffect, useState } from "react";
import { fetchFlags, setFlag } from "../api/featureFlags";
import { FlagRow } from "../components/FlagRow";
import type { FeatureFlag } from "../types";

interface Props {
  token: string;
  onLogout: () => void;
}

export function FlagsPage({ token, onLogout }: Props) {
  const [flags, setFlags] = useState<FeatureFlag[]>([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchFlags()
      .then(setFlags)
      .catch(() => setError("Failed to load flags"))
      .finally(() => setLoading(false));
  }, []);

  async function handleToggle(name: string, enabled: boolean) {
    setUpdating(name);
    setError(null);
    try {
      const updated = await setFlag(name, enabled, token);
      setFlags((prev) => prev.map((f) => (f.name === name ? updated : f)));
    } catch (err) {
      if (err instanceof Error && err.message === "Unauthorized") {
        onLogout();
      } else {
        setError(`Failed to update "${name}"`);
      }
    } finally {
      setUpdating(null);
    }
  }

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <div style={styles.brand}>
          <img src="/logo.png" alt="Croniot Logo" style={styles.logo} />
          <h1 style={styles.title}>Croniot Feature Flags</h1>
        </div>
        <button style={styles.logoutBtn} onClick={onLogout}>
          Sign out
        </button>
      </header>

      <main style={styles.main}>
        {error && <div style={styles.error}>{error}</div>}

        <div style={styles.card}>
          {loading && <p style={styles.empty}>Loading…</p>}
          {!loading && flags.length === 0 && (
            <p style={styles.empty}>No flags found.</p>
          )}
          {flags.map((flag) => (
            <FlagRow
              key={flag.name}
              flag={flag}
              onToggle={handleToggle}
              disabled={updating === flag.name}
            />
          ))}
        </div>
      </main>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: "100vh",
    background: "#f3f4f6",
    fontFamily: "system-ui, sans-serif",
  },
  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    padding: "20px 32px",
    background: "#fff",
    borderBottom: "1px solid #e5e7eb",
    boxShadow: "0 1px 4px rgba(0,0,0,0.04)",
  },
  title: {
    margin: 0,
    fontSize: 20,
    fontWeight: 700,
    color: "#111827",
  },
  brand: {
    display: "flex",
    alignItems: "center",
    gap: "12px",
  },
  logo: {
    width: "40px",
    height: "40px",
  },
  logoutBtn: {
    padding: "7px 16px",
    borderRadius: 8,
    border: "1px solid #d1d5db",
    background: "#fff",
    fontSize: 14,
    cursor: "pointer",
    color: "#374151",
  },
  main: {
    maxWidth: 680,
    margin: "32px auto",
    padding: "0 16px",
  },
  card: {
    background: "#fff",
    borderRadius: 12,
    boxShadow: "0 2px 12px rgba(0,0,0,0.06)",
    overflow: "hidden",
  },
  error: {
    marginBottom: 16,
    padding: "10px 16px",
    borderRadius: 8,
    background: "#fef2f2",
    color: "#ef4444",
    fontSize: 14,
  },
  empty: {
    padding: 24,
    textAlign: "center",
    color: "#6b7280",
    fontSize: 15,
  },
};
