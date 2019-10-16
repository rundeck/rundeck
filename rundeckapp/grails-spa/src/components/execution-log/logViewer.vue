<template>
  <div class="execution-log">
    <div class="stats">
      <span>Following:{{follow}} Lines:{{logEntries.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} TotalTime:{{totalTime/1000}}s</span>
    </div>
    <div ref="mine" class="scroller"/>
  </div>
</template>

<script lang="ts">
import {ExecutionLog} from '@/utilities/ExecutionLogConsumer'
import { Component, Prop, Watch, Vue } from 'vue-property-decorator'
import { RecycleScroller, DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import Entry from './logEntry.vue'
import EntryFlex from './logEntryFlex.vue'
import { ExecutionOutputGetResponse } from 'ts-rundeck/dist/lib/models';

Vue.component("recycle-scroller",RecycleScroller)
Vue.component("dynamic-scroller",DynamicScroller)
Vue.component("dynamic-sroller-item",DynamicScrollerItem)

@Component
export default class LogViewer extends Vue {
    scrollTolerance = 5

    viewer!: ExecutionLog

    logEntries: Array<{log?: string, id: number}> = [{log: 'Foo', id: 0}]

    follow = false

    frameTime = 0

    private scrollCount = 0

    totalTime = 0

    private startTime = 0

    private resp?: Promise<ExecutionOutputGetResponse>

    async mounted() {
      this.startTime = Date.now()
      this.populateLogs()

      const mine = this.$refs["mine"] as HTMLElement
      mine.addEventListener('wheel', (ev: UIEvent) => {
        if (ev.isTrusted)
          console.log('Scrolled!')
          this.scrollCount++

        if (this.follow) {
          ev.preventDefault()
          ev.returnValue = false
        }
        
        if (this.scrollCount > this.scrollTolerance)
          this.follow = false
      }, {passive: false})
    }

    async populateLogs() {
        this.viewer = new ExecutionLog('3')

        let count = 0

        let newVues: any = []
        
        while(true) {
            if (!this.resp)
              this.resp = this.viewer.getOutput(500)

            const res = await this.resp
            this.resp = undefined

            if (!this.viewer.completed) {
              this.resp = this.viewer.getOutput(500)
              await new Promise((res, rej) => setTimeout(() => {res()},0))
            }

            const newEntries = res.entries.map(e => {
              count++
              return {log: e.log, id: count}
            })

            this.logEntries.push(...newEntries)

            const chunk = document.createElement("DIV")
            chunk.className = "log-chunk ansicolor-on"
            const frag = document.createDocumentFragment()
            frag.appendChild(chunk)

            res.entries.forEach( e => {
              const span = document.createElement("SPAN")
              // span.className = 'log-line'
              // span.innerHTML = `${e.log}`

              chunk.append(span)
              
              newVues.push(new Vue({el: span, render: h => h(EntryFlex, {props: {entry: e}})}))
            })
            
            const mine = this.$refs["mine"] as HTMLElement
            mine.appendChild(frag)

            // if (this.logEntries.length < 500) {
            //   window.requestAnimationFrame(() => {
            //     mine.style.display = 'none'
            //     window.requestAnimationFrame(() => {
            //       mine.style.display = 'block'
            //     })
            //   })
            // }

            if (this.follow) {
                const last = mine.lastChild!.lastChild as HTMLElement
                if (last && this.follow) {
                  console.log('Scroll')
                  last.scrollIntoView()
                }
            }
            
            if (this.viewer.completed)
                break
        }
        this.totalTime = Date.now() - this.startTime
    }
}
</script>

<style lang="scss">
@import './ansi.css';

body, html {
  height: 100%;
  margin: 0;
}

.execution-log {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.log-gutter {
  background-color: rgb(32, 56, 56);
  color: cornsilk;
  box-sizing: border-box;
  border-style: none none dotted none;
  border-width: 1px;
  border-color: rgb(22, 36, 36);
}

.log-content {
  background-color: darkslategrey;
  color: bisque;
  box-sizing: border-box;
  border-style: none none dotted none;
  border-width: 1px;
  border-color: rgb(39, 66, 66);
}

.log-chunk {
  contain: layout;
}

</style>

<style lang="scss" scoped>
// @import '../../../node_modules/vue-virtual-scroller/dist/vue-virtual-scroller.css';

.stats {
  flex: 0 0 100px;
}

.scroller {
  // height: 100%;
  overflow-y: auto;
  will-change: transform;
  width: 100%;
  flex: 1;
}

</style>