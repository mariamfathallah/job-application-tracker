import { useState } from "react";
import { api, setToken } from "../api";
import { useNavigate, Link } from "react-router-dom";

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

    return (
        <div style={{ maxWidth: 420, margin: "40px auto" }}>
            <h1>Login</h1>
            <form onSubmit={onSubmit}>
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
                    {loading ? "..." : "Login"}
                </button>
            </form>
            {err && <p style={{color: "crimson"}}>{err}</p>}
            <p style={{marginTop: 12}}>
                No account? <Link to="/register">Register</Link>
            </p>
        </div>
    );
}
