import chalk from 'chalk'
import indent from 'indent-string'

import {exec} from './async/child-process'
import {ITest, ITestGroup, TestRepo} from './test-repo'

interface ITestResult {
    test: ITest
    stdout: string
    stderr: string
    success: boolean
}

interface ITestGroupResult {
    testGroup: ITestGroup
    testResults: ITestResult[]
}

export class BitScriptRunner {
    constructor(readonly testRepo: TestRepo) {}

    async run() {
        const groupResults = await this.testGroups()
        this.summary(groupResults)
    }

    async testGroups(): Promise<ITestGroupResult[]> {
        const groupResults: ITestGroupResult[] = []

        for (const group of this.testRepo.groups) {
            const testResults: ITestResult[] = []

            for (const test of group.tests) {
                let stdout = ''
                let stderr = ''
                let success = false

                try {
                    const res = await exec(test.file)
                    console.log(`${chalk.green('✔️')} ${group.name}/${test.name}`)
                    stdout = res.stdout
                    stderr = res.stderr
                    success = true
                } catch (e) {
                    console.log(`${chalk.red('❌')} ${test.name} ${test.file}`)
                    stdout = e.stdout
                    stderr = e.stderr
                } finally {
                    if (stdout != '')
                        console.log(indent(`stdout:\n${indent(stdout, 4)}`, 4))
                    if (stderr != '')
                        console.log(indent(`stderr:\n${indent(chalk.red(stderr), 4)}`, 4))
                }

                testResults.push({
                    stderr,
                    stdout,
                    success,
                    test,
                })
            }
            groupResults.push({
                testGroup: group,
                testResults,
            })
        }
        return groupResults
    }

    summary(resultGroups: ITestGroupResult[]) {
        let total = 0
        let passed = 0
        let failed = 0

        const testResults = resultGroups.reduce( (tests: ITestResult[], group) => tests.concat(group.testResults), [])

        testResults.forEach( result => {
            if (result.success)
                passed++
            else
                failed++
            total++
        })

        const metrics: string[] = []

        if (failed != 0)
            metrics.push(chalk.red(`${failed} failed`))
        if (passed != 0)
            metrics.push(chalk.green(`${passed} passed`))
        metrics.push(`${total} total`)

        let statusEmoji = ''

        if (failed == 0)
            statusEmoji = chalk.green('💯')
        else
            statusEmoji = chalk.red('⛔')

        console.log(`${statusEmoji} Tests: ${metrics.join(', ')}`)

        if (failed != 0)
            process.exitCode = 1
    }
}