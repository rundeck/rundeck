<template>
      <DynamicScroller
          v-if="projects.length > 0"
        ref="scroller"
        :items="projects"
        :min-item-size="43"
        key-field="created"
        item-tag="div"
        page-mode
      >

        <template #default="{ item, index, active }">
          <DynamicScrollerItem v-if="active" :item="item" :active="active" :size-dependecies="[item.description, item.meta]" :index="index">
            <HomeBrowserItem :project="item" :index="index" />
          </DynamicScrollerItem>
        </template>
      </DynamicScroller>
</template>

<script lang="ts">
import {defineComponent, PropType} from "vue";
import { DynamicScroller, DynamicScrollerItem } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
import HomeBrowserItem from "./HomeBrowserItem.vue";
import {Project} from "./types/projectTypes";

export default defineComponent({
  name: "HomeBrowser",
  components: { HomeBrowserItem, DynamicScroller, DynamicScrollerItem },
  props: {
    projects: {
      type: Array as PropType<Project[]>,
      required: true,
    },
  },
});
</script>
