<template>
  <span :class="cssForIcon(node.attributes)"
        :style="styleForIcon(node.attributes)">

      <i :class="glyphiconForName(node.attributes['ui:icon:name'])"
         v-if="node.attributes['ui:icon:name']"></i>

      <i :class="iconCss" v-if="!node.attributes['ui:icon:name']"></i>

  </span>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import {cssForIcon, styleForIcon, glyphiconForName} from '../../../utilities/nodeUi'

export default defineComponent({
  name: 'NodeIcon',
  props: {
    node: {
      type: Object,
      required: true,
    },
    defaultIconCss: {
      type: Object,
      required: false,
      default: 'fas fa-hdd',
    },
  },
  computed: {
    iconCss(): string {
      if (!this.defaultIconCss) {
        return 'far fa-circle'
      }
      return this.defaultIconCss
    }
  },
  methods: {
    cssForIcon(attrs: any) {
      return cssForIcon(attrs)
    },
    styleForIcon(attrs: any) {
      return styleForIcon(attrs)
    },
    glyphiconForName(attrs: any) {
      return glyphiconForName(attrs)
    },
  }
})
</script>