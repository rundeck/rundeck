import {getRundeckContext, getSynchronizerToken, RundeckBrowser, EventBus } from '../../../library'

import { RootStore } from '../../../library/stores/RootStore'

const context = getRundeckContext()
const token = getSynchronizerToken()

context.rundeckClient = new RundeckBrowser(token.TOKEN, token.URI, context.rdBase)
context.eventBus = EventBus
context.rootStore = new RootStore(context.rundeckClient, context.appMeta)
