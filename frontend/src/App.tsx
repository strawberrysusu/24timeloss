import { BrowserRouter } from "react-router-dom";

import { AppContent } from "./app/AppContent";

export function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}
