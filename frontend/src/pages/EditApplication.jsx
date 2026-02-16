import { useEffect, useState } from "react";
import { api } from "../api";
import { useNavigate, useParams } from "react-router-dom";
import AppShell from "../components/AppShell";

const STATUSES = ["APPLIED", "INTERVIEW", "OFFER", "REJECTED"];

export default function EditApplication() {
    const nav = useNavigate();
    const { id } = useParams();

    const [company, setCompany] = useState("");
    const [position, setPosition] = useState("");
    const [status, setStatus] = useState("APPLIED");
    const [dateApplied, setDateApplied] = useState("");
    const [notes, setNotes] = useState("");

    const [err, setErr] = useState("");
    const [loading, setLoading] = useState(false);
    const [loadingData, setLoadingData] = useState(true);

    useEffect(() => {
        (async () => {
            setErr("");
            try {
                const a = await api.getApplication(id);
                const d = a.dateApplied ? String(a.dateApplied).slice(0, 10) : new Date().toISOString().slice(0, 10);
                setCompany(a.company || "");
                setPosition(a.position || "");
                setStatus(a.status || "APPLIED");
                setDateApplied(d);
                setNotes(a.notes || "");
            } catch (e) {
                setErr(e.message);
                if (e.message.includes("401")) {
                    nav("/login");
                }
            } finally {
                setLoadingData(false);
            }
        })();
    }, [id]);

    async function onSubmit(e) {
        e.preventDefault();
        setErr("");
        setLoading(true);
        try {
            await api.updateApplication(id, { company, position, status, dateApplied, notes });
            nav("/applications");
        } catch (e) {
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
            <h1 className="pageTitle">Edit application</h1>
            <p className="pageSubtitle">Update details for this job application.</p>

            <div className="card" style={{ maxWidth: 720 }}>
                <div className="cardBody">
                    {loadingData ? (
                        <div className="smallMuted">Loading…</div>
                    ) : (
                        <form onSubmit={onSubmit} className="formGrid">
                            <div>
                                <div className="label">Company</div>
                                <input className="input" value={company} onChange={(e) => setCompany(e.target.value)} />
                            </div>

                            <div>
                                <div className="label">Position</div>
                                <input className="input" value={position} onChange={(e) => setPosition(e.target.value)} />
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
                                <textarea className="textarea" rows={4} value={notes} onChange={(e) => setNotes(e.target.value)} />
                            </div>

                            <div className="actionsRow">
                                <button type="button" className="btn btnGhost" onClick={() => nav("/applications")}>
                                    Cancel
                                </button>
                                <button className="btn btnPrimary" disabled={loading}>
                                    {loading ? "Saving…" : "Save changes"}
                                </button>
                            </div>
                        </form>
                    )}

                    {err && <div className="error">{err}</div>}
                </div>
            </div>
        </AppShell>
    );
}
