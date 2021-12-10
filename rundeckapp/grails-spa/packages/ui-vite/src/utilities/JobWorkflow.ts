import {WorkflowStep} from '@rundeck/client/dist/lib/models/index'

export interface IRenderedStep {
  stepNumber: string,
  label: string,
  type: string
}

export type RenderedStepList = Array<IRenderedStep|null>

export class JobWorkflow {
  static CONTEXT_STRING_SEPARATOR = '/'

  constructor(readonly workflow: WorkflowStep[]) {}

  contextType(ctx: string|string[]) {
    let lookupCtx: string[] 
    if (typeof (ctx) == 'string') {
        lookupCtx = JobWorkflow.parseContextId(ctx) as string[]
    } else {
      lookupCtx = ctx
    }
    let step = this.workflow[JobWorkflow.workflowIndexForContextId(lookupCtx[0] as string) as number]
    return _wfTypeForStep(step)
  }

  renderContextStepNumber(ctx: string|string[]) {
    if (typeof (ctx) == 'string') {
        ctx = JobWorkflow.parseContextId(ctx) as string[]
    }
    var string = ''
    string += JobWorkflow.stepNumberForContextId(ctx[0])
    if (ctx.length > 1) {
//                string += "/" + ctx.slice(1).join("/")
    }
    string += ". "
    return string
  }

  renderContextString(ctx: string | string[]) {
      let lookupCtx: string[]

      if (typeof (ctx) == 'string') {
        lookupCtx = JobWorkflow.parseContextId(ctx) as string[]
      } else {
        lookupCtx = ctx
      }

      let step = this.workflow[JobWorkflow.workflowIndexForContextId(lookupCtx[0]) as number]
      return _wfStringForStep(step)
  }

  renderStepsFromContextPath(ctx: string | string[]): RenderedStepList {
    function renderStep(workflow: JobWorkflow, ctx: string[]): RenderedStepList {
      if (ctx===undefined || ctx.length == 0)
        return []
      let step = workflow.workflow[JobWorkflow.workflowIndexForContextId(ctx[0]) as number]

      let nested: RenderedStepList = []

      if (!step)
        return [null]
      else if (step.jobref && step.workflow)
        nested = renderStep(new JobWorkflow(step.workflow), ctx.slice(1))
      else if (step.jobref)
        nested = ctx.slice(1).map(() => null)

      return [
        {
          stepNumber: workflow.renderContextStepNumber(ctx[0]),
          label: _wfStringForStep(step),
          type: _wfTypeForStep(step)
        },
        ...nested
      ]
    }

    var lookupCtx: string[]

    if (typeof (ctx) == 'string') {
      const cleaned = JobWorkflow.cleanContextId(ctx) as string
      lookupCtx = JobWorkflow.parseContextId(cleaned) as string[]
    } else {
      lookupCtx = ctx
    }

    return renderStep(this, lookupCtx)
  }

  static isErrorhandlerForContextId(ctxid: string) {
    let m = ctxid.match(/^(\d+)(e)?(@.+)?$/)
    if (m != null && m[2] == 'e') {
        return true
    }
    return false
}

  static stepNumberForContextId(ctxid: string) {
    let m = ctxid.match(/^(\d+)(e)?(@.+)?$/)
    if (m != null && m[1]) {
        return parseInt(m[1])
    }
    return null
  }

  static workflowIndexForContextId(ctxid: string) {
    let m = this.stepNumberForContextId(ctxid)
    if (m!=null) {
        return m - 1
    }
    return null
  }

  static paramsForContextId(ctxid: string) {
    let m = ctxid.match(/^(\d+)(e)?(@(.+))?$/)
    if (m != null && m[4]) {
        return m[4].replace(/\\([/@,=])/g, '$1')
    }
    return null
  }

  /**
   * Returns array of step context strings given the context identifier
   */
  static parseContextId(context: string | string[]) {
    if (context == null) {
        return null
    }
    //if context is already array, return it
    if (Array.isArray(context)) {
        return context
    }
    //split context into project,type,object
    const t = this.splitEscaped(context, JobWorkflow.CONTEXT_STRING_SEPARATOR)
    return t.slice()
  }

  static createContextId(contextArr: string[]) {
    if (contextArr == null) {
        return null
    }

    if (!Array.isArray(contextArr)) {
        contextArr = [contextArr]
    }
    //split context into project,type,object
    return JobWorkflow.joinEscaped(contextArr, JobWorkflow.CONTEXT_STRING_SEPARATOR)
  }

