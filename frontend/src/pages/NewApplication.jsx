import { useState } from "react";
import { api } from "../api";
import { useNavigate } from "react-router-dom";

const STATUSES = ["APPLIED", "INTERVIEW", "OFFER", "REJECTED"];

export default function NewApplication() {
    const nav = useNavigate();
    const [company, setCompany] = useState("");
    const [position, setPosition] = useState("");
    const [status, setStatus] = useState("APPLIED");
    const [dateApplied, setDateApplied] = useState(new Date().toISOString().slice(0, 10));
    const [notes, setNotes] = useState("");
    const [err, setErr] = useState("");
    const [loading, setLoading] = useState(false);

    async function onSubmit(e) {
        e.preventDefault();
        setErr("");
        setLoading(true);
        try {
            await api.createApplication({ company, position, status, dateApplied, notes });
            nav("/applications");
        } catch (e) {
            setErr(e.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div style={{ maxWidth: 520, margin: "40px auto" }}>
            <h1>New Application</h1>
            <form onSubmit={onSubmit}>
                <label htmlFor="company">Company</label>
                <input
                    id="company"
                    name="company"
                    type="company"
                    value={company}
                    onChange={(e) => setCompany(e.target.value)} style={{ width: "100%" }} />

                <label htmlFor="position">Position</label>
                <input
                    id="position"
                    name="position"
                    type="position"
                    value={position}
                    onChange={(e) => setPosition(e.target.value)} style={{ width: "100%" }} />

                <label htmlFor="status">Status</label>
                <select
                    id="status"
                    name="status"
                    value={status}
                    onChange={(e) => setStatus(e.target.value)} style={{ width: "100%" }}>
                    {STATUSES.map((s) => (
                        <option key={s} value={s}>
                            {s}
                        </option>
                    ))}
                </select>

                <label htmlFor="dateApplied">Date Applied</label>
                <input
                    id="date"
                    name="date"
                    type="date"
                    value={dateApplied}
                    onChange={(e) => setDateApplied(e.target.value)} style={{ width: "100%" }} />

                <label htmlFor="notes">Notes</label>
                <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={4} style={{ width: "100%" }} />

                <button disabled={loading} style={{ marginTop: 12 }}>
                    {loading ? "..." : "Create"}
                </button>
            </form>

            {err && <p style={{ color: "crimson" }}>{err}</p>}

            <button style={{ marginTop: 12 }} onClick={() => nav("/applications")}>
                Back
            </button>
        </div>
    );
}
