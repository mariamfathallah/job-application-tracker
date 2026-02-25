const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function getToken(){
    return localStorage.getItem("token");
}

export function setToken(token){
    localStorage.setItem("token", token);
}

export function clearToken(){
    localStorage.removeItem("token");
}

async function request(path, { method = "GET", body, auth = true } = {}){
    const headers = {};
    if (body) headers["Content-Type"]= "application/json";

    if (auth){
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;
    }

    const res = await fetch(`${BASE_URL}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });

    const text = await res.text();
    const data = (() => {
        try {
            return text ? JSON.parse(text) : null;
        } catch {
            return text ? { message: text } : null;
        }
    })();

    if (!res.ok){
        if (res.status === 401) {
            clearToken();
            //force redirect even if we are deep in the app
            window.location.href = "/login";
            return;
        }
        const message = data?.message || `${res.status} ${res.statusText}`;
        throw new Error(message);
    }
    return data;
}

export const api = {
    register: (payload) => request("/api/auth/register", { method: "POST", body: payload, auth:false }),
    login: (payload) => request("/api/auth/login", { method:  "POST", body: payload, auth: false }),

    listApplications: (params) => {
        const q = new URLSearchParams(params).toString();
        return request(`/api/applications?${q}`, { auth: true });
    },

    createApplication: (payload) => request("/api/applications", { method: "POST", body: payload, auth: true}),

    getApplication: (id) =>
        request(`/api/applications/${id}`, { auth: true }),

    updateApplication: (id, payload) =>
        request(`/api/applications/${id}`, { method: "PUT", body: payload, auth: true }),

    deleteApplication: (id) =>
        request(`/api/applications/${id}`, { method: "DELETE", auth: true }),

    updateApplicationStatus: (id, status) =>
        request(`/api/applications/${id}/status`, { method: "PATCH", body: { status }, auth: true }),
};