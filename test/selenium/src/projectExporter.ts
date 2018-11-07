import * as Path from 'path'
import * as util from 'util'

import chalk from 'chalk'
import Mkdirp from 'mkdirp'
import {Rundeck} from 'ts-rundeck'
import YAML from 'js-yaml'

import {exec} from 'async/child-process'
import * as Tmp from 'async/tmp'
import * as FS from 'async/fs'
import {readStream} from 'async/stream'

const MakeDirAsync = util.promisify(Mkdirp)

export enum KeyType {
    Public = 'publicKey',
    Private = 'privateKey',
    Password = 'password',
}

export class ProjectExporter {
    constructor(readonly repoPath: string, readonly projectName: string, readonly client: Rundeck) {}


    async exportProject() {
        const dir = await Tmp.dir({prefix: 'demo_projects_'})
        const exportFileName = Path.join(dir, this.projectName)

        const projectDir = Path.join(this.repoPath, this.projectName)

        /**
         * Project export
         */
        console.log('Exporting project...')
        console.log(chalk.blue(this.projectName))

        const exportResp = await this.client.projectArchiveExportSync(this.projectName)
        let projectArchive: Buffer
        if (exportResp.readableStreamBody)
            projectArchive = await readStream(exportResp.readableStreamBody)
        else
            throw new Error('Stream not readable')

        await FS.writeFile(exportFileName, projectArchive)

        console.log('Exploding export into git repo...')
        // Clear out existing exported data so we sync removed resources
        await exec(`rm -r ${projectDir}/rundeck-* || true`)
        await exec(`unzip -oq ${exportFileName} -d ${projectDir}/ -x '*/reports/*' -x '*/executions/*'`)

        /**
         * Public key export
         */
        // console.log('Exporting public keys..')
        // let keyList = [] as string[]
        // try {
        //     keyList = (await exec(`rd keys list --path ${this.projectName}`)).stdout.toString().split('\n').filter(line => line.length > 0)
        // } catch (e) {
        //     if (! e.stderr.toString().includes('404 Not Found'))
        //         throw e
        // }

        // const keys = keyList
        //     .filter( k => !k.endsWith('/'))
        //     .map( k => {
        //         const kParts = k.split(' ')
        //         const keyType = kParts[1].substr(1,kParts[1].length - 2)
        //         return {path: kParts[0], type: keyType, value: ''}
        //     })
        
        // const pubKeys = keys.filter(k => k.type == KeyType.Public)
        // for (let key of pubKeys) {
        //     key.value = (await exec(`rd keys get --path ${key.path}`)).stdout.toString()
        // }
        
        // for (let key of pubKeys) {
        //     console.log(chalk.blue(key.path))
        //     const keyRelPath = key.path.split('/').slice(2).join()
        //     const KeyRepoPath = Path.join('projects', this.projectName, 'system/keys', `${keyRelPath}.yaml`)
        //     await MakeDirAsync(Path.dirname(KeyRepoPath))
        //     await FS.writeFile(KeyRepoPath, YAML.dump(key))
        // }

        /**
         * System ACL Export
         */
        // console.log('Exporting ACLs...')
        // const aclRepoPath = Path.join('projects', this.projectName, 'system/acls')
        // await MakeDirAsync(aclRepoPath)
        // const aclList = JSON.parse((await exec(`RD_FORMAT=json rd system acls list`)).stdout.toString()) as string[]

        // const projectAcls = aclList.filter(a => 
        //     a.toLowerCase().startsWith(this.projectName.toLocaleLowerCase())
        // )

        // for (const acl of projectAcls) {
        //     console.log(chalk.blue(acl))
        //     const aclRepoFile = Path.join(aclRepoPath, `${acl}.yaml`)
        //     const aclContent = (await exec(`rd system acls get --name ${acl}`)).stdout
        //     await FS.writeFile(aclRepoFile, aclContent)
        // }

        // console.log('\n\n       Fin        ')
    }
}