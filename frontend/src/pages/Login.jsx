import {useEffect, useState} from "react";
import {api, getToken, setToken} from "../api";
import { useNavigate, Link } from "react-router-dom";
import AppShell from "../components/AppShell";

export default function Login() {
    const nav = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [err, setErr] = useState("");
    const [loading, setLoading] = useState(false);

    async function onSubmit(e) {
        e.preventDefault();
        setErr("");
        setLoading(true);
        try {
            const res = await api.login({ email, password });
            setToken(res.token);
            nav("/applications");
        } catch (e) {
            setErr(e.message);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        if (getToken()) nav("/applications");
    }, [nav]);

    return (
        <AppShell>
            <div className="authShell">
                <div className="card authCard">
                    <div className="cardBody">
                        <h1 className="pageTitle">Login</h1>
                        <p className="pageSubtitle">Welcome back. Track your applications in one place.</p>

                        <form onSubmit={onSubmit} className="formGrid">
                            <div>
                                <div className="label">Email</div>
                                <input
                                    className="input"
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="you@email.com"
                                    autoComplete="email"
                                />
                            </div>

                            <div>
                                <div className="label">Password</div>
                                <input
                                    className="input"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="••••••••"
                                    autoComplete="current-password"
                                />
                            </div>

                            <div className="actionsRow">
                                <button className="btn btnPrimary" disabled={loading}>
                                    {loading ? "Signing in…" : "Login"}
                                </button>
                            </div>
                        </form>

                        {err && <div className="error">{err}</div>}

                        <p className="smallMuted" style={{ marginTop: 14 }}>
                            No account? <Link to="/register">Create one</Link>
                        </p>
                    </div>
                </div>
            </div>
        </AppShell>
    );
}