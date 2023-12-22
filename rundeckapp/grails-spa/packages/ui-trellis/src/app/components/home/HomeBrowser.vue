<template>
  <div>
    <ul class="list-unstyled">
      <DynamicScroller
        ref="scroller"
        :items="sortedItems"
        :min-item-size="43"
        #default="{ item, active }"
        key-field="id"
        itemTag="li"
        page-mode
        :key="browsePath"
      >
<!--        <browser-job-item-->
<!--          :job="item"-->
<!--          v-if="item.job"-->
<!--          :load-meta="breakpointHit"-->
<!--        />-->
      </DynamicScroller>
    </ul>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { DynamicScroller } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
// import BrowserJobItem from "@/app/pages/job/browse/tree/BrowserJobItem.vue";

export default defineComponent({
  name: "HomeBrowser",
  components: { DynamicScroller },
});
</script>

<style scoped lang="scss"></style>