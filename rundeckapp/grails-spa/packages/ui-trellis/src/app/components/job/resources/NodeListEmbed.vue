<template>
  <div>
    <div class="nodes-embed ansicolor-on matchednodes embed embed_clean">

      <div class="row">
        <template v-for="(node,i) in nodes">
          <node-show-embed :node="node"
                           :class="cssForNode(node.attributes)"
                           :show-exclude-filter-links="showExcludeFilterLinks"
                           @filter="filterClick"
          />
        </template>
      </div>
    </div>
    <slot></slot>

  </div>
</template>
<script lang="ts">

import NodeDetailsSimple from '../../job/resources/NodeDetailsSimple.vue'
import NodeFilterLink from '../../job/resources/NodeFilterLink.vue'
import NodeIcon from '../../job/resources/NodeIcon.vue'
import NodeShowEmbed from '../../job/resources/NodeShowEmbed.vue'
import NodeStatus from '../../job/resources/NodeStatus.vue'

import {getRundeckContext} from '../../../../library'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'
import {cssForNode, styleForNode} from '../../../utilities/nodeUi'

const rdBase = getRundeckContext().rdBase
const project = getRundeckContext().projectName

@Component({
  components: {NodeShowEmbed, NodeStatus, NodeIcon, NodeDetailsSimple, NodeFilterLink}
})
export default class NodeListEmbed extends Vue {
  @Prop({required: true})
  nodes!: Array<any>
  @Prop({
    required: false, default: () => {
    }
  })
  tagsummary!: any
  @Prop({required: false, default: false})
  showExcludeFilterLinks!: boolean

  cssForNode(node: any) {
    return cssForNode(node, this.nodes)
  }

  styleForNode(node: any) {
    return styleForNode(node)
  }

  filterClick(filter: any) {
    this.$emit('filter', filter)
  }

}
</script>