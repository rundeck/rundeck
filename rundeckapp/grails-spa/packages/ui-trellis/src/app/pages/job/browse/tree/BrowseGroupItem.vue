<template>
  <div
    class="job_list_group_header hover-reveal-hidden"
    @click="handleClick"
    ref="itemDiv"
  >
    <btn
      @click="$emit('toggleExpanded', item.groupPath)"
      type="link"
      class="group-name text-secondary"
    >
      <i
        class="glyphicon"
        :class="{
          'glyphicon-chevron-right': !expanded,
          'glyphicon-chevron-down': expanded,
        }"
      ></i>
      {{ lastPathItem(item.groupPath) }}
    </btn>
    <a
      class="groupname text-strong group-name visibility-hidden"
      :title="`Browse job group: ${item.groupPath}`"
      @click.prevent="$emit('rootBrowse', item.groupPath)"
      :href="href"
    >
      <i class="glyphicon glyphicon-folder-open"></i>
    </a>
    <slot name="supplemental" />
  </div>
</template>

<script lang="ts">
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent } from "vue";

export default defineComponent({
  name: "BrowseGroupItem",
  emits: ["rootBrowse", "toggleExpanded"],
  props: {
    item: {
      type: JobBrowseItem,
      required: true,
    },
    expanded: {
      type: Boolean,
      default: false,
    },
    href: {
      type: String,
      default: "",
    },
  },
  methods: {
    lastPathItem(path: string) {
      const parts = path.split("/");
      return parts[parts.length - 1];
    },
    handleClick(event) {
      if (event.target == this.$refs.itemDiv) {
        //only emit if the click was on the item div to avoid case when clicking on inputs/buttons
        this.$emit("toggleExpanded", this.item.groupPath);
      }
    },
  },
});
</script>

<style scoped lang="scss">
.btn.group-name {
  padding: 0;
}

.btn.btn-link.text-secondary {
  color: var(--text-secondary-color);
}
.job_list_group_header {
  padding: 3px;
  > .btn {
    margin-right: var(--spacing-2);
  }
}
</style>
