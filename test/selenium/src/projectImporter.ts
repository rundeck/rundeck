import * as Path from 'path'
import * as util from 'util'

import chalk from 'chalk'
import {Rundeck} from 'ts-rundeck'

import {exec} from 'async/child-process'
import * as Tmp from 'async/tmp'
import * as FS from 'async/fs'
import { JobUuidOption } from 'ts-rundeck/dist/lib/models'

export enum KeyType {
    Public = 'publicKey',
    Private = 'privateKey',
    Password = 'password',
}

export class ProjectImporter {
    constructor(readonly repoPath: string, readonly projectName: string, readonly client: Rundeck) {}

    async importProject() {
        const dir = await Tmp.dir({prefix: 'demo_projects_'})
        const importFileName = Path.join(dir, `${this.projectName}.zip`)

        const projectDir = Path.join(this.repoPath, this.projectName)

        console.log('Creating project archive...')
        console.log(chalk.blue(this.projectName))
        await exec(`cd ${projectDir} && zip -r ${importFileName} *`)
       
        const projectList = await this.client.projectList()


        /* Create project in instance if it does not exist. */
        if (projectList.filter(p => p.name == this.projectName).length == 0) {
            console.log('Creating project...')
            await this.client.projectCreate({name: this.projectName})
        }

        /**
         * Project import
         */
        console.log('Importing project...')
        await this.client.projectArchiveImport(this.projectName, await FS.readFile(importFileName), {importConfig: true, jobUuidOption: JobUuidOption.Preserve})
        // await exec(`rd projects archives import --file ${importFileName} --project ${this.projectName}`)

        /**
         * MOTD Import
         */
        // console.log('Importing MOTD and REAMDE...')
        // const motdFile = `./projects/${this.projectName}/rundeck-${this.projectName}/files/motd.md`
        // const readmeFile = `./projects/${this.projectName}/rundeck-${this.projectName}/files/readme.md`

        // if (await FS.exists(motdFile)) {
        //     await exec(`rd projects readme put --file ${motdFile} --motd --project ${this.projectName}`)
        // }

        // if (await FS.exists(readmeFile)) {
        //     await exec(`rd projects readme put --file ${readmeFile} --project ${this.projectName}`)
        // }

        /**
         * Key import
         */
        // const keyRepoPath = Path.join('projects', this.projectName, 'system/keys')
        // if (await FS.exists(keyRepoPath)) {
        //     console.log('Importing keys...')
        //     const keyFiles = (await exec(`find projects/${this.projectName}/system/keys -type f`)).stdout.toString().trim().split('\n')

        //     const keys = [] as Key[]
        //     for (const file of keyFiles) {
        //         keys.push(
        //             YAML.safeLoad(await FS.readFile(file).toString())
        //         )
        //     }

        //     for (const key of keys) {
        //         console.log(chalk.blue(key.path))
        //         const tmpFile = TmpP.fileSync().name
        //         const commonArgs = ['--path', key.path, '--file', tmpFile, '--type', key.type]
        //         await FS.writeFile(tmpFile, key.value)
        //             let res = await spawn('rd', ['keys', 'create', ...commonArgs], {stdio: 'inherit'})
        //             if (res != 0) {
        //                 await spawn('rd', ['keys', 'update', ...commonArgs], {stdio: 'inherit'})
        //             }
        //     }
        // }

        /**
         * ACL import
         */
        // const aclRepoPath = Path.join('projects', this.projectName, 'system/acls')
        // if (existsSync(aclRepoPath)) {
        //     console.log('Importing system ACLs...')
        //     const aclFiles = (await exec(`find ./projects/${this.projectName}/system/acls -type f`)).stdout.toString().trim().split('\n')
            
        //     const aclList = JSON.parse((await exec(`RD_FORMAT=json rd system acls list`)).stdout.toString()) as string[]

        //     for (const file of aclFiles) {
        //         const aclName = Path.basename(file).split('.').slice(0, -1).join('.')
        //         console.log(chalk.blue(aclName))
        //         if (aclList.indexOf(aclName) < 0)
        //             await exec(`rd system acls create --name ${aclName} --file ${file}`)
        //         else
        //             await exec(`rd system acls upload --name ${aclName} --file ${file}`)
        //     }
        // }

        // console.log('\n\n       Fin        ')
    }
}