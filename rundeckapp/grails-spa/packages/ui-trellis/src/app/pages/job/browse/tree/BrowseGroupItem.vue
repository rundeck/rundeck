<template>
    <div class="group-control">
        <btn
            @click="$emit('toggleExpanded', item.groupPath)"
            class="btn-link group-name jobgroupexpand text-secondary"
            size="sm"
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
        <btn
            class="btn-link groupname text-strong group-name"
            size="sm"
            :title="`Browse job group: ${item.groupPath}`"
            @click="$emit('rootBrowse', item.groupPath)"
        >
            <i class="glyphicon glyphicon-folder-close"></i>
        </btn>
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
    },
    methods: {
        lastPathItem(path: string) {
            const parts = path.split("/");
            return parts[parts.length - 1];
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
</style>
