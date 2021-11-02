<template>
  <span
      :title="node.attributes['ui:status:text']"
      :class="statusIconCss(node.attributes)"
      :style="statusIconStyle(node.attributes)"
  >
      <i :class="glyphiconForName(node.attributes['ui:status:icon'])" class="node-status-icon" v-if="node.attributes['ui:status:icon']"></i>
      <slot>
        <span v-if="showText && node.attributes['ui:status:text']">{{ node.attributes['ui:status:text'] }}</span>
      </slot>
  </span>
</template>
<script lang="ts">
import {glyphiconForName, statusIconStyle, statusIconCss} from '@/utilities/nodeUi'

export default {
  name: 'node-status',
  props: {
    node: {},
    showText: false,
  },

  methods: {
    statusIconCss,
    statusIconStyle,
    glyphiconForName
  }
}
</script>