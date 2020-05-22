import {Stats} from 'fs'
import * as Path from 'path'

import * as FS from './async/fs'

export interface ITest {
    name: string
    file: string
}

export interface ITestGroup {
    name: string
    tests: ITest[]
}

/**
 * Represents the discovered test folder.
 */
export class TestRepo {
    /** Returns a TestRepo constructed from the supplied path. */
    static async CreateTestRepo(path: string, filter: RegExp): Promise<TestRepo> {
        const absPath = Path.resolve(path)

        const groups = await this._loadRepo(absPath, filter)

        return new TestRepo(groups)
    }

    private static async _loadRepo(path: string, filter: RegExp): Promise<ITestGroup[]> {
        const dirContents = await this._dirContents(path)

        const dirStats = await this._statFiles(dirContents)

        const dirs = await dirStats.filter( e => e.stats.isDirectory() )

        const groupProms = dirs.map( d => this._loadRepoFolder(d.file, filter) )

        groupProms.push(this._loadRepoFolder(path, filter, 'main'))

        return await Promise.all(groupProms)
    }

    private static async _loadRepoFolder(path: string, filter: RegExp, groupName?: string): Promise<ITestGroup> {
        groupName = groupName ? groupName : path.split(Path.sep).pop()!

        const dirContents = (await FS.readdir(path)).map( f => Path.join(path, f) )

        const dirStats = (
            await Promise.all( dirContents.map(f => FS.stat(f)) )
        ).map( (s, i) => ({file: dirContents[i], stats: s}) )

        const testEntries = dirStats.filter( e => e.stats.isFile() && filter.test(e.file) )

        const tests = testEntries.map(
            t => ({
                file: t.file,
                name:
                Path.basename(t.file).split('.').shift()!,
            }),
        )

        return {
            name: groupName,
            tests,
        }
    }

    private static async _dirContents(path: string): Promise<string[]> {
        return (await FS.readdir(path)).map( f => Path.join(path, f) )
    }

    private static async _statFiles(paths: string[]): Promise<Array<{file: string, stats: Stats}>> {
        return (
            await Promise.all(
                paths.map( f => FS.stat(f) ),
            )
        ).map( (s, i) => ({file: paths[i], stats: s}) )
    }

    constructor(readonly groups: ITestGroup[]) {}
}