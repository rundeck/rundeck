<template>
  <span :class="cssForIcon(node.attributes)"
        :style="styleForIcon(node.attributes)">

      <i :class="glyphiconForName(node.attributes['ui:icon:name'])"
         v-if="node.attributes['ui:icon:name']"></i>

      <i :class="iconCss" v-if="!node.attributes['ui:icon:name']"></i>

  </span>
</template>
<script lang="ts">
import Vue from 'vue'
import {cssForIcon, styleForIcon, glyphiconForName} from '@/app/utilities/nodeUi'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'

@Component
export default class NodeIcon extends Vue {

  @Prop({required: true})
  node!: {}
  @Prop({required: false, default: 'fas fa-hdd'})
  defaultIconCss!: string

  cssForIcon(attrs: any) {
    return cssForIcon(attrs)
  }

  styleForIcon(attrs: any) {
    return styleForIcon(attrs)
  }

  glyphiconForName(attrs: any) {
    return glyphiconForName(attrs)
  }

  get iconCss(): string {
    if (!this.defaultIconCss) {
      return 'far fa-circle'
    }
    return this.defaultIconCss
  }

}
</script>