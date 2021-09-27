<template>
  <span class="col-xs-6">
  <popover :class="{server:node.islocal}" trigger="click">

  <a
      data-role="trigger"
      class="node_ident embedded_node tight"
      tabindex="0"
      role="button"
      data-viewport="#section-content"
      data-placement="auto"
      data-container="body"
      data-delay="{&quot;show&quot;:0,&quot;hide&quot;:200}"
      data-popover-template-class="popover-wide"
      :style="styleForNode(node.attributes)"
      :data-node="node.nodename"
      :title="node.nodename"
  >
    <node-icon :node="node" :default-icon-css="resourceTypeDefinition.defaultIconCss"/>

    <span style="margin: 0 0.5em" :class="{'node_unselected':node.unselected}">{{ node.nodename }}</span>


    <node-status :node="node"/>
  </a>
    <template slot="popover">
      <div class="_mousedown_popup_allowed detailpopup node_entry tooltipcontent">

          <node-icon :node="node"/>

          <span>{{ node.nodename }}</span>

          <node-filter-link :node-filter="`name: ${node.nodename} `" @nodefilterclick="filterClick">
            <i class="glyphicon glyphicon-circle-arrow-right"/>
          </node-filter-link>

        <span class="nodedesc"></span>

        <div class="nodedetail" style="overflow-x: scroll;">

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

import NodeDetailsSimple from '@/components/job/resources/NodeDetailsSimple.vue'
import NodeFilterLink from '@/components/job/resources/NodeFilterLink.vue'
import NodeIcon from '@/components/job/resources/NodeIcon.vue'
import NodeStatus from '@/components/job/resources/NodeStatus.vue'

import {getRundeckContext} from '@rundeck/ui-trellis'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'
import {styleForNode} from '@/utilities/nodeUi'

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