import {getRundeckContext} from '../../../library'

const context = getRundeckContext()

export const tourManifestUrl = `${context.rdBase}tour/listAll`
export const tourUrl = `${context.rdBase}tour/get/`

export default {
  tourManifestUrl,
  tourUrl
}
