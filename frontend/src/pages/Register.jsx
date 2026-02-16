import { useState } from "react";
import { api, setToken } from "../api";
import { useNavigate, Link } from "react-router-dom";
import AppShell from "../components/AppShell";

export default function Register() {
    const nav = useNavigate();
    const [displayName, setDisplayName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [err, setErr] = useState("");
    const [loading, setLoading] = useState(false);

    async function onSubmit(e) {
        e.preventDefault();
        setErr("");
        setLoading(true);
        try {
            const res = await api.register({ email, password, displayName });
            setToken(res.token);
            nav("/applications");
        } catch (e) {
            setErr(e.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <AppShell>
            <div className="authShell">
                <div className="card authCard">
                    <div className="cardBody">
                        <h1 className="pageTitle">Create account</h1>
                        <p className="pageSubtitle">Start tracking your job search in minutes.</p>

                        <form onSubmit={onSubmit} className="formGrid">
                            <div>
                                <div className="label">Name</div>
                                <input
                                    className="input"
                                    value={displayName}
                                    onChange={(e) => setDisplayName(e.target.value)}
                                    placeholder="Your name"
                                    autoComplete="name"
                                />
                            </div>

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
                                    placeholder="Create a password"
                                    autoComplete="new-password"
                                />
                            </div>

                            <div className="actionsRow">
                                <button className="btn btnPrimary" disabled={loading}>
                                    {loading ? "Creating…" : "Create account"}
                                </button>
                            </div>
                        </form>

                        {err && <div className="error">{err}</div>}

                        <p className="smallMuted" style={{ marginTop: 14 }}>
                            Already have an account? <Link to="/login">Login</Link>
                        </p>
                    </div>
                </div>
            </div>
        </AppShell>
    );
}