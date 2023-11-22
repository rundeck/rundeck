<template>
    <div class="page-title">
        <div
            class="subtitle-head-item flex-container flex-align-items-baseline"
        >
            <div class="flex-item-auto text-h3">
                <i class="fas fa-tasks"></i>

                <!--        <g:if test="${wasfiltered && wasfiltered.contains('groupPath') }">-->
                <!--          <g:render template="/scheduledExecution/groupBreadcrumbs" model="[groupPath:paginateParams.groupPath,project:params.project]"/>-->

                <!--        </g:if>-->
                <a class="link-quiet" href="#">
                    <!--          <g:if test="${wasfiltered}">-->

                    <!--            <g:if test="${wasfiltered.contains('groupPath') && wasfiltered.size()>1 || wasfiltered.size()>0 }">-->

                    <!--                            <span class="query-section">-->
                    <!--                                <g:each in="${wasfiltered.sort()}" var="qparam">-->
                    <!--                                    <g:if test="${qparam!='groupPath'}">-->
                    <!--                                        <g:if test="${paginateParams[qparam] instanceof Map}">-->
                    <!--                                            <g:each in="${paginateParams[qparam]}" var="customParam">-->
                    <!--                                                <span class="text-secondary">${customParam.key}:</span>-->
                    <!--                                                <span class="text-info">${customParam.value}</span>-->
                    <!--                                            </g:each>-->
                    <!--                                        </g:if>-->
                    <!--                                        <g:else>-->
                    <!--                                            <span class="text-secondary"><g:message code="jobquery.title.${qparam}"/>:</span>-->

                    <!--                                            <span class="text-info">-->
                    <!--                                                ${g.message(code:'jobquery.title.'+qparam+'.label.'+paginateParams[qparam].toString(),default:enc(html:paginateParams[qparam].toString()).toString())}-->
                    <!--                                            </span>-->
                    <!--                                        </g:else>-->

                    <!--                                    </g:if>-->
                    <!--                                </g:each>-->
                    <!--                            </span>-->

                    <!--            </g:if>-->
                    <!--          </g:if>-->
                    <!--          <g:else>-->
                    <!--            All Jobs-->
                    <!--          </g:else>-->

                    {{ $t("page.section.title.AllJobs") }}
                </a>

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
import { getRundeckContext } from "@/library";
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, ref, inject } from "vue";
import JobSearchModal from "./JobSearchModal.vue";

const eventBus = getRundeckContext().eventBus;
export default defineComponent({
    name: "JobsPageHeader",
    components: {
        JobSearchModal,
    },

    setup(props) {
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobPageStore,
            totalauthorized: ref(0),
            wasfiltered: ref([]),
            advancedSearchModalVisible: ref(false),
        };
    },
    methods: {
        doSearch() {
            this.advancedSearchModalVisible = false;
            eventBus.emit("job-search-modal:search");
        },
        doSearchQuick(){
            this.doSearch()
        }
    },
});
</script>

<style scoped lang="scss">
.page-title {
    padding: var(--spacing-8);
    padding-bottom: 0;
}
</style>
