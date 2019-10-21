import Vue from 'vue'

import EntryFlex from './logEntryFlex.vue'

import {ExecutionOutputEntry} from 'ts-rundeck/dist/lib/models/index'

interface IBuilderOpts {
  nodeIcon: boolean
}

export class LogBuilder {
  private logEntries: Array<{id: number} & ExecutionOutputEntry> = []

  currNode?: HTMLElement
  currChunk?: HTMLElement

  constructor(readonly root: HTMLElement, opts: IBuilderOpts) {

  }

  addLine(logEntry: ExecutionOutputEntry): Vue {
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

    const span = document.createElement("SPAN")

    this.currChunk!.append(span)

    const renderNodeBadge = (lastEntry == undefined || logEntry.node != lastEntry.node)

    const vue = new EntryFlex({el: span, propsData: {entry: newEntry, nodeBadge: renderNodeBadge}})
    vue.$on('line-select', () => true)

    const elem = vue.$el as HTMLElement
    elem.title = `${newEntry.node} ${newEntry.absoluteTime} ${newEntry.stepctx}`

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
    }
  }

  private closeChunk() {
    this.currChunk = undefined
  }
}