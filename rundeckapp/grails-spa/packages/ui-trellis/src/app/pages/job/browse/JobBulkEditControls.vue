<template>
    <div >
        <div class="flex col flex-justify-end">
            <div class="bulk_edit_controls">
                <dropdown>
                    <btn class="dropdown-toggle mr-2">
                        {{ $t("job.actions") }}
                        <span class="caret"></span>
                    </btn>
                    <template #dropdown>
                        <template v-if="projAuthz('create')">
                            <li>
                                <a :href="uploadJobHref">
                                    <i class="glyphicon glyphicon-upload"></i>
                                    {{ $t("upload.definition.button.label") }}
                                </a>
                            </li>
                            <li class="divider"></li>
                        </template>
                        <li>
                            <a
                                @click="jobPageStore.bulkEditMode = !jobPageStore.bulkEditMode"
                                role="button"
                                >
                              {{ $t(jobPageStore.bulkEditMode?"job.bulk.deactivate.menu.label":"job.bulk.activate.menu.label") }}
                            </a>
                        </li>
                    </template>
                </dropdown>
                <ui-socket section="job-list-page" location="action-buttons">
                    <a :href="newJobHref" class="btn btn-primary" v-if="projAuthz('create')">
                        <i class="glyphicon glyphicon-plus"></i>
                        {{ $t("new.job.button.label") }}
                    </a>
                </ui-socket>
            </div>
        </div>
        <div v-if="jobPageStore.bulkEditMode">
            <div class="panel panel-warning">
                <div class="panel-heading">
                    <btn
                        class="close"
                        @click="jobPageStore.bulkEditMode = false"
                        aria-hidden="true"
                        >&times;
                    </btn>
                    <h3 class="panel-title">
                        {{ $t("job.bulk.panel.select.title") }}
                    </h3>
                </div>
                <div class="panel-body">
                    <btn
                        size="xs"
                        type="simple"
                        class="btn-hover"
                        @click="selectAll"
                    >
                        <b class="glyphicon glyphicon-check"></b>
                        {{ $t("select.all") }}
                    </btn>
                    <btn
                        size="xs"
                        type="simple"
                        class="btn-hover"
                        @click="selectNone"
                    >
                        <b class="glyphicon glyphicon-unchecked"></b>
                        {{ $t("select.none") }}
                    </btn>
                    {{
                        $tc(
                            "job.bulk.panel.select.message",
                            jobPageStore.selectedJobs.length
                        )
                    }}
                </div>

                <div class="panel-footer">
                    <dropdown>
                        <btn size="sm" class="dropdown-toggle">
                            {{ $t("job.bulk.perform.action.menu.label") }}
                            <span class="caret"></span>
                        </btn>
                        <template #dropdown>
                            <li v-if="projAuthz('delete')">
                                <a @click="bulkAction('delete')">
                                    {{ $t("delete.selected.jobs") }}
                                </a>
                            </li>
                            <li class="divider"></li>
                            <li v-for="action in ['enable', 'disable']">
                                <a @click="bulkAction(`${action}_schedule`)">
                                    {{
                                        $t(`job.bulk.${action}_schedule.button`)
                                    }}
                                </a>
                            </li>
                            <li class="divider"></li>
                            <li v-for="action in ['enable', 'disable']">
                                <a @click="bulkAction(`${action}_execution`)">
                                    {{
                                        $t(
                                            `job.bulk.${action}_execution.button`
                                        )
                                    }}
                                </a>
                            </li>
                        </template>
                    </dropdown>
                </div>
            </div>
        </div>
        <modal
            id="bulk_confirm_modal"
            ref="bulk_confirm_modal"
            v-model="bulkConfirm"
            :title="$t('job.bulk.modify.confirm.panel.title')"
        >
            <p>{{ $t(`job.bulk.${bulkConfirmAction}.confirm.message`) }}</p>
            <p>
              {{
                $tc(
                    "job.bulk.panel.select.message",
                    jobPageStore.selectedJobs.length
                )
            }}
            </p>
            <template #footer>
                <btn @click="bulkConfirm = false">{{ $t("no") }}</btn>
                <btn type="danger" @click="performBulkAction">
                    {{ $t(`job.bulk.${bulkConfirmAction}.button`) }}
                </btn>
            </template>
        </modal>
    </div>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import {
    JobBrowserStore,
    JobBrowserStoreInjectionKey,
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobBrowser";
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";
import UiSocket from '@/library/components/utils/UiSocket.vue'

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
    name: "JobBulkEditControls",
    components:{UiSocket},
    setup(props) {
        const jobBrowserStore: JobBrowserStore = inject(
            JobBrowserStoreInjectionKey
        ) as JobBrowserStore;
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobBrowserStore,
            jobPageStore,
            bulkConfirm: ref(false),
            bulkConfirmAction: ref(""),
        };
    },
    methods: {
        bulkAction(name: string) {
            this.confirmAction({ name });
        },
        confirmAction(evt: { name: string }) {
            const { name } = evt;
            this.bulkConfirmAction = name;
            this.bulkConfirm = true;
        },
        confirmActionSingle(evt: { name: string; job: JobBrowseItem }) {
            const { name, job } = evt;
            this.jobPageStore.selectedJobs = [job];
            this.jobPageStore.bulkEditMode = true;
            this.confirmAction({name})
        },
        async performBulkAction() {
            await this.jobPageStore.performBulkAction(this.bulkConfirmAction);
            this.jobPageStore.selectedJobs = [];
            this.bulkConfirm = false;
            this.jobPageStore.bulkEditMode = false;
        },
        projAuthz(action: string): boolean {
            return this.jobPageStore.authz?.[action];
        },
        selectAll() {
            eventBus.emit("job-bulk-edit-select-all");
        },
        selectNone() {
            this.jobPageStore.selectedJobs = [];
        },
    },
    computed: {
        newJobHref() {
            return `${context.rdBase}project/${context.projectName}/job/create`;
        },
        uploadJobHref() {
            return `${context.rdBase}project/${context.projectName}/job/upload`;
        },
    },
    mounted() {
        eventBus.on("job-action", this.confirmAction);
        eventBus.on("job-action-single", this.confirmActionSingle);
    },
});
</script>

<style scoped lang="scss">
.bulk_edit_controls{
  margin-bottom: var(--spacing-8)
}
</style>