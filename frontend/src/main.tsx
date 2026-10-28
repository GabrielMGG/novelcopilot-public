import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import "./i18n";
import { BrowserRouter } from "react-router-dom";
import './index.css'
import "./styles/workspace.css";
import App from './app/App.tsx'
import { AuthProvider } from "./contexts/AuthProvider";
import { CreditProvider } from "./contexts/CreditProvider";


createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider>
      <CreditProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </CreditProvider>
    </AuthProvider>
  </StrictMode>
);
