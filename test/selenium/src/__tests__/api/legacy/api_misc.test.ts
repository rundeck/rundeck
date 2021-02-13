import {ShimApiTests} from '@rundeck/testdeck/util/apiShim'

describe('Legacy API Project Tests', ()=> {
    [
        /^test-history/,
        /^test-invalid/,
        /^test-metrics/,
        /^test-require-version/,
        /^test-resource/,
        /^test-run/,
        /^test-storage/,
        /^test-v23/,
        /^test-workflow/,
    ].forEach(t => {
        ShimApiTests(t)
    })
})