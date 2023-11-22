<template>
    <div class="page-title">
        <div
            class="subtitle-head-item flex-container flex-align-items-baseline"
        >
            <div class="flex-item-auto text-h3">
                <span v-if="groupPath">
                    <job-groups-breadcrumbs
                        :groupPath="groupPath"
                        @browse-to="doRootBrowse"
                    >
                        <template #root>
                            <i class="fas fa-tasks"></i>
                        </template>
                    </job-groups-breadcrumbs>
                </span>
                <span v-else class="query-section">
                    <i class="fas fa-tasks query-item"></i>
                    <a class="link-quiet" href="#" @click="advancedSearchModalVisible = true">
                        <template v-if="wasFiltered && wasFiltered.length > 0">
                            <template v-for="qparam in wasFiltered">
                              <template v-if="jobPageStore.query[qparam]">
                                <span class="text-secondary query-item"
                                    >{{ $t(`jobquery.title.${qparam}`) }}:
                                </span>

                                <span class="text-info query-item">
                                    {{ jobPageStore.query[qparam] }}
                                </span>
                              </template>
                            </template>
                        </template>
                        <template v-else>
                            {{ $t("page.section.title.AllJobs") }}
                        </template>
                    </a>
                </span>

                <!--        <span-->
                <!--          class="label label-secondary has_tooltip"-->
                <!--          data-container="#section-content"-->
                <!--          data-placement="auto bottom"-->
                <!--          title="${totalauthorized} Jobs Found">-->
                <!--&lt;!&ndash;                        <g:enc>${totalauthorized}</g:enc>&ndash;&gt;-->
                <!--          0-->
                <!--                </span>-->
            </div>

            <div class="flex">
                <btn
                    size="md"
                    :title="$t('jobs.advanced.search.title')"
                    @click="advancedSearchModalVisible = true"
                >
                    {{ $t("advanced.search") }}
                </btn>
                <span class="search">
                    <span><b class="glyphicon glyphicon-search" /></span>
                    <input
                        type="search"
                        v-model="jobPageStore.query['jobFilter']"
                        :placeholder="$t('job.filter.quick.placeholder')"
                        class="form-control input-md"
                        @keyup.enter="doSearchQuick"
                    />
                </span>
                <btn type="primary" @click="doSearchQuick">{{
                    $t("job.filter.apply.button.title")
                }}</btn>
            </div>
        </div>
        <job-search-modal
            v-model="advancedSearchModalVisible"
            @close="advancedSearchModalVisible = false"
            @search="doSearch"
        />
    </div>
</template>

<script lang="ts">
import JobGroupsBreadcrumbs from "@/app/pages/job/browse/JobGroupsBreadcrumbs.vue";
import { getRundeckContext } from "@/library";
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, ref, inject, unref } from "vue";
import JobSearchModal from "./JobSearchModal.vue";

const eventBus = getRundeckContext().eventBus;
export default defineComponent({
    name: "JobsPageHeader",
    components: {
        JobGroupsBreadcrumbs,
        JobSearchModal,
    },

    setup(props) {
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobPageStore,
            advancedSearchModalVisible: ref(false),
            groupPath: ref(""),
            wasFiltered: ref([]),
        };
    },
    methods: {
        doSearch() {
            this.advancedSearchModalVisible = false;
            eventBus.emit("job-search-modal:search");
            this.updateFilters();
        },
        doSearchQuick() {
            this.doSearch();
        },
        doRootBrowse(path: string) {
            eventBus.emit("job-list-page:rootBrowse", path);
        },
        updateFilters() {
            let keys = Object.keys(this.jobPageStore.query).filter(
                (key) => key !== "groupPath" && this.jobPageStore.query[key]
            );
            this.wasFiltered = keys;
        },
    },
    mounted() {
        eventBus.on("job-list-page:browsed", (path: string) => {
            this.groupPath = path;
        });
    },
});
</script>

<style scoped lang="scss">
.page-title {
    padding: var(--spacing-8);
    padding-bottom: 0;
}

.query-section {
    .query-item {
        margin-right: var(--spacing-2);
    }
}
</style>