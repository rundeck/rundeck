import {getRundeckContext} from '@rundeck/ui-trellis'

const context = getRundeckContext()

export const seRoot = `${context.rdBase}/job`

export default {
  seRoot
}
