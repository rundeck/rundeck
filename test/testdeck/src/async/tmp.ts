import * as util from 'util'

import Tmp from 'tmp'

export const tmp =  util.promisify(Tmp.file)

export function dir(options: Tmp.Options): Promise<string> {
    return new Promise( (res, rej) => {
        Tmp.dir(options, (err, path) => {
            if (err) rej(err)
            else res(path)
        })
    })
}