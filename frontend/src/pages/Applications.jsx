import { useEffect, useState } from "react";
import { api, clearToken } from "../api";
import { useNavigate } from "react-router-dom";

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
        <div style={{ maxWidth: 900, margin: "40px auto" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h1>Applications</h1>
                <div>
                    <button onClick={() => nav("/applications/new")}>New</button>{" "}
                    <button
                        onClick={() => {
                            clearToken();
                            nav("/login");
                        }}
                    >
                        Logout
                    </button>
                </div>
            </div>

            {err && <p style={{ color: "crimson" }}>{err}</p>}
            {!data ? (
                <p>Loading…</p>
            ) : (
                <>
                    <table width="100%" border="1" cellPadding="8" style={{ borderCollapse: "collapse" }}>
                        <thead>
                        <tr>
                            <th>Company</th>
                            <th>Position</th>
                            <th>Status</th>
                            <th>Date</th>
                        </tr>
                        </thead>
                        <tbody>
                        {data.content.map((a) => (
                            <tr key={a.id}>
                                <td>{a.company}</td>
                                <td>{a.position}</td>
                                <td>{a.status}</td>
                                <td>{a.dateApplied}</td>
                            </tr>
                        ))}
                        {data.content.length === 0 && (
                            <tr>
                                <td colSpan="4">No applications yet.</td>
                            </tr>
                        )}
                        </tbody>
                    </table>

                    <div style={{ marginTop: 12 }}>
                        <button disabled={page === 0} onClick={() => load(page - 1)}>
                            Prev
                        </button>{" "}
                        <button disabled={data.last} onClick={() => load(page + 1)}>
                            Next
                        </button>
                        <span style={{ marginLeft: 12 }}>
              Page {data.page + 1} / {data.totalPages || 1}
            </span>
                    </div>
                </>
            )}
        </div>
    );
}
