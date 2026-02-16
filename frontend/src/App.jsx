import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Applications from "./pages/Applications";
import NewApplication from "./pages/NewApplication";
import { getToken } from "./api";
import EditApplication from "./pages/EditApplication.jsx";

function RequireAuth({ children }) {
    const token = getToken();
    return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Navigate to="/applications" replace />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                <Route
                    path="/applications"
                    element={
                        <RequireAuth>
                            <Applications />
                        </RequireAuth>
                    }
                />
                <Route
                    path="/applications/new"
                    element={
                        <RequireAuth>
                            <NewApplication />
                        </RequireAuth>
                    }
                />
                <Route
                    path="/applications/:id/edit"
                    element={
                        <RequireAuth>
                            <EditApplication />
                        </RequireAuth>
                    }
                />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
