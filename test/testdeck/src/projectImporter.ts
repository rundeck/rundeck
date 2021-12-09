import * as Path from 'path'

import { JobUuidOption } from 'ts-rundeck/dist/lib/models'

import chalk from 'chalk'
import {Rundeck} from 'ts-rundeck'
import * as TmpP from 'tmp'
import YAML from 'js-yaml'

import {exec} from './async/child-process'
import * as Tmp from './async/tmp'
import * as FS from './async/fs'

import {Key} from './interfaces/Key'

export enum KeyType {
    Public = 'publicKey',
    Private = 'privateKey',
    Password = 'password',
}

TmpP.setGracefulCleanup()

export class ProjectImporter {
    constructor(readonly repoPath: string, readonly projectName: string, readonly client: Rundeck) {}

    async importProject() {
        const dir = await Tmp.dir({prefix: 'demo_projects_'})
        const importFileName = Path.join(dir, `${this.projectName}.zip`)

        const projectDir = Path.join(this.repoPath, this.projectName)

        // console.log('Creating project archive...')
        // console.log(chalk.blue(this.projectName))
        await exec(`cd ${projectDir} && zip -r ${importFileName} *`)
       
        const projectList = await this.client.projectList()


        /* Create project in instance if it does not exist. */
        if (projectList.filter(p => p.name == this.projectName).length == 0) {
            // console.log('Creating project...')
            await this.client.projectCreate({name: this.projectName})
        }

        /**
         * Project import
         */
        // console.log('Importing project...')
        await this.client.projectArchiveImport(this.projectName, await FS.readFile(importFileName), {importConfig: true, importACL: true, jobUuidOption: JobUuidOption.Preserve})


        // console.log('Importing readme...')
        const readmePath = Path.join(projectDir, `rundeck-${this.projectName}/files/readme.md`)
        if (await FS.exists(readmePath)) {
            const readme = await FS.readFile(readmePath)
            await this.client.projectReadmePut(this.projectName, {contents: readme.toString()})
        }
        
        // console.log('Importing mod...')
        const motdPath = Path.join(projectDir, `rundeck-${this.projectName}/files/motd.md`)
        if (await FS.exists(motdPath)) {
            const motd = await FS.readFile(motdPath)
            await this.client.projectMotdPut(this.projectName, {contents: motd.toString()})
        }

        /**
         * Key import
         */
        const keyRepoPath = Path.join(projectDir, 'system/keys')
        if (await FS.exists(keyRepoPath)) {
            // console.log('Importing keys...')
            const keyFiles = (await exec(`find ${keyRepoPath} -type f`)).stdout.toString().trim().split('\n')

            const keys = [] as Key[]
            for (const file of keyFiles) {
                keys.push(
                    YAML.safeLoad((await FS.readFile(file)).toString())
                )
            }

            for (const key of keys) {
                // console.log(chalk.blue(key.path))

                let contentType = 'application/pgp-keys'
                
                switch(key.type) {
                    case 'password':
                        contentType = 'application/x-rundeck-data-password'
                }

                const [path, file] = [key.path.split('/').slice(1).join('/')!, key.value] as [string, string]
                const resp = await this.client.storageKeyCreate(path, file, {contentType})
                if (resp._response.status == 409)
                    await this.client.storageKeyUpdate(path, file, {contentType})
            }
        }

        /**
         * ACL import
         */
        const aclRepoPath = Path.join('projects', this.projectName, 'system/acls')
        if (await FS.exists(aclRepoPath)) {
            // console.log('Importing system ACLs...')
            const aclFiles = (await exec(`find ${aclRepoPath} -type f`)).stdout.toString().trim().split('\n')
            
            const aclList = await this.client.systemAclPolicyList()

            const aclNames = aclList.resources!.map(a => a.name)

            for (const file of aclFiles) {
                const aclName = Path.basename(file).split('.').slice(0, -1).join('.')
                const aclContents = await FS.readFile(file)
                // console.log(chalk.blue(aclName))
                if (aclNames.indexOf(aclName) < 0)
                    await this.client.systemAclPolicyCreate(aclName, {systemAclPolicyCreateRequest: {contents: aclContents.toString()}})
                else
                    await this.client.systemAclPolicyUpdate(aclName, {systemAclPolicyUpdateRequest: {contents: aclContents.toString()}})
            }
        }

        // console.log('\n\n       Fin        ')
    }
}