  /**
   * Removes error handler/parameters from the context path
   */
  static cleanContextId(context: string) {
    const parts = JobWorkflow.parseContextId(context)

    if (parts == null)
      return null

    const newParts = []

    for (let part of parts) {
      newParts.push(JobWorkflow.stepNumberForContextId(part))
    }

    return newParts.join(JobWorkflow.CONTEXT_STRING_SEPARATOR)
  }

  static splitEscaped(input: string, sep: string){
    let parts = [] as string[]

    let rest: string | null = input
    while (rest) {
        let result= this.unescape(rest, '\\', ['\\','/'], [sep])
        parts.push(result.text)
        rest = result.rest
    }
    return parts
  }

  /**
   * Join array strings into single string using the separator, escaping
   * internal chars with backslash
   */
  static joinEscaped = function (arr: string[], sep: string) {
    var res = [];
    for (var i = 0; i < arr.length; i++) {
        if (i > 0) {
            res.push(sep);
        }
        res.push(JobWorkflow.escapeStr(arr[i], '\\', ['\\', sep]));
    }
    return res.join("");
};

  /**
   * Escape listed chars in the string with the escape char
   */
  static escapeStr = function (str: string, echar: string, chars: string[]) {
    var arr = []
    for (var i = 0; i < str.length; i++) {
        var c = str.charAt(i)
        if(chars.indexOf(c)>=0){
            arr.push(echar)
        }
        arr.push(c)
    }
    return arr.join("")
  }

  static unescape(input: string, echar: string, chars: string[], breakchars: string[]) {
    let arr = []
    let e = false
    let bchar = null

    let i = 0
    for (; i<input.length; i++){
        let c = input.charAt(i)
        if(c == echar){
            if(e){
                arr.push(echar)
                e = false
            }else{
                e = true
            }
        }else if(chars.indexOf(c) >= 0){
            if(e){
                arr.push(c)
                e = false
            }else if(breakchars.indexOf(c) >= 0){
                bchar=c
                break
            }else{
                arr.push(c)
            }
        } else {
            if(e){
                arr.push(echar)
                e = false
            }
            arr.push(c)
        }
    }
    return {
      text: arr.join(""),
      bchar: bchar,
      rest: i <= input.length - 1 ? input.substring(i + 1) : null
    }

  }
}

function _wfTypeForStep(step: WorkflowStep){
  if (typeof(step) != 'undefined') {
      if (step['exec']) {
          return 'command';
      } else if (step['jobref']) {
          return 'job';
      } else if (step['script']) {
          return 'script';
      } else if (step['scriptfile']) {
          return 'scriptfile';
      } else if (step['scripturl']) {
          return 'scripturl';
      } else if (step['type']) {//plugin
          if (step['nodeStep'] ) {
              return 'node-step-plugin plugin';
          } else if (null != step['nodeStep'] && !step['nodeStep'] ) {
              return 'workflow-step-plugin plugin';
          }else{
              return 'plugin';
          }
      }
  }
  return 'console'
}

function _wfStringForStep(step: WorkflowStep){
  var string = "";
  if (typeof(step) != 'undefined') {
      if(step['description']){
          string = step['description'];
      }else if (step['exec']) {
//                        string+=' $ '+step['exec'];
          string = 'Command';
      } else if (step['jobref']) {
          string = (step['jobref']['group'] ? step['jobref']['group'] + '/' : '') + step['jobref']['name'];
      } else if (step['script']) {
          string = "Script";
      } else if (step['scriptfile']) {
          string = 'File';
      }else if (step['scripturl']) {
          string = 'URL';
      } else if (step['type']) {//plugin
          var title = "Plugin " + step['type'];
          // TODO: Figure out how to get this data without relying on it being in the window
          // if (step['nodeStep'] && RDWorkflow.nodeSteppluginDescriptions && RDWorkflow.nodeSteppluginDescriptions[step['type']]) {
          //     title = RDWorkflow.nodeSteppluginDescriptions[step['type']].title || title;
          // } else if (!step['nodeStep'] && RDWorkflow.wfSteppluginDescriptions && RDWorkflow.wfSteppluginDescriptions[step['type']]) {
          //     title = RDWorkflow.wfSteppluginDescriptions[step['type']].title || title;
          // }
          // string = title;
      }
  }else{
      return "[?]";
  }
  return string;
}