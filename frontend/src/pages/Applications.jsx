import { useEffect, useState } from "react";
import { api, clearToken } from "../api";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";


function StatusBadge({ status }) {
    const s = String(status || "").toLowerCase();
    return (
        <span className={`badge ${s}`}>
      <span className="badgeDot" />
            {status}
    </span>
    );
}

function formatDate(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "2-digit" });
}

export default function Applications() {
    const nav = useNavigate();
    const [data, setData] = useState(null);
    const [err, setErr] = useState("");
    const [page, setPage] = useState(0);

    async function load(p = 0) {
        setErr("");
        try {
            const res = await api.listApplications({ page: p, size: 5, sort: "dateApplied,desc" });
            setData(res);
            setPage(p);
        } catch (e) {
            setErr(e.message);
            if (e.message.includes("401")) {
                clearToken();
                nav("/login");
            }
        }
    }

    useEffect(() => {
        load(0);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <AppShell
            right={
                <>
                    <button className="btn btnPrimary" onClick={() => nav("/applications/new")}>
                        New application
                    </button>
                    <button
                        className="btn btnGhost"
                        onClick={() => {
                            clearToken();
                            nav("/login");
                        }}
                    >
                        Logout
                    </button>
                </>
            }
        >
            <h1 className="pageTitle">Applications</h1>
            <p className="pageSubtitle">Keep track of where you applied and what’s next.</p>

            {err && <div className="error">{err}</div>}

            {!data ? (
                <div className="card">
                    <div className="cardBody">Loading…</div>
                </div>
            ) : (
                <div className="card">
                    <div className="cardBody">
                        <div className="tableWrap">
                            <table className="table">
                                <thead>
                                <tr>
                                    <th>Company</th>
                                    <th>Position</th>
                                    <th>Status</th>
                                    <th>Date applied</th>
                                </tr>
                                </thead>
                                <tbody>
                                {data.content.map((a) => (
                                    <tr key={a.id}>
                                        <td style={{ fontWeight: 700 }}>{a.company}</td>
                                        <td>{a.position}</td>
                                        <td><StatusBadge status={a.status} /></td>
                                        <td>{formatDate(a.dateApplied)}</td>
                                    </tr>
                                ))}

                                {data.content.length === 0 && (
                                    <tr>
                                        <td colSpan="4" style={{ color: "var(--muted)" }}>
                                            No applications yet. Click “New application” to create one.
                                        </td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </div>

                        <div style={{ display: "flex", gap: 10, alignItems: "center", marginTop: 14 }}>
                            <button className="btn" disabled={page === 0} onClick={() => load(page - 1)}>
                                Prev
                            </button>
                            <button className="btn" disabled={data.last} onClick={() => load(page + 1)}>
                                Next
                            </button>
                            <span className="smallMuted" style={{ marginLeft: 6 }}>
                Page {data.page + 1} / {data.totalPages || 1}
              </span>
                        </div>
                    </div>
                </div>
            )}
        </AppShell>
    );
}