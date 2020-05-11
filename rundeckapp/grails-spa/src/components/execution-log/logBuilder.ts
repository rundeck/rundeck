import Vue from 'vue'

import {ExecutionOutputEntry} from 'ts-rundeck/dist/lib/models/index'

import EntryFlex from './logEntryFlex.vue'
import {IRenderedEntry} from 'utilities/ExecutionLogConsumer'

interface IBuilderOpts {
  nodeIcon?: boolean
  time?: {
    visible: boolean
  }
  maxLines?: number
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

  private lastEntry?: {id: number} & ExecutionOutputEntry
  private count: number = 0
  constructor(readonly root: HTMLElement, opts: IBuilderOpts) {
    this.opts = Object.assign(LogBuilder.DefaultOpts(), opts)
  }

  static DefaultOpts(): Required<IBuilderOpts> {
    return {
      nodeIcon: true,
      maxLines: 5000,
      time: {
        visible: false
      }
    }
  }

  addLine(logEntry: IRenderedEntry, selected: boolean): Vue {
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

    const renderNodeBadge = (lastEntry == undefined || logEntry.node != lastEntry.node)

    const label = this.entryStepLabel(newEntry)
    const stepType = this.entryStepType(newEntry)
    const path = this.entryPath(newEntry)

    const vue = new EntryFlex({propsData: {selected, timestamp: this.opts.time.visible}});

    // TODO: Remove seemingly failed attempt to reduce mem footprint due to Vue reactivity
    // The entry can probably be passed through as a prop again
    (<any>vue).entry = {
      log: newEntry.log,
      logHtml: (<any>newEntry).loghtml,
      time: newEntry.time,
      level: newEntry.level,
      stepLabel: label,
      path,
      stepType,
      lineNumber: newEntry.id,
      node: newEntry.node,
      nodeBadge: renderNodeBadge,
      selected,
    }
    vue.$mount(span)

    const elem = vue.$el as HTMLElement
    elem.title = this.entryTitle(newEntry)

    this.lastEntry = newEntry

    return vue
  }

  private entryStepType(newEntry: IRenderedEntry) {
    const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    return lastStep ? lastStep.type : ''
  }

  private entryStepLabel(newEntry: IRenderedEntry) {
    const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    const label = lastStep ? 
      `${lastStep.stepNumber}${lastStep.label}` :
      newEntry.stepctx

    return label
  }

  private entryPath(newEntry: IRenderedEntry) {
    let stepString = newEntry.renderedStep.map( s => {
      if (!s)
        return '..'
      return `${s.stepNumber}${s.label}`
    })
    return stepString.join(' / ')
  }

  private entryTitle(newEntry: IRenderedEntry) {
    return `#${newEntry.lineNumber} ${newEntry.absoluteTime} ${this.entryPath(newEntry)}`
  }

  private openNode() {
    this.currNode = document.createElement("DIV")
    this.currNode.className = 'execution-log__node-chunk'
    this.root.appendChild(this.currNode)
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
      this.currChunk.className = "execution-log__chunk ansicolor-on"
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