import { useState } from "react";
import { api, setToken } from "../api";
import { useNavigate, Link } from "react-router-dom";

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
        <div style={{ maxWidth: 420, margin: "40px auto" }}>
            <h1>Register</h1>
            <form onSubmit={onSubmit}>
                <label htmlFor="displayName">Name</label>
                <input id="displayName"
                       name="displayName"
                       value={displayName}
                       onChange={(e) => setDisplayName(e.target.value)}
                       style={{ width: "100%" }} />
                <label htmlFor="email">Email</label>
                <input
                    id="email"
                    name="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    style={{ width: "100%" }}
                />

                <label htmlFor="password">Password</label>
                <input
                    id="password"
                    name="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={{ width: "100%" }}
                />
                <button disabled={loading} style={{marginTop: 12}}>
                    {loading ? "..." : "Create account"}
                </button>
            </form>
            {err && <p style={{color: "crimson"}}>{err}</p>}
            <p style={{marginTop: 12}}>
                Already have an account? <Link to="/login">Login</Link>
            </p>
        </div>
    );
}
