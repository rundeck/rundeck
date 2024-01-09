<template>
  <span class="col-xs-6">
  <popover trigger="outside-click"
           :show-delay="0"
           :hide-delay="200"
           placement="auto"
           append-to="body"
           viewport="#section-content"
           :custom-class="`node-embed-popover ${node.islocal?'server':''}`"
  >

  <a
      data-role="trigger"
      class="node_ident embedded_node tight"
      tabindex="0"
      role="button"
      :style="styleForNode(node.attributes)"
      :data-node="node.nodename"
      :title="node.nodename"
  >
    <node-icon :node="node"/>

    <span style="margin: 0 0.5em" :class="{'node_unselected':node.unselected}">{{ node.nodename }}</span>


    <node-status :node="node"/>
  </a>
    <template v-slot:popover>
      <div class=" detailpopup node_entry tooltipcontent" :class="{server:node.islocal}">
        <div class="node-header">

          <node-icon :node="node"/>

          <span>{{ node.nodename }}</span>

          <node-filter-link :node-filter="`name: ${node.nodename} `" @nodefilterclick="filterClick">
            <i class="glyphicon glyphicon-circle-arrow-right"/>
          </node-filter-link>

          <span class="nodedesc"></span>
        </div>

        <div class="nodedetail" style="overflow-x: auto;">

          <node-details-simple :attributes="node.attributes"
                               :show-exclude-filter-links="showExcludeFilterLinks"
                               :authrun="node.authrun"
                               :use-namespace="true"
                               :tags="node.tags"
                               :node-columns="true"
                               @filter="filterClick"/>
        </div>
      </div>
    </template>
  </popover>
    </span>
</template>
<script lang="ts">

import NodeDetailsSimple from '../../job/resources/NodeDetailsSimple.vue'
import NodeFilterLink from '../../job/resources/NodeFilterLink.vue'
import NodeIcon from '../../job/resources/NodeIcon.vue'
import NodeStatus from '../../job/resources/NodeStatus.vue'

import {getRundeckContext} from '../../../../library'
import {defineComponent} from 'vue'
import type { PropType } from "vue"
import {styleForNode} from '../../../utilities/nodeUi'

const rdBase = getRundeckContext().rdBase
const project = getRundeckContext().projectName

export default defineComponent({
  name: 'NodeShowEmbed',
  components: {
    NodeStatus,
    NodeIcon,
    NodeDetailsSimple,
    NodeFilterLink,
  },
  props: {
    node: {
      type: Object as PropType<any>,
      required: true,
    },
    showExcludeFilterLinks: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  emits: ['filter'],
  methods: {
    styleForNode(node: any) {
      return styleForNode(node)
    },
    filterClick(filter: any) {
      this.$emit('filter', filter)
    },
  }
})
</script>
<style lang="scss">
.popover.node-embed-popover {
  max-width: 500px;
}

.node-header {
  display: flex;
  justify-content: flex-start;

  span,a{
    flex: initial;
    margin-right: 0.5em;
    &.nodedesc{
      flex:auto;
    }
  }
}
</style>