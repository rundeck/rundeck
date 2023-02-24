<template>
  <span>
    <btn :class="{disabled:!hasUndo}" @click="doUndo" size="xs">
      <i class="glyphicon glyphicon-step-backward"></i>
      Undo
    </btn>
    <btn :class="{disabled:!hasRedo}" @click="doRedo" size="xs">
      Redo
      <i class="glyphicon glyphicon-step-forward"></i>
    </btn>
  </span>
</template>
<script lang="ts">

import Vue from "vue"

export default Vue.extend({
  name: "UndoRedo",
  props: {
    'eventBus': Vue,
    'ident': String,
    'max': Number
  },
  data() {
    return {
      stack: <any>[],
      index: 0,
    }
  },
  computed: {
    hasUndo(): boolean {
      return this.stack.length > this.index
    },
    hasRedo(): boolean {
      return this.index > 0
    }
  },
  methods: {
    addChange(val: any) {
      if (this.index > 0) {
        this.stack.splice(0, this.index)
        this.index = 0
      }
      this.stack.unshift(val)
    },
    doUndo() {
      if(this.index>=this.stack.length){
        return
      }
      let newindex = this.index + 1
      let change = this.stack[this.index]
      this.index = newindex
      this.eventBus.$emit('undo', change)
    },
    doRedo() {
      if (this.index < 1) {
        return
      }
      let newindex = this.index - 1
      let change = this.stack[newindex]
      this.index = newindex
      this.eventBus.$emit('redo', change)
    }
  },
  mounted() {
    this.eventBus.$on('change', this.addChange)
  }
})
</script>
