import {getRundeckContext} from '@rundeck/ui-trellis'

const context = getRundeckContext()

export const tourManifestUrl = `${context.rdBase}tour/listAll`
export const tourUrl = `${context.rdBase}tour/get/`

export default {
  tourManifestUrl,
  tourUrl
}
