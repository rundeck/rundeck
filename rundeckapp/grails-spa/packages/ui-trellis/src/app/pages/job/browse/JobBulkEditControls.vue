<template>
  <div class="flex col justify-between">
    <div class="flex col flex-justify-start">
      <ui-socket section="job-list-page" location="card-header" />
    </div>
    <div class="flex col flex-justify-end">
      <div class="bulk_edit_controls">
        <ui-socket section="job-list-page" location="status-item" />

        <job-list-scm-status class="status-item" />

        <dropdown id="project_job_actions">
          <btn class="dropdown-toggle mr-2">
            {{ $t("job.actions") }}
            <span class="caret"></span>
          </btn>
          <template #dropdown>
            <template v-if="projAuthz('create')">
              <li>
                <a :href="jobPageStore.uploadJobHref()">
                  <i class="glyphicon glyphicon-upload"></i>
                  {{ $t("upload.definition.button.label") }}
                </a>
              </li>
              <li class="divider"></li>
            </template>
            <li>
              <a id="project_job_actions_bulk_edit"
                role="button"
                @click="jobPageStore.bulkEditMode = !jobPageStore.bulkEditMode"
              >
                {{
                  $t(
                    jobPageStore.bulkEditMode
                      ? "job.bulk.deactivate.menu.label"
                      : "job.bulk.activate.menu.label",
                  )
                }}
              </a>
            </li>
            <job-list-scm-actions />
            <ui-socket
              location="job-list-actions-menu"
              section="extended"
            ></ui-socket>
          </template>
        </dropdown>
        <ui-socket section="job-list-page" location="action-buttons">
          <create-new-job-button />
        </ui-socket>
      </div>
    </div>
    <div v-if="jobPageStore.bulkEditMode">
      <div class="panel panel-warning bulk_edit_panel">
        <div class="panel-heading">
          <btn
            class="close"
            aria-hidden="true"
            @click="jobPageStore.bulkEditMode = false"
            >&times;
          </btn>
          <h3 class="panel-title">
            {{ $t("job.bulk.panel.select.title") }}
          </h3>
        </div>
        <div class="panel-body">
          <btn id="bulk_select_all_button" size="xs" type="simple" class="btn-hover" @click="selectAll">
            <b class="glyphicon glyphicon-check"></b>
            {{ $t("select.all") }}
          </btn>
          <btn id="bulk_select_none_button" size="xs" type="simple" class="btn-hover" @click="selectNone">
            <b class="glyphicon glyphicon-unchecked"></b>
            {{ $t("select.none") }}
          </btn>
          {{
            $tc(
              "job.bulk.panel.select.message",
              jobPageStore.selectedJobs.length,
            )
          }}
        </div>

        <div class="panel-footer">
          <dropdown >
            <btn id="bulk_perform_action_button"
              size="sm"
              class="dropdown-toggle"
              :disabled="jobPageStore.selectedJobs.length < 1"
            >
              {{ $t("job.bulk.perform.action.menu.label") }}
              <span class="caret"></span>
            </btn>
            <template #dropdown>
              <li v-if="projAuthz('delete')">
                <a id="bulk_delete_jobs_action" role="button" @click="bulkAction('delete')">
                  <b class="glyphicon glyphicon-remove-circle"></b>
                  {{ $t("delete.selected.jobs") }}
                </a>
              </li>
              <li class="divider"></li>
              <li v-for="action in ['enable', 'disable']">
                <a  :id="`bulk_${action}_schedules_action`" role="button" @click="bulkAction(`${action}_schedule`)">
                  <b
                    class="glyphicon"
                    :class="
                      action === 'enable'
                        ? 'glyphicon-check'
                        : 'glyphicon-unchecked'
                    "
                  ></b>
                  {{ $t(`job.bulk.${action}_schedule.button`) }}
                </a>
              </li>
              <li class="divider"></li>
              <li v-for="action in ['enable', 'disable']">
                <a :id="`bulk_${action}_execution_action`" role="button" @click="bulkAction(`${action}_execution`)">
                  <b
                    class="glyphicon"
                    :class="
                      action === 'enable'
                        ? 'glyphicon-check'
                        : 'glyphicon-unchecked'
                    "
                  ></b>
                  {{ $t(`job.bulk.${action}_execution.button`) }}
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
          $tc("job.bulk.panel.select.message", jobPageStore.selectedJobs.length)
        }}
      </p>
      <template #footer>
        <btn id="bulk_confirm_action_no_button"  @click="bulkConfirm = false">{{ $t("no") }}</btn>
        <btn
           id="bulk_confirm_action_yes_button"
            type="danger"
          :disabled="jobPageStore.selectedJobs.length < 1"
          @click="performBulkAction"
        >
          {{ $t(`job.bulk.${bulkConfirmAction}.button`) }}
        </btn>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import CreateNewJobButton from "@/app/pages/job/browse/components/CreateNewJobButton.vue";
