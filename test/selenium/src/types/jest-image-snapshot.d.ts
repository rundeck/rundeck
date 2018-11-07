declare module 'jest-image-snapshot' {
    function toMatchImageSnapshot(config: any): { message(): string, pass: boolean }
}


declare module jest {
    interface Matchers<R> {
        toMatchImageSnapshot(config: any): R;
    }
    interface Expect {
        getState(): {currentTestName: string}
    }
}


