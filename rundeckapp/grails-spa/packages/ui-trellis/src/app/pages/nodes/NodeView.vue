

<template>
  <div class="title">
    <span class="text-h3"><i class="fas fa-sitemap"></i> {{ $t('gui.menu.Nodes')}}</span>
  </div>
  <div style="margin-bottom:20px">
    <slot />
  </div>
  <div id="nodesContent">
    <div class="container-fluid">
      <div class="row">
        <div class="col-xs-12">
          <NodeCard :run-authorized="itemData.runAuthorized" :job-create-authorized="itemData.jobCreateAuthorized" />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import type {PropType} from 'vue'
import {defineComponent} from 'vue'
import {EventBus} from "@rundeck/ui-trellis";
import NodeCard from "@/app/components/job/resources/NodeCard.vue";

export default defineComponent({
  name: "NodeView",
  components: {
    NodeCard
  },
  props: {
    itemData: {
      type: Object,
      required: true,
    },
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: false
    },
  },
})
</script>