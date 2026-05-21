import { STATUSES } from '../constants/statuses'

describe('STATUSES', () => {
    it('contains exactly the four valid statuses in order', () => {
        expect(STATUSES).toEqual(['APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'])
    })

    it('has exactly 4 entries', () => {
        expect(STATUSES).toHaveLength(4)
    })

    it('every entry is uppercase', () => {
        STATUSES.forEach(s => expect(s).toBe(s.toUpperCase()))
    })
})