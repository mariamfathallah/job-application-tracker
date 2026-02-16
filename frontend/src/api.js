const BASE_URL = import.meta.env.VITE_API_BASE_URL;

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
    const headers = { "Content-Type": "application/json" };

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
    const data = text ? JSON.parse(text) : null;

    if (!res.ok){
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
};