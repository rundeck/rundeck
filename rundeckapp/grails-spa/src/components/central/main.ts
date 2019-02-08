import {getRundeckContext, getSynchronizerToken, RundeckBrowser} from '@rundeck/ui-trellis'


const context = getRundeckContext()
const token = getSynchronizerToken()

context.rundeckClient = new RundeckBrowser(token.TOKEN, token.URI, context.rdBase)