import JobListScmActions from "@/app/pages/job/browse/JobListScmActions.vue";
import JobListScmStatus from "@/app/pages/job/browse/JobListScmStatus.vue";
import { getRundeckContext } from "@/library";
import {
  JobBrowserStore,
  JobBrowserStoreInjectionKey,
} from "@/library/stores/JobBrowser";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { Notification } from "uiv";
import { defineComponent, inject, ref } from "vue";
import UiSocket from "@/library/components/utils/UiSocket.vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
  name: "JobBulkEditControls",
  components: {
    JobListScmStatus,
    CreateNewJobButton,
    UiSocket,
    JobListScmActions,
    Notification,
  },
  setup(props) {
    const jobBrowserStore: JobBrowserStore = inject(
      JobBrowserStoreInjectionKey,
    ) as JobBrowserStore;
    const jobPageStore: JobPageStore = inject(
      JobPageStoreInjectionKey,
    ) as JobPageStore;
    return {
      jobBrowserStore,
      jobPageStore,
      bulkConfirm: ref(false),
      bulkConfirmAction: ref(""),
    };
  },
  mounted() {
    eventBus.on("job-action", this.confirmAction);
    eventBus.on("job-action-single", this.confirmActionSingle);
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
      this.jobPageStore.bulkEditMode = true;
      eventBus.emit(`browser-job-item-select:${job.id}`);
      this.confirmAction({ name });
    },
    async performBulkAction() {
      try {
        await this.jobPageStore.performBulkAction(this.bulkConfirmAction);

        Notification.notify({
          type: "success",
          content: this.$t(`job.bulk.${this.bulkConfirmAction}.success`, [
            this.jobPageStore.selectedJobs.length,
          ]),
        });
      } catch (e) {
        Notification.notify({
          type: "error",
          html: false,
          content: e.message,
        });
        this.bulkConfirm = false;
        return;
      }
      this.bulkConfirm = false;
      const modifiedPaths = [];
      this.jobPageStore.selectedJobs.forEach((job) => {
        if (!modifiedPaths.includes(job.groupPath)) {
          modifiedPaths.push(job.groupPath || "");
        }
      });
      this.selectNone();
      this.jobPageStore.bulkEditMode = false;
      modifiedPaths.forEach((path) => {
        this.jobPageStore.getJobBrowser().refresh(path);
      });

      eventBus.emit("job-bulk-modified-paths", modifiedPaths);
    },
    projAuthz(action: string): boolean {
      return this.jobPageStore.jobAuthz?.[action];
    },
    selectAll() {
      eventBus.emit("job-bulk-edit-select-all");
    },
    selectNone() {
      eventBus.emit("job-bulk-edit-select-none");
    },
  },
});
</script>

<style scoped lang="scss">
.bulk_edit_controls {
  //margin-bottom: var(--spacing-8);

  .status-item {
    margin-right: var(--spacing-2);
  }
}

.bulk_edit_panel {
  margin-top: var(--spacing-8);
  .btn + .btn {
    margin-left: var(--spacing-2);
  }
}
</style>
