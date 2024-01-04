<template>
  <div>
    <ul class="list-unstyled">
      <DynamicScroller
        ref="scroller"
        :items="projects"
        :min-item-size="43"
        #default="{ item, index, active }"
        key-field="created"
        item-tag="li"
        page-mode
      >
        <DynamicScrollerItem :item="item" :active="active" :size-dependecies="[item.description]" :data-index="index">
          <home-browser-item :project="item" :index="index" />
        </DynamicScrollerItem>
      </DynamicScroller>
    </ul>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { DynamicScroller, DynamicScrollerItem } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
import HomeBrowserItem from "@/app/components/home/HomeBrowserItem.vue";

export default defineComponent({
  name: "HomeBrowser",
  components: { HomeBrowserItem, DynamicScroller, DynamicScrollerItem },
  props: {
    projects: {
      type: Array,
      required: true,
    },
  },
});
</script>
