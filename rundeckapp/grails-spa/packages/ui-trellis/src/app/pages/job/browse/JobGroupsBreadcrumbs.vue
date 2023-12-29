<template>
    <section class="job-breadcrumbs">
        <span class="job-breadcrumb-item">
            <a @click.prevent="browsePath('')" :href="browseHref('')">
                <slot name="root">&larr;</slot>
            </a>
        </span>
        <template v-for="(part, i) in parts">
            <span class="breadcrumb-separator job-breadcrumb-item" v-if="i != 0">
              <slot name="separator">/</slot>
            </span>

            <a
                :class="linkCss"
                class="job-breadcrumb-item"
                :title="$t('view.jobs.in.this.group')"
                @click.prevent="browsePath(subgroup(i))"
                :href="browseHref(subgroup(i))"
            >
                <b class="glyphicon glyphicon-folder-close" v-if="i == 0"></b>

                {{ part }}
            </a>
        </template>
    </section>
</template>

<script lang="ts">
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, inject } from "vue";

export default defineComponent({
    name: "JobGroupsBreadcrumbs",
    props: {
        groupPath: String,
        linkCss: String,
    },
    emits: ["browseTo"],

    setup(props) {
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobPageStore,
        };
    },
    methods: {
        subgroup(i: number) {
            return this.parts.slice(0, i + 1).join("/");
        },
        browseHref(path: string) {
            return this.jobPageStore.jobPagePathHref(path);
        },
        browsePath(path: string) {
            this.$emit("browseTo", path, this.browseHref(path));
        },
    },
    computed: {
        parts() {
            return this.groupPath.split("/");
        },
    },
});
</script>

<style scoped lang="scss">
.breadcrumb-separator {
    color: var(--text-secondary-color);
}

.job-breadcrumbs {
    .job-breadcrumb-item {
        margin-right: var(--spacing-4);
    }
}
</style>
