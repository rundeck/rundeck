<template>
  <a :href="href" class="xnodefilterlink" @click.prevent="handleClick">
    <slot name="prefix"></slot>
    <slot>{{ getText() }}</slot>
    <slot name="suffix"></slot>
  </a>
</template>
<script lang="ts">

import {_genUrl} from '../../../utilities/genUrl'
import { defineComponent } from 'vue'

import {
  getRundeckContext,
  url
} from '../../../../library'

const rdBase = getRundeckContext().rdBase
const project = getRundeckContext().projectName

export default defineComponent({
  name: 'NodeFilterLink',
  props: {
    nodeFilterName: {
      type: String,
      required: false,
      default: '',
    },
    nodeFilter: {
      type: String,
      required: false,
      default: '',
    },
    exclude: {
      type: Boolean,
      required: false,
      default: false,
    },
    filterKey: {
      type: String,
      required: false,
      default: '',
    },
    filterVal: {
      type: String,
      required: false,
      default: '',
    },
    text: {
      type: String,
      required: false,
      default: '',
    },
  },
  emits: ['nodefilterclick'],
  methods: {
    handleClick() {
      this.$emit('nodefilterclick', this.filterParamValues)
    },

    getText() {
      if (this.text) {
        return this.text
      } else if (this.nodeFilterName) {
        return this.nodeFilterName
      } else if (this.filterVal) {
        return this.filterVal
      }
      return this.getFilter()
    },

    getFilter() {
      if (this.nodeFilter) {
        let nodeFilterCpy = this.nodeFilter.trim()
        let idxKey = nodeFilterCpy.indexOf(": ") + 2
        if (nodeFilterCpy.slice(idxKey).includes(" ")) {
          nodeFilterCpy += "\""
          nodeFilterCpy = nodeFilterCpy.slice(0, idxKey) + "\"" + nodeFilterCpy.slice(idxKey)

          return nodeFilterCpy
        }
        return this.nodeFilter
      } else if (this.filterKey && this.filterVal) {
        return `${this.filterKey}: ${this.filterVal}`
      }
    },
  },
  computed: {
    filterParam() {
      return this.exclude ? 'filterExclude' : 'filter'
    },
    filterParamValues() {
      let params = {[this.filterParam]: this.getFilter()}
      if (this.nodeFilterName) {
        params[this.filterParam] = this.nodeFilterName
      }
      return params
    },
    href() {
      return url(_genUrl('/project/' + project + '/nodes', Object.assign({}, this.filterParamValues))).href
    }
  },
})
</script>