<template>
  <div v-if="modelData">
    <div class="form-group">
      <label class="col-sm-2 control-label"> Log level </label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="logLevelNormalTrue"
            v-model="modelData.loglevel"
            type="radio"
            name="loglevel"
            :value="'INFO'"
          />
          <label for="logLevelNormalTrue">
            {{ $t("loglevel.normal") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="logLevelNormalFalse"
            v-model="modelData.loglevel"
            type="radio"
            name="loglevel"
            :value="'DEBUG'"
          />
          <label for="logLevelNormalFalse">
            {{ $t("loglevel.debug") }}
          </label>
        </div>
        <div class="help-block">
          {{ $t("scheduledExecution.property.loglevel.help") }}
        </div>
      </div>
    </div>
    <div class="form-group">
      <label class="col-sm-2 control-label"> Multiple Executions? </label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="multipleFalse"
            v-model="modelData.multipleExecutions"
            type="radio"
            name="multipleExecutions"
            :value="false"
          />
          <label for="multipleFalse"> No </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="multipleTrue"
            v-model="modelData.multipleExecutions"
            type="radio"
            name="multipleExecutions"
            :value="true"
          />
          <label for="multipleTrue"> Yes </label>
        </div>
        <div class="help-block">
          {{ $t("scheduledExecution.property.multipleExecutions.description") }}
        </div>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t("scheduledExecution.property.maxMultipleExecutions.label") }}
      </div>

      <div :class="fieldColHalfSize">
        <input
          id="maxMultipleExecutions"
          v-model="modelData.maxMultipleExecutions"
          type="text"
          name="maxMultipleExecutions"
          class="form-control"
        />
        <span class="help-block">
          {{
            $t("scheduledExecution.property.maxMultipleExecutions.description")
          }}
        </span>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">Timeout</div>

      <div :class="fieldColHalfSize">
        <input
          id="schedJobTimeout"
          v-model="modelData.timeout"
          type="text"
          name="timeout"
          class="form-control"
        />
        <span class="help-block">
          {{ $t("scheduledExecution.property.timeout.description") }}
        </span>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label">
        <label for="schedJobRetry">Retry</label>
      </div>

      <div :class="fieldColShortSize">
        <input
          id="schedJobRetry"
          v-model="modelData.retry"
          type="text"
          name="retry"
          class="form-control"
        />

        <span class="help-block">
          {{ $t("scheduledExecution.property.retry.description") }}
        </span>
      </div>

      <label class="col-sm-2 control-label text-form-label">
        Retry Delay
      </label>

      <div :class="fieldColShortSize">
        <input
          id="schedJobRetryDelay"
          v-model="modelData.retryDelay"
          type="text"
          name="retryDelay"
          class="form-control"
        />
        <span class="help-block">
          {{ $t("scheduledExecution.property.retry.delay.description") }}
        </span>
      </div>
    </div>
    <div class="form-group">
      <label
        class="col-sm-2 control-label text-form-label"
        for="schedJobLogOutputThreshold"
      >
        {{ $t("scheduledExecution.property.logOutputThreshold.label") }}
      </label>

      <div :class="fieldColShortSize">
        <input
          id="schedJobLogOutputThreshold"
          v-model="modelData.logOutputThreshold"
          type="text"
          name="logOutputThreshold"
          class="form-control"
          :placeholder="
            $t('scheduledExecution.property.logOutputThreshold.placeholder')
          "
        />
        <span class="help-block">
          {{ $t("scheduledExecution.property.logOutputThreshold.description") }}
        </span>
      </div>
      <label
        class="col-sm-2 control-label text-form-label"
        for="logOutputThresholdAction"
      >
        {{ $t("scheduledExecution.property.logOutputThresholdAction.label") }}
      </label>

      <div :class="fieldColShortSize">
        <div class="radio">
          <input
            id="logOutputThresholdAction"
            v-model="modelData.logOutputThresholdAction"
            type="radio"
            name="logOutputThresholdAction"
            :value="'halt'"
          />
          <label for="logOutputThresholdAction">
            {{
              $t(
                "scheduledExecution.property.logOutputThresholdAction.halt.label",
              )
            }}
          </label>
        </div>

        <div class="input-group">
          <input
            id="schedJobLogOutputThresholdStatus"
            v-model="modelData.logOutputThresholdStatus"
            v-tooltip.left.hover="
              'Enter a status, such as \'failed\', \'aborted\' or any custom status.'
            "
            type="text"
            name="logOutputThresholdStatus"
            class="form-control"
            :placeholder="
              $t(
                'scheduledExecution.property.logOutputThresholdStatus.placeholder',
              )
            "
          />
        </div>
        <div class="radio">
          <input
            id="logOutputThresholdActionTruncateAndContinue"
            v-model="modelData.logOutputThresholdAction"
            type="radio"
            name="logOutputThresholdAction"
            :value="'truncate'"
          />
          <label for="logOutputThresholdActionTruncateAndContinue">
            {{
              $t(
                "scheduledExecution.property.logOutputThresholdAction.truncate.label",
              )
            }}
          </label>
        </div>
        <span class="help-block">
          {{
            $t(
              "scheduledExecution.property.logOutputThresholdAction.description",
            )
          }}
        </span>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t("scheduledExecution.property.defaultTab.label") }}
      </div>

      <div :class="fieldColSize">
        <div class="radio radio-inline">
          <input
            id="tabSummary"
            v-model="modelData.defaultTab"
            type="radio"
            name="defaultTab"
            :value="'nodes'"
          />
          <label for="tabSummary">
            {{ $t("execution.page.show.tab.Nodes.title") }}
          </label>
        </div>

        <div class="radio radio-inline">
          <input
            id="tabOutput"
            v-model="modelData.defaultTab"
            type="radio"
            name="defaultTab"
            :value="'output'"
          />
          <label for="tabOutput">
            {{ $t("execution.show.mode.Log.title") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="tabHTML"
            v-model="modelData.defaultTab"
            type="radio"
            name="defaultTab"
            :value="'html'"
          />
          <label for="tabHTML"> HTML </label>
        </div>

        <span class="help-block">
          {{ $t("scheduledExecution.property.defaultTab.description") }}
        </span>
      </div>
    </div>

    <div id="schedJobUuidLabel" class="form-group">
      <label for="schedJobUuid" class="col-sm-2 control-label text-primary">
        UUID
      </label>

      <div v-if="modelData.uuid" :class="fieldColSize">
        <p class="form-control-static text-primary">
          {{ modelData.uuid }}
        </p>
      </div>
      <div v-if="!modelData.uuid" :class="fieldColSize">
        <input
          id="schedJobUuid"
          v-model="modelData.uuid"
          type="text"
          name="uuid"
          size="36"
          class="form-control"
        />
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import InlineValidationErrors from "../../form/InlineValidationErrors.vue";
import { defineComponent } from "vue";
import type { PropType } from "vue";

import { EventBus } from "../../../../library/utilities/vueEventBus";

export default defineComponent({
  name: "OtherEditor",
  components: {
    InlineValidationErrors,
  },
  props: {
    modelValue: {
      type: Object as PropType<any>,
      required: true,
    },
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      modelData: {} as { [key: string]: any },
    };
  },
  computed: {
    labelColClass() {
      return "col-sm-2 control-label";
    },
    fieldColSize() {
      return "col-sm-10";
    },
    fieldColHalfSize() {
      return "col-sm-5";
    },
    fieldColShortSize() {
      return "col-sm-4";
    },
  },
  watch: {
    modelData: {
      handler() {
        this.$emit("update:modelValue", this.modelData);
      },
      deep: true,
    },
  },
  mounted() {
    this.onMounted();
  },
  methods: {
    async onMounted() {
      this.modelData = Object.assign({}, this.modelValue);
      if (
        !this.modelData.defaultTab ||
        this.modelData.defaultTab in ["summary", "monitor", "nodes"]
      ) {
        this.modelData.defaultTab = "nodes";
      }

      if (!this.modelData.logOutputThresholdAction) {
        this.modelData.logOutputThresholdAction = "halt";
      }
    },
  },
});
</script>
