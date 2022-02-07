<template>
    <details :class="detailsCss">
      <summary  :class="linkCss">
        <slot name="link" :open="open">
          <slot name="label">More...</slot>
          <span class="more-indicator-verbiage more-info-icon"><slot name="more"><i class="glyphicon glyphicon-chevron-right"></i></slot></span>
          <span class="less-indicator-verbiage more-info-icon"><slot name="less"><i class="glyphicon glyphicon-chevron-down"></i></slot></span>
        </slot>
      </summary>

      <slot></slot>

    </details>
</template>
<script lang="ts">
import Vue from 'vue'
import {Collapse} from 'uiv'
import { Component, Prop } from "vue-property-decorator"

@Component({components:{Collapse}})
export default class Expandable extends Vue {
  @Prop({
    type: Object as () => {open: boolean, iconOpen: string, iconClosed: string, linkCss: string},
      default: () => {
        return {open: false}
      }
  })
  options!: any

  open: boolean = !!this.options.open
  linkCss: string =this.options.linkCss || ''

  css: string=this.options.css || ''

  get detailsCss (): string[] {
      return ['more-info','details-reset',this.css]
  }
}

</script>
