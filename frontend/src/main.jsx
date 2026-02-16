import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import "./styles.css"

import { Toaster } from "react-hot-toast";

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
      <Toaster
          position="top-right"
          toastOptions={{
              duration: 2800,
              style: {
                  background: "rgba(20, 24, 38, 0.92)",
                  color: "rgba(255,255,255,0.92)",
                  border: "1px solid rgba(255,255,255,0.12)",
                  borderRadius: "12px",
              },
          }}
      />
  </StrictMode>,
)
