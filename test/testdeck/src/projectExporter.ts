import * as Path from 'path'
import * as util from 'util'

import chalk from 'chalk'
import Mkdirp from 'mkdirp'
import {Rundeck} from '@rundeck/client'
import YAML from 'js-yaml'

import {exec} from './async/child-process'
import * as Tmp from './async/tmp'
import * as FS from './async/fs'
import {readStream} from './async/stream'

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

        const exportResp = await this.client.projectArchiveExportSync(this.projectName, {exportExecutions: false})
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
        const keyList = await this.client.storageKeyGetMetadata(this.projectName)

        if (keyList._response.status == 200) {
            const keys = keyList.resources!
                .filter( k => k.meta!.rundeckKeyType == 'public')
                .map( k => {
                    return {path: k.path, type: k.meta!.rundeckKeyType, value: ''}
                })

            for (let key of keys) {
                const keyPath = key.path!.split('/').slice(1).join('/')
                const resp = await this.client.storageKeyGetMaterial(keyPath, {customHeaders: {accept: '*/*'}})
                
                if (resp.readableStreamBody)
                    key.value = (await readStream(resp.readableStreamBody)).toString()
                else
                    throw new Error('Stream not readable')

                const keyRelPath = keyPath.split('/').slice(1).join()
                const KeyRepoPath = Path.join(projectDir, 'system/keys', `${keyRelPath}.yaml`)
                await MakeDirAsync(Path.dirname(KeyRepoPath))
                await FS.writeFile(KeyRepoPath, YAML.dump(key))
            }
        }

        /**
         * System ACL Export
         */
        const aclRepoPath = Path.join(projectDir, 'system/acls')
        await MakeDirAsync(aclRepoPath)

        const acls = await this.client.systemAclPolicyList()

        const projectAcls = acls.resources!
            .filter( a => a.name!.toLowerCase().startsWith(this.projectName.toLowerCase()))

        for (const acl of projectAcls) {
            console.log(chalk.blue(acl.name!))
            const aclRepoFile = Path.join(aclRepoPath, `${acl.name}.yaml`)
            const aclResp = await this.client.systemAclPolicyGet(acl.name!)
            const aclContent = aclResp.contents
            await FS.writeFile(aclRepoFile, aclContent)
        }

        // console.log('\n\n       Fin        ')
    }
}