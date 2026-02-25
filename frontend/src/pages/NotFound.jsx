import {useNavigate} from "react-router-dom";
import AppShell from "../components/AppShell.jsx";

export default function NotFound(){
    const nav = useNavigate();
    return (
        <AppShell>
            <h1 className="pageTitle">404</h1>
            <p className="pageSubtitle">This page doesn't exist.</p>
            <button className="btn btnPrimary" onClick={() => nav("/applications")}>
                Go to Applications
            </button>
        </AppShell>
    );
}