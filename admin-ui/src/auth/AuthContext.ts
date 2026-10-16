import { createContext } from "react";
import type { Tokens } from "../api/auth";

export interface AuthState {
  token: string | null;
  login: (tokens: Tokens) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthState | null>(null);
