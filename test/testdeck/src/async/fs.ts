import * as FS from 'fs'
import * as util from 'util'

export const readdir = util.promisify(FS.readdir)

export const stat = util.promisify(FS.stat)

export const writeFile = util.promisify(FS.writeFile)

export const readFile = util.promisify(FS.readFile)

export const exists = util.promisify(FS.exists)