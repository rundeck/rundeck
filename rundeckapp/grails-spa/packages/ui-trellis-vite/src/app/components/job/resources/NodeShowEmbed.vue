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
    <template slot="popover">
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

import NodeDetailsSimple from '@/app/components/job/resources/NodeDetailsSimple.vue'
import NodeFilterLink from '@/app/components/job/resources/NodeFilterLink.vue'
import NodeIcon from '@/app/components/job/resources/NodeIcon.vue'
import NodeStatus from '@/app/components/job/resources/NodeStatus.vue'

import {getRundeckContext} from '@/library'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'
import {styleForNode} from '@/app/utilities/nodeUi'

const rdBase = getRundeckContext().rdBase
const project = getRundeckContext().projectName

@Component({
  components: {NodeStatus, NodeIcon, NodeDetailsSimple, NodeFilterLink}
})
export default class NodeShowEmbed extends Vue {
  @Prop({required: true})
  node!: Array<any>
  @Prop({required: false, default: false})
  showExcludeFilterLinks!: boolean

  styleForNode(node: any) {
    return styleForNode(node)
  }

  filterClick(filter: any) {
    this.$emit('filter', filter)
  }
}
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