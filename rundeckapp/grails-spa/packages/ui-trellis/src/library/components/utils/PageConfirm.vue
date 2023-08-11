<template>
  <span ><slot v-if="display && needsConfirm" :confirm="confirmData" :needs-confirm="needsConfirm">{{message}}</slot></span>
</template>
<script lang="ts">
import { defineComponent, ref} from 'vue'
import type { PropType } from 'vue'
import {EventBus} from "../../utilities/vueEventBus";

export default defineComponent({
  name: 'PageConfirm',
  props: {
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: true,
    },
    message: {
      type: String,
      required: true,
    },
    display: {
      type: Boolean,
      required: true,
    },
  },
  setup() {
    const confirmData = ref<string[]>([])
    return {
      confirmData,
    }
  },
  computed: {
    needsConfirm() {
      return this.confirmData.length > 0
    },
  },
  methods: {
    setConfirm(name: string) {
      const loc = this.confirmData.indexOf(name)
      if (loc < 0) {
        this.confirmData.push(name)
      }
    },
    resetConfirm(name: string) {
      const loc = this.confirmData.indexOf(name)
      if (loc >= 0) {
        this.confirmData.splice(loc, 1)
      } else if (name === '*') {
        this.confirmData.splice(0, this.confirmData.length)
      }
    }
  },
  mounted() {
    this.eventBus.on('page-modified', this.setConfirm)
    this.eventBus.on('page-reset', this.resetConfirm)

    const orighandler = window.onbeforeunload
    window.onbeforeunload = (ev: BeforeUnloadEvent) => {
      if (this.needsConfirm) {
        return this.message || 'confirm'
      }
      if (typeof (orighandler) === 'function') {
        //@ts-ignore
        return orighandler(ev)
      }
    }
  }
})
</script>
