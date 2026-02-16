import { useState } from "react";
import { api } from "../api";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";
import toast from "react-hot-toast";


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
            toast.success("Application created")
            nav("/applications");
        } catch (e) {
            toast.error(e.message);
            setErr(e.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <AppShell
            right={
                <button className="btn btnGhost" onClick={() => nav("/applications")}>
                    Back
                </button>
            }
        >
            <h1 className="pageTitle">New application</h1>
            <p className="pageSubtitle">Add a new job you applied to.</p>

            <div className="card" style={{ maxWidth: 720 }}>
                <div className="cardBody">
                    <form onSubmit={onSubmit} className="formGrid">
                        <div>
                            <div className="label">Company</div>
                            <input className="input" value={company} onChange={(e) => setCompany(e.target.value)} placeholder="e.g. Amazon" />
                        </div>

                        <div>
                            <div className="label">Position</div>
                            <input className="input" value={position} onChange={(e) => setPosition(e.target.value)} placeholder="e.g. Software Engineer" />
                        </div>

                        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                            <div>
                                <div className="label">Status</div>
                                <select className="select" value={status} onChange={(e) => setStatus(e.target.value)}>
                                    {STATUSES.map((s) => (
                                        <option key={s} value={s}>{s}</option>
                                    ))}
                                </select>
                            </div>

                            <div>
                                <div className="label">Date applied</div>
                                <input className="input" type="date" value={dateApplied} onChange={(e) => setDateApplied(e.target.value)} />
                            </div>
                        </div>

                        <div>
                            <div className="label">Notes</div>
                            <textarea className="textarea" value={notes} onChange={(e) => setNotes(e.target.value)} rows={4} placeholder="Interview details, links, contacts…" />
                        </div>

                        <div className="actionsRow">
                            <button type="button" className="btn btnGhost" onClick={() => nav("/applications")}>
                                Cancel
                            </button>
                            <button className="btn btnPrimary" disabled={loading}>
                                {loading ? "Creating…" : "Create"}
                            </button>
                        </div>
                    </form>

                    {err && <div className="error">{err}</div>}
                </div>
            </div>
        </AppShell>
    );
}