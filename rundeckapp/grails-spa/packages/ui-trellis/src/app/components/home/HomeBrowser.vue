<template>
    <ul class="list-unstyled">
      <DynamicScroller
        ref="scroller"
        :items="projects"
        :min-item-size="43"
        key-field="created"
        item-tag="li"
        page-mode
      >
        <template #default="{ item, index, active }">
          <DynamicScrollerItem v-if="active" :item="item" :active="active" :size-dependecies="[item.description]" :index="index">
            <HomeBrowserItem :project="item" :index="index" />
          </DynamicScrollerItem>
        </template>

      </DynamicScroller>
    </ul>
</template>

<script lang="ts">
import {defineComponent, PropType} from "vue";
import { DynamicScroller, DynamicScrollerItem } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
import HomeBrowserItem from "@/app/components/home/HomeBrowserItem.vue";
import {Project} from "@/app/components/home/types/projectTypes";

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
