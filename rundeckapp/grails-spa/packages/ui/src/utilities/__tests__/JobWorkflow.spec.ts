import 'jest'

import {JobWorkflow} from '../JobWorkflow'

const workflowSample = [
    {
        "jobref": {
          "name": "Call JSON",
          "uuid": "e49de4ec-b58c-440a-b881-9bf4013372af",
          "nodeStep": "true",
          "importOptions": true
        },
        "jobId": "e49de4ec-b58c-440a-b881-9bf4013372af",
        "workflow": [
          {
            "jobref": {
              "name": "JSON",
              "uuid": "15ecee9e-cdd1-4f51-a443-631f8e223992",
              "nodeStep": "true",
              "importOptions": true
            },
            "jobId": "15ecee9e-cdd1-4f51-a443-631f8e223992",
            "workflow": [
              {
                "description": "Uh-oh",
                "exec": "true"
              },
              {
                "description": "Ha",
                "exec": "echo \"${option.opt1}\" | jq -C ."
              },
              {
                "jobref": {
                  "name": "Output",
                  "uuid": "bd9c4952-82f4-4f40-97a6-9d339a917697",
                  "nodeStep": "true"
                },
                "jobId": "bd9c4952-82f4-4f40-97a6-9d339a917697"
              }
            ]
          }
        ]
      }
]

