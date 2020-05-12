import {ShimApiTests} from '@rundeck/testdeck/util/apiShim'

describe('Legacy API Execution Tests', ()=> {
    ShimApiTests(/^test-executions?/)
})