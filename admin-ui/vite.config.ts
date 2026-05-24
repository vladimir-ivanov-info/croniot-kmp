import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "https://192.168.50.163:8443",
        changeOrigin: true,
        secure: false, // self-signed cert in dev
      },
    },
  },
});
