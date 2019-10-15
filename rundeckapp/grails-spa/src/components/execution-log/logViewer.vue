<template>
  <div class="execution-log">
    <div class="stats">
      <span>Following:{{follow}} Lines:{{logEntries.length.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",")}} UpdateTime:{{frameTime}}ms TotalTime:{{totalTime/1000}}s</span>
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

Vue.component("recycle-scroller",RecycleScroller)
Vue.component("dynamic-scroller",DynamicScroller)
Vue.component("dynamic-sroller-item",DynamicScrollerItem)

@Component
export default class LogViewer extends Vue {
    viewer!: ExecutionLog

    logEntries: Array<{log?: string, id: number}> = [{log: 'Foo', id: 0}]

    follow = true

    frameTime = 0

    private scrollCount = 0

    totalTime = 0

    private startTime = 0

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
        
        if (this.scrollCount > 5)
          this.follow = false
      }, {passive: false})
    }

    async populateLogs() {
        this.viewer = new ExecutionLog('5')

        let count = 0

        let newVues: any = []
        
        while(true) {
            const res = await this.viewer.getOutput(500)
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

            const time = Date.now()

            window.requestAnimationFrame(() => {
              this.frameTime = Date.now() - time
              if (this.logEntries.length < 500) {
                console.log('Resize!')
                mine.style.display = 'none'
                window.requestAnimationFrame(() => {
                  mine.style.display = 'block'
                })
              }
            })

            if (this.follow) {
              window.requestAnimationFrame(() => {
                const last = mine.lastChild!.lastChild as HTMLElement
                if (last && this.follow) {
                  console.log('Scroll')
                  last.scrollIntoView()
                }
                })
              mine.lastChild
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
</style>

<style lang="scss" scoped>
// @import '../../../node_modules/vue-virtual-scroller/dist/vue-virtual-scroller.css';

.stats {
  flex: 0 0 100px;
}

.log-chunk {
  width: 100%;
  // contain: layout;
}

.scroller {
  // height: 100%;
  overflow-y: auto;
  will-change: transform;
  width: 100%;
  flex: 1;
}

</style>