export default function AppShell({ right, children }) {
    return (
        <>
            <div className="topbar">
                <div className="topbarInner">
                    <div className="brand">
                        <span className="logoDot"/>
                        Job Application Tracker
                    </div>
                    <div style={{display: "flex", gap: 10, alignItems: "center"}}>
                        {right}
                    </div>
                </div>
            </div>

            <main className="page">
                <div className="pageInner">{children}</div>
            </main>
        </>
    );
}
