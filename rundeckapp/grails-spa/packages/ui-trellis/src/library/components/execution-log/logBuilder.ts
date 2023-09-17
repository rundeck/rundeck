import {App, createApp} from 'vue'

import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutput, ExecutionOutputEntry } from '../../stores/ExecutionOutput'
import { IObservableArray, autorun } from 'mobx'
import { EventBus } from '../../utilities/vueEventBus'

export interface IBuilderOpts {
  node?: string,
  stepCtx?: string,
  nodeIcon?: boolean
  command?: {
    visible: boolean
  }
  gutter?: {
    visible: boolean
  },
  time?: {
    visible: boolean
  }
  maxLines?: number
  content?: {
    lineWrap: boolean
  }
}

/**
 * LogBuilder constructs and manages a list of log entry elements
 * inside the provided DOM element.
 */
export class LogBuilder {
  chunks: HTMLElement[] = []
  currNode?: HTMLElement
  currChunk?: HTMLElement
  chunkSize: number = 0

  private opts: Required<IBuilderOpts>

  newLineHandlers: Array<(entries: Array<App<Element>>) => void> = []

  private lastEntry?: {id: number} & ExecutionOutputEntry
  private count: number = 0

  constructor(
      readonly executionOutput: ExecutionOutput,
      readonly rootElem: HTMLElement,
      readonly eventBus: typeof EventBus,
      opts: IBuilderOpts) {
    
    this.opts = Object.assign(LogBuilder.DefaultOpts(), opts)

    const {node, stepCtx} = this.opts

    this.observeFiltered(() => {
      return executionOutput.getEntriesFiltered(node, stepCtx)
    })
  }

  onNewLines(handler: (entries: Array<App<Element>>) => void) {
    this.newLineHandlers.push(handler)
  }

  /**
   * Observes output returned by a lookup function
   */
  observeFiltered(lookupFunc: () => IObservableArray<ExecutionOutputEntry> | undefined) {
    autorun( (reaction) => {
      const output = lookupFunc()
      if (!output)
        return

      reaction.dispose() // Data is available so we no longer need to react to the map
      this.addLines(output)
      this.observeOutput(output)
    })
  }

  observeOutput(entries: IObservableArray<ExecutionOutputEntry>) {
    entries.observe(change => {
      if (change.type == 'splice' && change.addedCount > 0) {
        this.addLines(change.added)
      }
    })
  }

  static DefaultOpts(): Required<IBuilderOpts> {
    return {
      node: '',
      stepCtx: '',
      nodeIcon: true,
      maxLines: 5000,
      command: {
        visible: true
      },
      time: {
        visible: false
      },
      gutter: {
        visible: true
      },
      content: {
        lineWrap: true
      }
    }
  }

  updateProps(opts: IBuilderOpts) {
    this.opts = Object.assign(LogBuilder.DefaultOpts(), opts)
    this.eventBus.emit("execution-log-settings-changed", this.opts)
  }

  addLines(entries: Array<ExecutionOutputEntry>) {
    const items = entries.map( e => {
      return this.addLine(e, false)
    })
    for (const handler of this.newLineHandlers) {
      handler(items)
    }
  }

  addLine(logEntry: ExecutionOutputEntry, selected: boolean): App<Element> {
    this.count++
    const {lastEntry, count} = this

    const newEntry = {id: count, ...logEntry}

    if (!this.currNode)
      this.openNode()
    else if (lastEntry != undefined && logEntry.node != lastEntry.node) {
      this.closeNode()
      this.openNode()
    }

    if (this.chunkSize > 200)
      this.openChunk()

    this.chunkSize++

    const span = document.createElement("SPAN")

    this.currChunk!.append(span)

    const label = this.entryStepLabel(newEntry)
    const stepType = this.entryStepType(newEntry)
    const path = this.entryPath(newEntry)

    const vue = createApp({
      name: "EntryFlexLine",
      components: { EntryFlex },
      template: "<EntryFlex></EntryFlex>"
    },{
        eventBus: this.eventBus,
        selected: selected,
        config: this.opts,
        prevEntry: this.lastEntry,
        logEntry: {
          meta: newEntry.meta,
          log: newEntry.log,
          logHtml: newEntry.logHtml,
          time: newEntry.time,
          level: newEntry.level,
          stepLabel: label,
          path,
          stepType,
          lineNumber: newEntry.id,
          node: newEntry.node,
          selected,
        }
    });
    vue.mount(span)

    const elem = vue._container as HTMLElement
    elem.title = this.entryTitle(newEntry)

    this.lastEntry = newEntry

    return vue
  }

  private entryStepType(newEntry: ExecutionOutputEntry) {
    if (!newEntry.renderedStep)
      return ''

    const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    return lastStep ? lastStep.type : ''
  }

  private entryStepLabel(newEntry: ExecutionOutputEntry) {
    if (!newEntry.renderedStep)
      return ''

    const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    const label = lastStep ? 
      `${lastStep.stepNumber}${lastStep.label}` :
      newEntry.stepctx

    return label
  }

  private entryPath(newEntry: ExecutionOutputEntry) {
    if (!newEntry.renderedStep)
      return ''

    let stepString = newEntry.renderedStep.map( s => {
      if (!s)
        return '..'
      return `${s.stepNumber}${s.label}`
    })
    return stepString.join(' / ')
  }

  private entryTitle(newEntry: ExecutionOutputEntry) {
    return `#${newEntry.lineNumber} ${newEntry.absoluteTime} ${this.entryPath(newEntry)}`
  }

  private openNode() {
    this.currNode = document.createElement("DIV")
    this.currNode.className = 'execution-log__node-chunk'
    this.rootElem.appendChild(this.currNode)
    this.openChunk()
  }

  private closeNode() {
    this.currNode = undefined
  }

  private openChunk() {
    if (!this.currNode)
      this.openNode
    else {
      this.currChunk = document.createElement("DIV")
      this.currChunk.className = "execution-log__chunk"
      this.currNode.appendChild(this.currChunk)
      this.chunkSize = 0
      this.chunks.push(this.currChunk)
    }
  }

  public dropChunk() {
    let size = 0
    const chunk = this.chunks.shift()
    if (chunk) {
      size = chunk.childNodes.length
      while (chunk.firstChild) {
        chunk.removeChild(chunk.firstChild)
      }
    }
    return size
  }

  private closeChunk() {
    this.currChunk = undefined
  }
}
