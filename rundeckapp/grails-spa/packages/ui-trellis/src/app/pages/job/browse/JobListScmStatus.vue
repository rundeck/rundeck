<template>
    <template v-if="displayExport || displayImport">
      <popover trigger="hover"
               placement="left"
               viewport="#section-content"
               append-to="#section-content"
               position-by="#section-main"
      >
        <span class="text-info">
            <i class="glyphicon glyphicon-exclamation-sign"></i>
            {{ defaultDisplayText }}
        </span>
        <template #popover>
            <dl v-if="displayExport">
              <dt>{{ $t("scm.export.title") }}</dt>
              <dd>
                {{ exportMessage }}
              </dd>
            </dl>
            <dl v-if="displayImport">
              <dt>{{ $t("scm.import.title") }}</dt>
              <dd>
                {{ importMessage }}
              </dd>
            </dl>
        </template>
      </popover>
    </template>
</template>

<script lang="ts">
import {
    JobPageStore,
    JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, inject } from "vue";

export default defineComponent({
    name: "JobListScmStatus",

    setup(props) {
        const jobPageStore: JobPageStore = inject(
            JobPageStoreInjectionKey
        ) as JobPageStore;
        return {
            jobPageStore,
        };
    },
    computed: {
        scmImport: function () {
            return this.jobPageStore.findMeta("scmImport");
        },
        importState() {
            return this.scmImport?.status?.state;
        },
        scmExport: function () {
            return this.jobPageStore.findMeta("scmExport");
        },
        exportState() {
            return this.scmExport?.status?.state;
        },
        displayExport() {
            return this.exportState && this.exportState !== "CLEAN";
        },
        displayImport() {
            return this.importState && this.importState !== "CLEAN";
        },
        defaultDisplayText() {
            if(this.displayExport){
              return this.exportDisplayText
            }else{
              return this.importDisplayText
            }
        },
        importDisplayText() {
            switch (this.importState) {
                case "IMPORT_NEEDED":
                    return this.$t(
                        "scm.import.status.IMPORT_NEEDED.display.text"
                    );
                case "REFRESH_NEEDED":
                    return this.$t(
                        "scm.import.status.REFRESH_NEEDED.display.text"
                    );
                case "UNKNOWN":
                    return this.$t("scm.import.status.UNKNOWN.display.text");
                case "CLEAN":
                    return this.$t("scm.import.status.CLEAN.display.text");
                case "LOADING":
                    return this.$t("scm.import.status.LOADING.display.text");
            }
            return this.importState;
        },
        exportDisplayText() {
            switch (this.exportState) {
                case "EXPORT_NEEDED":
                    return this.$t(
                        "scm.export.status.EXPORT_NEEDED.display.text"
                    );
                case "CREATE_NEEDED":
                    return this.$t(
                        "scm.export.status.CREATE_NEEDED.display.text"
                    );
                case "REFRESH_NEEDED":
                    return this.$t(
                        "scm.export.status.REFRESH_NEEDED.display.text"
                    );
                case "DELETED":
                    return this.$t("scm.export.status.DELETED.display.text");
                case "CLEAN":
                    return this.$t("scm.export.status.CLEAN.display.text");
                case "LOADING":
                    return this.$t("scm.export.status.LOADING.display.text");
            }
            return this.exportState;
        },
        exportMessage() {
            return this.scmExport?.status?.message;
        },
        importMessage() {
            return this.scmImport?.status?.message;
        },
    },
});
</script>

<style scoped lang="scss"></style>