describe('Workflow', () => {

    test('unescape', () => {
        expect(JobWorkflow.unescape('abc/123', '\\', ['\\', '/'], ['/'])).toEqual({
            text: 'abc',
            bchar: '/',
            rest: '123'
        })

        expect(JobWorkflow.unescape('abc/123/456', '\\', ['\\', '/'], ['/'])).toEqual({
            text: 'abc',
            bchar: '/',
            rest: '123/456'
        })

        expect(JobWorkflow.unescape('a\\/bc/123/456', '\\', ['\\', '/'], ['/'])).toEqual({
            text: 'a/bc',
            bchar: '/',
            rest: '123/456'
        })
    })

    test('escape string', () => {
        expect(JobWorkflow.escapeStr('abc/123', '\\', ['\\', '/'])).toEqual('abc\\/123')
        expect(JobWorkflow.escapeStr('abc\\123', '\\', ['\\', '/'])).toEqual('abc\\\\123')
    })

    test('split escaped', () => {
        expect(JobWorkflow.splitEscaped('a\\/bc/123/456', '/')).toEqual(['a/bc', '123', '456'])
        expect(JobWorkflow.splitEscaped('a\\/b@c/1,2=3/4\\\\56', '/')).toEqual(['a/b@c', '1,2=3', '4\\56'])
    })

    test('join escaped', () => {
        expect(JobWorkflow.joinEscaped(['a/bc','123','456'], '/')).toEqual('a\\/bc/123/456')
        expect(JobWorkflow.joinEscaped(['a/b@c','1,2=3','4\\56'], '/')).toEqual('a\\/b@c/1,2=3/4\\\\56')
    })

    test('is error handler for context id', () => {
        expect(JobWorkflow.isErrorhandlerForContextId('1e@blah=c')).toBe(true)
        expect(JobWorkflow.isErrorhandlerForContextId('2')).toBe(false)
        expect(JobWorkflow.isErrorhandlerForContextId('2@node=a')).toBe(false)
        expect(JobWorkflow.isErrorhandlerForContextId('2e')).toBe(true)
        expect(JobWorkflow.isErrorhandlerForContextId('2e@blah=c')).toBe(true)
    })

    test('params for context id', () => {
        expect(JobWorkflow.paramsForContextId('2')).toBeNull()
        expect(JobWorkflow.paramsForContextId('2@node=a')).toEqual('node=a')
        expect(JobWorkflow.paramsForContextId('2@node\\=a')).toEqual('node=a')
        expect(JobWorkflow.paramsForContextId('2e')).toBeNull()
        expect(JobWorkflow.paramsForContextId('2e@blah=c')).toEqual('blah=c')
    })

    test('step number for context id', () => {
        expect(JobWorkflow.stepNumberForContextId('1e@blah=c')).toBe(1)
        expect(JobWorkflow.stepNumberForContextId('2')).toBe(2)
        expect(JobWorkflow.stepNumberForContextId('2@node=a')).toBe(2)
        expect(JobWorkflow.stepNumberForContextId('2e')).toBe(2)
        expect(JobWorkflow.stepNumberForContextId('2e@blah=c')).toBe(2)
    })

    test('workflow index for context id', () => {
        expect(JobWorkflow.workflowIndexForContextId('1e@blah=c')).toBe(0)
        expect(JobWorkflow.workflowIndexForContextId('2')).toBe(1)
        expect(JobWorkflow.workflowIndexForContextId('2@node=a')).toBe(1)
        expect(JobWorkflow.workflowIndexForContextId('2e')).toBe(1)
        expect(JobWorkflow.workflowIndexForContextId('2e@blah=c')).toBe(1)
    })

    test('parse context id', () => {
        expect(JobWorkflow.parseContextId('1')).toEqual(['1'])
        expect(JobWorkflow.parseContextId('1/1')).toEqual(['1', '1'])
        expect(JobWorkflow.parseContextId('1/1/1')).toEqual(['1', '1', '1'])
        expect(JobWorkflow.parseContextId('1/2/3')).toEqual(['1', '2', '3'])
        expect(JobWorkflow.parseContextId('1e@abc/2/3')).toEqual(['1e@abc', '2', '3'])
        expect(JobWorkflow.parseContextId('1/2e@asdf=xyz/3')).toEqual(['1', '2e@asdf=xyz', '3'])
        expect(JobWorkflow.parseContextId('2@node=crub\\/dub-1/1')).toEqual(['2@node=crub/dub-1', '1'])
    })

    test('create context id', () => {
        expect(JobWorkflow.createContextId(['1'])).toEqual('1')
        expect(JobWorkflow.createContextId(['1', '1'])).toEqual('1/1')
        expect(JobWorkflow.createContextId(['1', '1', '1'])).toEqual('1/1/1' )
        expect(JobWorkflow.createContextId(['1', '2', '3'])).toEqual('1/2/3' )
        expect(JobWorkflow.createContextId(['1e@abc', '2', '3'])).toEqual('1e@abc/2/3')
        expect(JobWorkflow.createContextId(['1', '2e@asdf=xyz', '3'])).toEqual('1/2e@asdf=xyz/3')
        expect(JobWorkflow.createContextId(['2@node=crub/dub-1', '1'])).toEqual('2@node=crub\\/dub-1/1')
    })

    test('clean context id', () => {
        expect(JobWorkflow.cleanContextId('1/2/3')).toEqual('1/2/3')
        expect(JobWorkflow.cleanContextId('1e@abc/2/3')).toEqual('1/2/3')
        expect(JobWorkflow.cleanContextId('1/2e@asdf=xyz/3')).toEqual('1/2/3')
    })

    test('render steps from context path', () => {
        const workflow = new JobWorkflow(workflowSample)
        let steps = workflow.renderStepsFromContextPath('1@node=ubuntu/1@node=ubuntu/1')

        expect(steps.length).toEqual(3)
        expect(steps[0]).toEqual({label: 'Call JSON', stepNumber: '1. ', type: 'job'})
        expect(steps[1]).toEqual({label: 'JSON', stepNumber: '1. ', type: 'job'})
        expect(steps[2]).toEqual({label: 'Uh-oh', stepNumber: '1. ', type: 'command'})

        steps = workflow.renderStepsFromContextPath('1@node=ubuntu/1@node=ubuntu/3/1/1')
        expect(steps.length).toEqual(5)
        expect(steps[0]).toEqual({label: 'Call JSON', stepNumber: '1. ', type: 'job'})
        expect(steps[1]).toEqual({label: 'JSON', stepNumber: '1. ', type: 'job'})
        expect(steps[2]).toEqual({label: 'Output', stepNumber: '3. ', type: 'job'})
        expect(steps[3]).toBeNull
        expect(steps[4]).toBeNull
    })

    test('step plugin description', () => {
        //TODO: Move these tests in
    })
})