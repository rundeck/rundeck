import ParseBool from '../../src/utilities/ParseBoolean'

describe("ParseBoolean", () => {
    it('parses strings', () => {
        [
            'true',
            'True',
            '1'
        ].forEach(value => {
            expect(ParseBool(value)).toEqual(true)
        });

        [
            'false',
            'False',
            '0',
        ].forEach(value => {
            expect(ParseBool(value)).toEqual(false)
        });
    })

    it('parses bools', () => {
        expect(ParseBool(true)).toEqual(true)
        expect(ParseBool(false)).toEqual(false)
    })
})