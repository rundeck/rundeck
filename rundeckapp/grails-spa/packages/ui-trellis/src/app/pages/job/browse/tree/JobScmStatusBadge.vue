<template>
    <span
        :title="tooltip"
        class="has_tooltip"
        data-viewport="#section-content"
        data-placement="right"
        v-if="shouldShow"
    >
        <span :class="iconStatus.color">
            <i
                class="glyphicon"
                :class="icon || iconStatus.icon"
                v-if="notext"
            ></i>
            <span v-if="!notext">{{ textOutput }}</span>
        </span>
    </span>
</template>

<script lang="ts">
import { defineComponent } from "vue";

interface StatusRep {
    icon: string;
    color: string;
}

export default defineComponent({
    name: "JobScmStatusBadge",
    props: {
        notext: {
            type: Boolean,
            default: false,
        },
        icon: {
            type: String,
            required: false,
        },
        text: {
            type: String,
            required: false,
        },
        exportStatus: {
            type: String,
            default: null,
        },
        importStatus: {
            type: String,
            default: null,
        },
        showClean: {
            type: Boolean,
            default: false,
        },
    },
    computed: {
        shouldShow(){
          return this.showClean && (this.exportStatus||this.importStatus) ||
            (this.exportStatus && this.exportStatus!=='CLEAN')||
            (this.importStatus && this.importStatus!=='CLEAN')
        },
        tooltip() {
            let text = "";
            if (this.exportStatus) {
                text += this.$t(
                    `scm.export.status.${this.exportStatus}.description`
                );
            }
            if (this.importStatus) {
                text += this.$t(
                    `scm.import.status.${this.importStatus}.description`
                );
            }
            return text;
        },
        textOutput() {
            return this.text;
        },
        iconStatus(): StatusRep {
            if (
                this.exportStatus &&
                this.importStatus &&
                this.exportStatus !== "CLEAN" &&
                this.importStatus !== "CLEAN"
            ) {
                return {
                    icon: "glyphicon-exclamation-sign",
                    color: "text-warning",
                };
            } else {
                let showStatus =
                    this.exportStatus &&
                    (this.showClean || this.exportStatus !== "CLEAN")
                        ? this.exportStatus
                        : this.importStatus;
                switch (showStatus) {
                    case "EXPORT_NEEDED":
                        return {
                            color: "text-info",
                            icon: "glyphicon-exclamation-sign",
                        };
                    case "CREATE_NEEDED":
                        return {
                            color: "text-success",
                            icon: "glyphicon-exclamation-sign",
                        };
                    case "UNKNOWN":
                        return {
                            color: "text-strong",
                            icon: "glyphicon-question-sign",
                        };
                    case "IMPORT_NEEDED":
                        return {
                            color: "text-warning",
                            icon: "glyphicon-exclamation-sign",
                        };
                    case "REFRESH_NEEDED":
                        return {
                            color: "text-warning",
                            icon: "glyphicon-exclamation-sign",
                        };
                    case "DELETED":
                        return {
                            color: "text-danger",
                            icon: "glyphicon-minus-sign",
                        };
                    case "CLEAN":
                        return {
                            color: "text-strong",
                            icon: "glyphicon-ok",
                        };
                    case "DELETE_NEEDED":
                        return {
                            color: "text-danger",
                            icon: "glyphicon-minus-sign",
                        };
                }
            }
            return {
                icon: "text-info",
                color: "glyphicon-question-sign",
            };
        },
    },
});
</script>

<style scoped lang="scss"></style>