<template>
  <DynamicScroller
    ref="scroller"
    :items="projects"
    :min-item-size="43"
    key-field="name"
    item-tag="div"
  >
    <template #default="{ item, index, active }">
      <DynamicScrollerItem
        :item="item"
        :index="index"
        :active="active"
        :size-dependecies="[item.description, item.meta]"
      >
        <HomeBrowserItem
          :project="item"
          :index="getProjectIndex(item)"
          :loaded="loaded"
        />
      </DynamicScrollerItem>
    </template>
  </DynamicScroller>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { DynamicScroller, DynamicScrollerItem } from "vue-virtual-scroller";
import "vue-virtual-scroller/dist/vue-virtual-scroller.css";
import HomeBrowserItem from "./HomeBrowserItem.vue";
import { Project } from "./types/projectTypes";

export default defineComponent({
  name: "HomeBrowser",
  components: { HomeBrowserItem, DynamicScroller, DynamicScrollerItem },
  props: {
    projects: {
      type: Array as PropType<Project[]>,
      required: true,
    },
    allProjects: {
      type: Array as PropType<Project[]>,
      default: () => [] as Project[],
    },
    loaded: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    getProjectIndex(project: Project) {
      if (!this.allProjects) {
        return this.projects.findIndex((p) => p.name === project.name);
      }
      // dynamic scroller loses the reference of the index when the items prop (projects) changes.
      // so we need to find the index of the project in the allProjects array,
      // as this array won't be mutated by any action in the view.
      return this.allProjects.findIndex((p) => p.name === project.name);
    },
  },
});
</script>
