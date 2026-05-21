import { useEffect, useState } from "react";
import { api } from "../api";
import { useNavigate, useParams } from "react-router-dom";
import AppShell from "../components/AppShell";

function formatDate(iso) {
    if (!iso) return "";
    return new Date(iso).toLocaleDateString(undefined, {
        year: "numeric", month: "short", day: "2-digit",
    });
}

export default function ApplicationDetail() {
    const nav = useNavigate();
    const { id } = useParams();
    const [app, setApp] = useState(null);
    const [err, setErr] = useState("");

    useEffect(() => {
        api.getApplication(id)
            .then(setApp)
            .catch((e) => setErr(e.message));
    }, [id]);

    return (
        <AppShell
            right={
                <>
                    <button className="btn btnGhost" onClick={() => nav("/applications")}>
                        Back
                    </button>
                    {app && (
                        <button className="btn btnPrimary" onClick={() => nav(`/applications/${id}/edit`)}>
                            Edit
                        </button>
                    )}
                </>
            }
        >
            <h1 className="pageTitle">Application</h1>
            <p className="pageSubtitle">{app ? `${app.company} — ${app.position}` : " "}</p>

            {err && <div className="error">{err}</div>}

            {!app && !err && <div className="smallMuted">Loading…</div>}

            {app && (
                <div className="card" style={{ maxWidth: 720 }}>
                    <div className="cardBody">
                        <div style={{ display: "grid", gap: 20 }}>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                                <div>
                                    <div className="label">Company</div>
                                    <div style={{ fontWeight: 700 }}>{app.company}</div>
                                </div>
                                <div>
                                    <div className="label">Position</div>
                                    <div>{app.position}</div>
                                </div>
                            </div>

                            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                                <div>
                                    <div className="label">Status</div>
                                    <div>{app.status}</div>
                                </div>
                                <div>
                                    <div className="label">Date applied</div>
                                    <div>{formatDate(app.dateApplied)}</div>
                                </div>
                            </div>

                            <div>
                                <div className="label">Notes</div>
                                {app.notes
                                    ? <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{app.notes}</p>
                                    : <span className="smallMuted">No notes added.</span>
                                }
                            </div>

                        </div>
                    </div>
                </div>
            )}
        </AppShell>
    );
}