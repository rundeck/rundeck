import {CreateTestContext} from '@rundeck/testdeck/test/api'

import '@rundeck/testdeck/test/rundeck'


/** Fetch context which will be initialized before the tests run */
let context = CreateTestContext({})

describe('Plugins API', () => {
    it('Does not return v40 fields on older versions', async () => {
        /* {
          iconUrl?: String
          providerMetadata?: any
        }
        */

        const {cluster} = context

        const resp = await cluster.client.apiRequest({
            method: 'GET',
            pathTemplate: 'api/39/plugin/list'
        });

        const found = (<Array<any>>resp.parsedBody).find(i => i.iconUrl || i.providerMetadata)
        expect(found).toBeFalsy()
    })

    it('Returns v40 fieldd', async () => {
        /* {
          iconUrl?: String
          providerMetadata?: any
        }
        */

        const {cluster} = context

        const resp = await cluster.client.apiRequest({
            method: 'GET',
            pathTemplate: 'api/40/plugin/list'
        });

        const found = (<Array<any>>resp.parsedBody).find(i => i.iconUrl || i.providerMetadata)
        expect(found).toBeTruthy()
    })
})