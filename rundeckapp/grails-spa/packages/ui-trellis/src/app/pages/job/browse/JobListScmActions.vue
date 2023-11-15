<template>
    <template v-if="scmExport?.enabled">
        <li v-if="scmExport?.actions" class="divider"></li>
        <li
            v-if="scmExport?.actions"
            role="presentation"
            class="dropdown-header"
        >
            <i class="glyphicon glyphicon-circle-arrow-right" />
            {{ $t("scm.export.actions.title") }}
        </li>
        <li v-for="action in scmExport?.actions">
            <a
                :href="
                    jobPageStore.createProjectScmActionHref(action.id, 'export')
                "
                :title="action.description"
            >
                {{ action.title }}
            </a>
        </li>
    </template>
    <template v-if="scmImport?.enabled">
        <li v-if="scmImport?.actions" class="divider"></li>
        <li
            v-if="scmImport?.actions"
            role="presentation"
            class="dropdown-header"
        >
            <i class="glyphicon glyphicon-circle-arrow-left" />
            {{ $t("scm.import.actions.title") }}
        </li>
        <li v-for="action in scmImport?.actions">
            <a
                :href="
                    jobPageStore.createProjectScmActionHref(action.id, 'import')
                "
                :title="action.description"
            >
                {{ action.title }}
            </a>
        </li>
    </template>
    <template
        v-if="
            authProjectSCMAdmin &&
            (scmImport?.configured || scmExport?.configured)
        "
    >
        <li class="divider"></li>
        <li>
            <a
                id="toggle_btn"
                data-toggle="modal"
                href="#toggle_confirm"
                class=""
            >
                {{ $t(`job.toggle.scm.menu.${enabledStatus?'off':'on'}`) }}
            </a>
        </li>
    </template>
</template>

<script lang="ts">
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
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobPageStore,
        };
    },
    methods: {
        projectScmAction(id: string) {
            return `${this.jobPageStore.projectScmHref}/performAction?actionId=${id}`;
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
