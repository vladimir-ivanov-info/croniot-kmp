import { useEffect, useState } from "react";
import { UnauthorizedError } from "../api/client";
import { fetchFlags, setFlag } from "../api/featureFlags";
import { useAuth } from "../auth/useAuth";
import { FlagRow } from "../components/FlagRow";
import type { FeatureFlag } from "../types";

export function FlagsPage() {
  const { token } = useAuth();
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
      const updated = await setFlag(name, enabled, token ?? "");
      setFlags((prev) => prev.map((f) => (f.name === name ? updated : f)));
    } catch (err) {
      // UnauthorizedError already signed the user out.
      if (!(err instanceof UnauthorizedError)) setError(`Failed to update "${name}"`);
    } finally {
      setUpdating(null);
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.main}>
        <h1 style={styles.title}>Feature flags</h1>
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
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    background: "#f3f4f6",
    fontFamily: "system-ui, sans-serif",
  },
  title: {
    margin: "0 0 16px",
    fontSize: 20,
    fontWeight: 700,
    color: "#111827",
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
