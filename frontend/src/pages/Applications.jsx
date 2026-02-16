import { useEffect, useState } from "react";
import { api, clearToken } from "../api";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";


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


    const [query, setQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");

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


    const items = data?.content ?? [];
    const q = query.trim().toLowerCase();

    const filtered = items.filter((a) => {
        const matchesQuery =
            !q ||
            (a.company || "").toLowerCase().includes(q) ||
            (a.position || "").toLowerCase().includes(q);

        const matchesStatus =
            statusFilter === "ALL" || a.status === statusFilter;

        return matchesQuery && matchesStatus;
    });

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
                        <div style={{
                            display: "flex",
                            gap: 10,
                            alignItems: "center",
                            marginBottom: 12,
                            flexWrap: "wrap"
                        }}>
                            <input
                                className="input"
                                style={{maxWidth: 320}}
                                placeholder="Search company or position…"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                            />

                            <select
                                className="select selectSm"
                                value={statusFilter}
                                onChange={(e) => setStatusFilter(e.target.value)}
                            >
                                <option value="ALL">All statuses</option>
                                {["APPLIED", "INTERVIEW", "OFFER", "REJECTED"].map((s) => (
                                    <option key={s} value={s}>{s}</option>
                                ))}
                            </select>

                            {(query || statusFilter !== "ALL") && (
                                <button
                                    className="btn btnGhost"
                                    onClick={() => {
                                        setQuery("");
                                        setStatusFilter("ALL");
                                    }}
                                >
                                    Clear
                                </button>
                            )}

                            <span className="smallMuted" style={{marginLeft: "auto"}}>
                                    Showing {filtered.length} / {items.length}
                            </span>
                        </div>

                        <div className="tableWrap">
                            <table className="table">
                                <thead>
                                <tr>
                                    <th>Company</th>
                                    <th>Position</th>
                                    <th>Status</th>
                                    <th>Date applied</th>
                                    <th>Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                {filtered.map((a) => (
                                    <tr key={a.id}>
                                        <td style={{fontWeight: 700}}>{a.company}</td>
                                        <td>{a.position}</td>
                                        <td>
                                            <select
                                                className="select selectSm"
                                                value={a.status}
                                                onChange={async (e) => {
                                                    const newStatus = e.target.value;
                                                    try {
                                                        await api.updateApplicationStatus(a.id, newStatus);
                                                        await load(page);
                                                    } catch (err) {
                                                        setErr(err.message);
                                                    }
                                                }}
                                            >
                                                {["APPLIED", "INTERVIEW", "OFFER", "REJECTED"].map(s => (
                                                    <option key={s} value={s}>{s}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td>{formatDate(a.dateApplied)}</td>
                                        <td>
                                            <div style={{display: "flex", gap: 8}}>
                                                <button className="btn"
                                                        onClick={() => nav(`/applications/${a.id}/edit`)}>
                                                    Edit
                                                </button>

                                                <button
                                                    className="btn btnDanger"
                                                    onClick={async () => {
                                                        const ok = confirm(`Delete ${a.company} - ${a.position}?`);
                                                        if (!ok) return;

                                                        try {
                                                            await api.deleteApplication(a.id);

                                                            const isLastItemOnPage = data.content.length === 1;
                                                            const nextPage = isLastItemOnPage && page > 0 ? page - 1 : page;

                                                            await load(nextPage);
                                                        } catch (e) {
                                                            setErr(e.message);
                                                        }
                                                    }}

                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}

                                {filtered.length === 0 && (
                                    <tr>
                                        <td colSpan="5" style={{color: "var(--muted)"}}>
                                            No results. Try changing your search or filters.
                                        </td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </div>

                        <div style={{display: "flex", gap: 10, alignItems: "center", marginTop: 14}}>
                            <button className="btn" disabled={page === 0} onClick={() => load(page - 1)}>
                                Prev
                            </button>
                            <button className="btn" disabled={data.last} onClick={() => load(page + 1)}>
                                Next
                            </button>
                            <span className="smallMuted" style={{marginLeft: 6}}>
                Page {data.page + 1} / {data.totalPages || 1}
              </span>
                        </div>
                    </div>
                </div>
            )}
        </AppShell>
    );
}