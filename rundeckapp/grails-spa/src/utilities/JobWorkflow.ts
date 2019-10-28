import {JobWorkflowGetResponse, WorkflowStep} from 'ts-rundeck/dist/lib/models/index'

class JobWorkflow {
  static CONTEXT_STRING_SEPARATOR = '/'

  constructor(readonly workflow: JobWorkflowGetResponse) {}


  renderContextString(ctx: string) {
      if (typeof (ctx) == 'string') {
          ctx = this.parseContextId(ctx)
      }
      let step = this.workflow[RDWorkflow.workflowIndexForContextId(ctx[0])]
      return _wfStringForStep(step)
  }

  stepNumberForContextId(ctxid: string) {
    let m = ctxid.match(/^(\d+)(e)?(@.+)?$/)
    if (m != null && m[1]) {
        return parseInt(m[1])
    }
    return null
  }

  workflowIndexForContextId(ctxid: string) {
    let m = this.stepNumberForContextId(ctxid)
    if (m!=null) {
        return m - 1
    }
    return null
  }

  paramsForContextId(ctxid: string) {
    let m = ctxid.match(/^(\d+)(e)?(@(.+))?$/)
    if (m != null && m[4]) {
        return m[4].replace(/\\([/@,=])/g, '$1')
    }
    return null
  }

  parseContextId(context: string) {
    if (context == null) {
        return null
    }
    //if context is already array, return it
    if (Array.isArray(context)) {
        return context
    }
    //split context into project,type,object
    var t = this.splitEscaped(context, JobWorkflow.CONTEXT_STRING_SEPARATOR)
    return t.slice()
  }

  private splitEscaped(input: string, sep: string){
    let parts = [] as string[]

    let rest: string | null = input
    while (rest) {
        let result= this.unescape(rest, '\\', ['\\','/'], [sep])
        parts.push(result.text)
        rest = result.rest
    }
    return parts
  }

  private unescape(input: string, echar: string, chars: string[], breakchars: string[]) {
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
  "use strict";
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
  "use strict";
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
          if (step['nodeStep'] && RDWorkflow.nodeSteppluginDescriptions && RDWorkflow.nodeSteppluginDescriptions[step['type']]) {
              title = RDWorkflow.nodeSteppluginDescriptions[step['type']].title || title;
          } else if (!step['nodeStep'] && RDWorkflow.wfSteppluginDescriptions && RDWorkflow.wfSteppluginDescriptions[step['type']]) {
              title = RDWorkflow.wfSteppluginDescriptions[step['type']].title || title;
          }
          string = title;
      }
  }else{
      return "[?]";
  }
  return string;
}