import { useEffect, useState } from "react";
import { api, clearToken } from "../api";
import { useNavigate } from "react-router-dom";
import AppShell from "../components/AppShell";
import toast from "react-hot-toast";


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

    const [menuOpenId, setMenuOpenId] = useState(null);
    const [deleteTarget, setDeleteTarget] = useState(null);

    const [statusBusyId, setStatusBusyId] = useState(null);
    const [pageLoading, setPageLoading] = useState(false);

    const [deleteBusy, setDeleteBusy] = useState(false);

    async function load(p = 0) {
        setErr("");
        setPageLoading(true);
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
        } finally {
            setPageLoading(false);
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

    function updateRow(id, patch) {
        setData((prev) => {
            if (!prev) return prev;
            return {
                ...prev,
                content: prev.content.map((x) => (x.id === id ? { ...x, ...patch } : x)),
            };
        });
    }

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
                    <div className="cardBody">
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
                                {[...Array(5)].map((_, i) => (
                                    <tr key={i}>
                                        <td><div className="sk skText" /></td>
                                        <td><div className="sk skText" /></td>
                                        <td><div className="sk skPill" /></td>
                                        <td><div className="sk skText" /></td>
                                        <td><div className="sk skBtn" /></td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
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
                                    <tr key={a.id}
                                        onClick={() => nav(`/applications/${a.id}/edit`)}
                                        style={{ cursor: "pointer" }}
                                    >
                                        <td style={{fontWeight: 700}}>{a.company}</td>
                                        <td>{a.position}</td>
                                        <td>
                                            <select
                                                className="select selectSm"
                                                value={a.status}
                                                disabled={statusBusyId === a.id}
                                                onClick={(e) => e.stopPropagation()}
                                                onChange={async (e) => {
                                                    const newStatus = e.target.value;
                                                    const oldStatus = a.status;

                                                    //optimistic update
                                                    updateRow(a.id, { status: newStatus });
                                                    setStatusBusyId(a.id);

                                                    try {
                                                        await api.updateApplicationStatus(a.id, newStatus);
                                                        toast.success("Status updated");
                                                        await load(page);
                                                    } catch (err) {
                                                        updateRow(a.id, { status: oldStatus });
                                                        toast.error(err.message);
                                                        setErr(err.message);
                                                    } finally {
                                                        setStatusBusyId(null);
                                                    }
                                                }}
                                            >
                                                {["APPLIED", "INTERVIEW", "OFFER", "REJECTED"].map(s => (
                                                    <option key={s} value={s}>{s}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td>{formatDate(a.dateApplied)}</td>
                                        <td onClick={(e)=> e.stopPropagation()}>
                                            <div style={{ position: "relative" }}>
                                                <button className="btn btnGhost"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            setMenuOpenId(menuOpenId === a.id ? null : a.id);
                                                        }}
                                                >
                                                    ...
                                                </button>

                                                {menuOpenId === a.id && (
                                                    <div className="menu">
                                                        <button
                                                            className="menuItem"
                                                            onClick={() => {
                                                                nav(`/applications/${a.id}/edit`);
                                                            }}
                                                        >
                                                            Edit
                                                        </button>

                                                        <button
                                                            className="menuItem danger"
                                                            onClick={() => {
                                                                setMenuOpenId(null);
                                                                setDeleteTarget(a);
                                                            }}
                                                        >
                                                            Delete
                                                        </button>
                                                    </div>
                                                )}
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
                            <button className="btn" disabled={pageLoading || page === 0} onClick={() => load(page - 1)}>
                                {pageLoading ? "..." : "Prev"}
                            </button>
                            <button className="btn" disabled={pageLoading || data.last} onClick={() => load(page + 1)}>
                                {pageLoading ? "..." : "Next"}
                            </button>
                            <span className="smallMuted" style={{marginLeft: 6}}>
                                Page {data.page + 1} / {data.totalPages || 1}
                            </span>
                        </div>
                    </div>
                </div>
            )}
            {deleteTarget && (
                <div className="modalOverlay">
                    <div className="modalCard">
                        <h3>Delete application?</h3>
                        <p className="smallMuted">
                            {deleteTarget.company} – {deleteTarget.position}
                        </p>

                        <div className="actionsRow">
                            <button
                                className="btn btnGhost"
                                onClick={() => setDeleteTarget(null)}
                            >
                                Cancel
                            </button>

                            <button
                                className="btn btnDanger"
                                disabled={deleteBusy}
                                onClick={async () => {
                                    try {
                                        setDeleteBusy(true);
                                        await api.deleteApplication(deleteTarget.id);
                                        toast.success("Application deleted");

                                        const isLastItemOnPage = data.content.length === 1;
                                        const nextPage = isLastItemOnPage && page > 0 ? page - 1 : page;

                                        setDeleteTarget(null);
                                        await load(nextPage);
                                    } catch (e) {
                                        toast.error(e.message);
                                    } finally {
                                        setDeleteBusy(false);
                                    }
                                }}
                            >
                                {deleteBusy ? "Deleting…" : "Delete"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </AppShell>
    );
}