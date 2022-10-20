<template>
  <a :href="href()" class="xnodefilterlink" @click.prevent="handleClick">
    <slot name="prefix"></slot>
    <slot>{{ getText() }}</slot>
    <slot name="suffix"></slot>
  </a>
</template>
<script lang="ts">

import {_genUrl} from '@/app/utilities/genUrl'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'

import {
  getRundeckContext,
  url
} from '@/library/rundeckService'

const rdBase = getRundeckContext().rdBase
const project = getRundeckContext().projectName
@Component
export default class NodeFilterLink extends Vue {
  @Prop({required: false, default: ''})
  nodeFilterName!: string
  @Prop({required: false})
  nodeFilter!: string
  @Prop({required: false, default: false})
  exclude!: boolean
  @Prop({required: false, default: ''})
  filterKey!: string
  @Prop({required: false, default: ''})
  filterVal!: string
  @Prop({required: false, default: ''})
  text!: string

  handleClick() {
    this.$emit('nodefilterclick', this.filterParamValues)
  }

  getText() {
    if (this.text) {
      return this.text
    } else if (this.nodeFilterName) {
      return this.nodeFilterName
    } else if (this.filterVal) {
      return this.filterVal
    }
    return this.getFilter()
  }

  getFilter() {
    if (this.nodeFilter) {
      let nodeFilterCpy = this.nodeFilter.trim()
      let idxKey = nodeFilterCpy.indexOf(": ") + 2
      if(nodeFilterCpy.slice(idxKey).includes(" ")){
        nodeFilterCpy += "\""
        nodeFilterCpy = nodeFilterCpy.slice(0, idxKey) + "\"" + nodeFilterCpy.slice(idxKey)

        return nodeFilterCpy
      }
      return this.nodeFilter
    } else if (this.filterKey && this.filterVal) {
      return `${this.filterKey}: ${this.filterVal}`
    }
  }

  get filterParam() {
    return this.exclude ? 'filterExclude' : 'filter'
  }

  get filterParamValues() {
    let params = {[this.filterParam]: this.getFilter()}
    if (this.nodeFilterName) {
      params.filterName = this.nodeFilterName
    }
    return params
  }

  href() {
    return url(_genUrl('/project/' + project + '/nodes', Object.assign({}, this.filterParamValues)))
  }
}
</script>