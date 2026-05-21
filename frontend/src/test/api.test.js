import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest'
import { getToken, setToken, clearToken, api } from '../api'

// ─── token helpers ────────────────────────────────────────────────────────────

describe('token helpers', () => {
    beforeEach(() => localStorage.clear())

    it('getToken returns null when nothing is stored', () => {
        expect(getToken()).toBeNull()
    })

    it('setToken stores a value that getToken retrieves', () => {
        setToken('abc123')
        expect(getToken()).toBe('abc123')
    })

    it('clearToken removes the stored token', () => {
        setToken('abc123')
        clearToken()
        expect(getToken()).toBeNull()
    })
})

// ─── updateApplicationStatus ──────────────────────────────────────────────────

describe('api.updateApplicationStatus', () => {
    beforeEach(() => {
        localStorage.clear()
        setToken('my-jwt')
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: true,
            status: 200,
            text: () => Promise.resolve(JSON.stringify({ id: 1, status: 'INTERVIEW' })),
        }))
    })

    afterEach(() => vi.unstubAllGlobals())

    it('calls PATCH /api/applications/:id/status', async () => {
        await api.updateApplicationStatus(42, 'INTERVIEW')

        expect(fetch).toHaveBeenCalledOnce()
        const [url, opts] = fetch.mock.calls[0]
        expect(url).toContain('/api/applications/42/status')
        expect(opts.method).toBe('PATCH')
    })

    it('sends { status } as the request body', async () => {
        await api.updateApplicationStatus(42, 'OFFER')

        const [, opts] = fetch.mock.calls[0]
        expect(JSON.parse(opts.body)).toEqual({ status: 'OFFER' })
    })

    it('attaches the Authorization header when a token is stored', async () => {
        await api.updateApplicationStatus(42, 'REJECTED')

        const [, opts] = fetch.mock.calls[0]
        expect(opts.headers.Authorization).toBe('Bearer my-jwt')
    })
})

// ─── 401 auto-logout ──────────────────────────────────────────────────────────

describe('401 handling', () => {
    beforeEach(() => {
        setToken('stale-token')
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 401,
            statusText: 'Unauthorized',
            text: () => Promise.resolve(''),
        }))
        vi.stubGlobal('location', { href: '' })
    })

    afterEach(() => vi.unstubAllGlobals())

    it('clears the stored token on 401', async () => {
        await api.listApplications({})
        expect(getToken()).toBeNull()
    })

    it('redirects to /login on 401', async () => {
        await api.listApplications({})
        expect(window.location.href).toBe('/login')
    })
})

// ─── error propagation ────────────────────────────────────────────────────────

describe('error propagation', () => {
    beforeEach(() => {
        setToken('jwt')
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 400,
            statusText: 'Bad Request',
            text: () => Promise.resolve(JSON.stringify({ message: 'Company is required' })),
        }))
    })

    afterEach(() => vi.unstubAllGlobals())

    it('throws with the server error message on a non-ok response', async () => {
        await expect(api.createApplication({})).rejects.toThrow('Company is required')
    })

    it('falls back to status text when server returns no message', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
            ok: false,
            status: 500,
            statusText: 'Internal Server Error',
            text: () => Promise.resolve(''),
        }))
        await expect(api.createApplication({})).rejects.toThrow('500 Internal Server Error')
    })
})