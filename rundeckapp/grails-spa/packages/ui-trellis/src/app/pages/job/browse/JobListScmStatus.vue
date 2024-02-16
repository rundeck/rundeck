<template>
  <template v-if="displayExport || displayImport || exportError || importError">
    <popover
      trigger="hover"
      placement="left"
      viewport="#section-content"
      append-to="#section-content"
      position-by="#section-main"
    >
      <span class="text-info" v-if="displayExportState || displayImportState">
        <i class="glyphicon glyphicon-exclamation-sign"></i>
        {{ defaultDisplayText }}
      </span>
      <span class="text-danger" v-if="exportError || importError">
            <i class="glyphicon glyphicon-exclamation-sign"></i>
            {{ $t('scm.status.ERROR.display.text') }}
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
            <dl v-if="exportError">
              <dt>{{ $t("scm.export.title") }}</dt>
              <dd>
                {{ exportErrorText }}
              </dd>
            </dl>
            <dl v-if="importError">
              <dt>{{ $t("scm.import.title") }}</dt>
              <dd>
                {{ importErrorText }}
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
        importError(){
            return this.scmImport?.valid!==null && this.scmImport?.valid===false
        },
        scmExport: function () {
            return this.jobPageStore.findMeta("scmExport");
        },
        exportState() {
            return this.scmExport?.status?.state;
        },
        exportError(){
          return this.scmExport?.valid!==null && this.scmExport?.valid===false
        },
        displayExportState() {
            return this.exportState && this.exportState !== "CLEAN" ;
        },
        displayExport() {
            return this.displayExportState;
        },
        displayImportState() {
            return this.importState && this.importState !== "CLEAN";
        },
        displayImport() {
            return this.displayImportState;
        },
        defaultDisplayText() {
            if(this.displayExport){
              return this.exportDisplayText
            }else{
              return this.importDisplayText
            }
        },
        exportErrorText(){
            if(this.scmExport?.validationError){
              return this.scmExport?.validationError
            }else if (this.scmExport?.validationErrorCode){
              return this.$t(this.scmExport?.validationErrorCode)
            }else{
              return this.$t('scm.export.status.ERROR.display.text')
            }
        },
        importErrorText() {
            if(this.scmImport?.validationError){
              return this.scmImport?.validationError
            }else if (this.scmImport?.validationErrorCode){
              return this.$t(this.scmImport?.validationErrorCode)
            }else{
              return this.$t('scm.import.status.ERROR.display.text')
            }
        },
        importDisplayText() {
          return this.$t(
            `scm.import.status.${this.importState}.display.text`
          );
        },
        exportDisplayText() {
          return this.$t(
            `scm.export.status.${this.exportState}.display.text`
          );
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
