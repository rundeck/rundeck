<template>
    <div class="page-title">
        <div
            class="subtitle-head-item flex-container flex-align-items-baseline"
        >
            <div class="flex-item-auto text-h3 jobs-page-header">
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
                    <a
                        class="link-quiet"
                        href="#"
                        @click="advancedSearchModalVisible = true"
                    >
                        <template v-if="wasFiltered && wasFiltered.length > 0">
                            <template v-if="jobPageStore.selectedFilter">
                                <i class="glyphicon glyphicon-filter" />
                                {{ jobPageStore.selectedFilter }}
                            </template>
                            <template v-for="qparam in wasFiltered" v-else>
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
                <job-page-filters-popup
                    @select="selectedFilter"
                    @delete="deleteSelectedFilter"
                />
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
            @clear="doClear"
            @save="doSave"
        />
        <job-filter-save-modal
            v-model="saveFilterModal"
            :error="saveFilterError"
            @save="doFinishSave"
        />
    </div>
</template>

<script lang="ts">
import JobFilterSaveModal from "@/app/pages/job/browse/JobFilterSaveModal.vue";
import JobGroupsBreadcrumbs from "@/app/pages/job/browse/JobGroupsBreadcrumbs.vue";
import JobPageFiltersPopup from "@/app/pages/job/browse/JobPageFiltersPopup.vue";
import { getRundeckContext } from "@/library";
import {
    JobListFilterStore,
    JobListFilterStoreInjectionKey,
} from "@/library/stores/JobListFilterStore";
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
        JobFilterSaveModal,
        JobPageFiltersPopup,
    },
    props: {
        queryParams: {
            type: Object,
            default: () => {
                return {};
            },
        },
    },
    setup(props) {
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        const jobListFilterStore: JobListFilterStore = inject(
            JobListFilterStoreInjectionKey
        ) as JobListFilterStore;
        return {
            jobPageStore,
            jobListFilterStore,
            advancedSearchModalVisible: ref(false),
            groupPath: ref(props.queryParams?.groupPath || ""),
            wasFiltered: ref([]),
            saveFilterModal: ref(false),
            saveFilterError: ref(""),
        };
    },
    methods: {
        doSearch() {
            this.advancedSearchModalVisible = false;
            eventBus.emit("job-list-page:search");
        },
        doClear() {
            this.jobPageStore.query = {};
            this.doSearch();
        },
        doSave() {
            this.advancedSearchModalVisible = false;
            this.saveFilterModal = true;
        },
        async doFinishSave(name: string) {
            if (this.jobListFilterStore.hasFilter(name)) {
                this.saveFilterError = this.$t(
                    "job.list.filter.save.error.exists",
                    { name }
                ).toString();
                return;
            }
            this.saveFilterModal = false;
            this.saveFilterError = "";
            this.jobListFilterStore.saveFilter({
                name,
                query: this.jobPageStore.query,
            });
            this.selectedFilter(name);
        },
        selectedFilter(name: string) {
            let filter = this.jobListFilterStore.getFilter(name);
            if (!filter) {
                return;
            }
            this.jobPageStore.query = Object.assign({}, filter.query);
            eventBus.emit("job-list-page:search", name);
        },
        deleteSelectedFilter(name: string) {
            let filter = this.jobListFilterStore.getFilter(name);
            if (!filter) {
                return;
            }
            this.jobListFilterStore.deleteFilter(name);
            this.jobPageStore.selectedFilter = "";
        },
        doSearchQuick() {
            this.doSearch();
        },
        doRootBrowse(path: string, href: string) {
            eventBus.emit("job-list-page:rootBrowse", { path, href });
        },
        updateFilters(name: string) {
            let keys = Object.keys(this.jobPageStore.query).filter(
                (key) => key !== "groupPath" && this.jobPageStore.query[key]
            );
            this.wasFiltered = keys;
            if (name) {
                this.jobPageStore.selectedFilter = name;
            } else {
                this.jobPageStore.selectedFilter = "";
            }
        },
    },
    mounted() {
        eventBus.on("job-list-page:browsed", (path: string) => {
            this.groupPath = path;
        });
        eventBus.on("job-list-page:search", (name: string) => {
            this.updateFilters(name);
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
.jobs-page-header {
    .query-section + .btn-group,
    .query-section + .btn {
        margin-left: var(--spacing-2);
    }
}
</style>