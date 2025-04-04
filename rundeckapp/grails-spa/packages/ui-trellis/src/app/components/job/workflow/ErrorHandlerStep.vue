<template>
  <div class="error-handler-section">
    <div class="error-handler-section--content">
      <strong>{{ $t("Workflow.stepErrorHandler.label.on.error") }}:</strong>
      <div class="configuration" @click.stop="$emit('edit')">
        <plugin-config
            v-if="!step.errorhandler.jobref"
            :service-name="
          step.errorhandler.nodeStep
            ? ServiceType.WorkflowNodeStep
            : ServiceType.WorkflowStep
        "
            :provider="step.errorhandler.type"
            :config="step.errorhandler.config"
            :read-only="true"
            :show-title="true"
            :show-icon="true"
            :show-description="true"
            mode="show"
        >
          <template v-if="step.errorhandler.nodeStep" #iconSuffix>
            <i class="fas fa-hdd node-icon"></i>
          </template>
        </plugin-config>
        <div v-else class="row">
          <div class="col-xs-12">
            <job-ref-step :step="step.errorhandler"></job-ref-step>
          </div>
        </div>
      </div>

      <span
        v-if="step.errorhandler.keepgoingOnSuccess"
        :title="$t('Workflow.stepErrorHandler.keepgoingOnSuccess.description')"
        class="succeed"
        data-testid="keepgoingOnSuccess"
      >
        {{ $t("Workflow.stepErrorHandler.label.keep.going.on.success") }}
      </span>
    </div>
    <div class="btn-group" role="group" aria-label="item controls">
      <button data-testid="remove-handler-button" class="btn btn-xs btn-default" type="button" @click.stop="$emit('removeHandler', step)">
        <i class="glyphicon glyphicon-remove"></i>
      </button>
    </div>
  </div>
</template>
<script lang="ts">
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import { ServiceType } from "@/library/stores/Plugins";
import { PropType } from "vue";
import { EditStepData } from "@/app/components/job/workflow/types/workflowTypes";
import JobRefStep from "@/app/components/job/workflow/JobRefStep.vue";

export default {
  name: "ErrorHandlerStep",
  components: { JobRefStep, PluginConfig },
  props: {
    step: {
      type: Object as PropType<EditStepData>,
      required: true,
    },
  },
  data() {
    return {
      ServiceType,
    };
  },
  emits: ["removeHandler", "edit"]
};
</script>
<style lang="scss">
.error-handler-section {
  border: 1px solid var(--list-item-border-color);
  border-radius: 5px;
  padding: 10px;
  display: flex;
  justify-content: flex-end;

  &--content {
    flex-grow: 1;
  }

  .configuration {
    padding: 5px;
    border: 1px dotted transparent;

    &:hover {
      cursor: pointer;
      background-color: var(--light-gray);
      border-color: #68b3c8;
    }
  }
}
</style>