<template>
  <template v-if="scmExport?.enabled">
    <li v-if="scmExport?.actions" class="divider"></li>
    <li v-if="scmExport?.actions" role="presentation" class="dropdown-header">
      <i class="glyphicon glyphicon-circle-arrow-right" />
      {{ $t("scm.export.actions.title") }}
    </li>
    <li v-for="action in scmExport?.actions">
      <a
        :href="jobPageStore.createProjectScmActionHref(action.id, 'export')"
        :title="action.description"
      >
        {{ action.title }}
      </a>
    </li>
  </template>
  <template v-if="scmImport?.enabled">
    <li v-if="scmImport?.actions" class="divider"></li>
    <li v-if="scmImport?.actions" role="presentation" class="dropdown-header">
      <i class="glyphicon glyphicon-circle-arrow-left" />
      {{ $t("scm.import.actions.title") }}
    </li>
    <li v-for="action in scmImport?.actions">
      <a
        :href="jobPageStore.createProjectScmActionHref(action.id, 'import')"
        :title="action.description"
      >
        {{ action.title }}
      </a>
    </li>
  </template>
  <template
    v-if="
      authProjectSCMAdmin && (scmImport?.configured || scmExport?.configured)
    "
  >
    <li class="divider"></li>
    <li>
      <a @click="toggleModal = true">
        {{ $t(`job.toggle.scm.menu.${enabledStatus ? "off" : "on"}`) }}
      </a>
    </li>
    <Teleport to="body">
      <modal
        :title="$t('job.toggle.scm.confirm.panel.title')"
        v-model="toggleModal"
      >
        <p>
          {{ $t(`job.toggle.scm.confirm.${enabledStatus ? "off" : "on"}`) }}
        </p>
        <template #footer>
          <btn @click="toggleModal = false">
            {{ $t("no") }}
          </btn>
          <btn type="danger" @click="actionToggleScm(!enabledStatus)">
            {{
              $t(`job.toggle.scm.button.label.${enabledStatus ? "off" : "on"}`)
            }}
          </btn>
        </template>
      </modal>
    </Teleport>
  </template>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import { scmProjectToggle } from "@/library/services/jobBrowse";
import {
  JobBrowserStore,
  JobBrowserStoreInjectionKey,
} from "@/library/stores/JobBrowser";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";

interface ScmIntegrationMeta {
  enabled: boolean;
  configured: boolean;
  actions: ScmAction[];
}

interface ScmAction {
  id: string;
  title: string;
  description: string;
}

export default defineComponent({
  name: "JobListScmActions",
  setup(props) {
    const jobPageStore: JobPageStore = inject(
      JobPageStoreInjectionKey,
    ) as JobPageStore;
    return {
      jobPageStore,
      toggleModal: ref(false),
    };
  },
  methods: {
    projectScmAction(id: string) {
      return `${this.jobPageStore.projectScmHref}/performAction?actionId=${id}`;
    },
    async actionToggleScm(enabled: boolean) {
      this.toggleModal = false;
      let result = await scmProjectToggle(
        getRundeckContext().projectName,
        enabled,
      );
    },
  },
  computed: {
    enabledStatus(): boolean {
      return this.scmImport?.enabled || this.scmExport?.enabled;
    },
    authProjectSCMAdmin(): boolean {
      return this.jobPageStore.projAuthz["configure"];
    },
    scmExportEnabled(): boolean {
      return this.scmExport?.enabled;
    },
    scmExportActions(): ScmAction[] {
      return this.scmExport?.actions;
    },
    scmImportEnabled(): boolean {
      return this.scmExport?.enabled;
    },
    scmImportActions(): ScmAction[] {
      return this.scmImport?.actions;
    },
    scmExport(): ScmIntegrationMeta | undefined {
      return this.jobPageStore.findMeta("scmExport");
    },
    scmImport(): ScmIntegrationMeta | undefined {
      return this.jobPageStore.findMeta("scmImport");
    },
  },
});
</script>

<style scoped lang="scss"></style>
