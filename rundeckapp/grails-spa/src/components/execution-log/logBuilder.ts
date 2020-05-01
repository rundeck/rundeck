import Vue from 'vue'

import {ExecutionOutputEntry} from 'ts-rundeck/dist/lib/models/index'

import EntryFlex from './logEntryFlex.vue'
import {IRenderedEntry} from 'utilities/ExecutionLogConsumer'

interface IBuilderOpts {
  nodeIcon: boolean
}

export class LogBuilder {
  private logEntries: Array<{id: number} & ExecutionOutputEntry> = []

  currNode?: HTMLElement
  currChunk?: HTMLElement
  chunkSize: number = 0

  constructor(readonly root: HTMLElement, opts: IBuilderOpts) {

  }

  addLine(logEntry: IRenderedEntry, selected: boolean): Vue {
    let count = this.logEntries.length
    count++

    const newEntry = {id: count, ...logEntry}

    const lastEntry = this.logEntries[this.logEntries.length - 1]

    if (!this.currNode)
      this.openNode()
    else if (lastEntry != undefined && logEntry.node != lastEntry.node) {
      this.closeNode()
      this.openNode()
    }

    // if (this.chunkSize > 500)
    //   this.openChunk()

    this.chunkSize++

    const span = document.createElement("SPAN")

    this.currChunk!.append(span)

    const renderNodeBadge = (lastEntry == undefined || logEntry.node != lastEntry.node)

    const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    const label = lastStep ? 
      `${lastStep.stepNumber.trim()}${lastStep.label}` :
      newEntry.stepctx

    const vue = new EntryFlex({propsData: {selected}});

    (<any>vue).entry = {
      log: newEntry.log,
      logHtml: (<any>newEntry).loghtml,
      time: newEntry.time,
      level: newEntry.level,
      stepLabel: label,
      lineNumber: newEntry.id,
      nodeBadge: renderNodeBadge,
      selected
    }
    vue.$mount(span)

    const elem = vue.$el as HTMLElement
    elem.title = `#${newEntry.lineNumber} ${newEntry.node} ${newEntry.absoluteTime}`

    this.logEntries.push(newEntry)

    return vue
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
    }
  }

  private closeChunk() {
    this.currChunk = undefined
  }
}