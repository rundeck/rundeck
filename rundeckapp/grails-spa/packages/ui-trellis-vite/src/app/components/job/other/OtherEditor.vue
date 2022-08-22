<template>
  <div v-if="modelData">
    <div class="form-group">
      <label class="col-sm-2 control-label">
        Log level
      </label>
      <div class="col-sm-10 ">
        <div class="radio radio-inline">
          <input type="radio"
                 name="loglevel"
                 :value="'INFO'"
                 v-model="modelData.loglevel"
                 id="logLevelNormalTrue"/>
          <label for="logLevelNormalTrue">
            {{ $t('loglevel.normal') }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input id="logLevelNormalFalse"
                 type="radio"
                 name="loglevel"
                 :value="'DEBUG'"
                 v-model="modelData.loglevel"/>
          <label for="logLevelNormalFalse">
            {{ $t('loglevel.debug') }}
          </label>
        </div>
        <div class="help-block">
          {{ $t('scheduledExecution.property.loglevel.help') }}
        </div>
      </div>
    </div>
    <div class="form-group">
      <label class="col-sm-2 control-label">
        Multiple Executions?
      </label>
      <div class="col-sm-10 ">
        <div class="radio radio-inline">
          <input type="radio"
                 name="multipleExecutions"
                 :value="false"
                 v-model="modelData.multipleExecutions"
                 id="multipleFalse"/>
          <label for="multipleFalse">
            No
          </label>
        </div>
        <div class="radio radio-inline">
          <input id="multipleTrue"
                 type="radio"
                 name="multipleExecutions"
                 :value="true"
                 v-model="modelData.multipleExecutions"/>
          <label for="multipleTrue">
            Yes
          </label>
        </div>
        <div class="help-block">
          {{ $t('scheduledExecution.property.multipleExecutions.description') }}
        </div>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t('scheduledExecution.property.maxMultipleExecutions.label') }}
      </div>

      <div :class="fieldColHalfSize">

        <input type='text'
               name="maxMultipleExecutions"
               v-model="modelData.maxMultipleExecutions"
               id="maxMultipleExecutions"
               class="form-control"/>
        <span class="help-block">
                    {{ $t('scheduledExecution.property.maxMultipleExecutions.description') }}
                </span>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        Timeout
      </div>

      <div :class="fieldColHalfSize">
        <input type='text'
               name="timeout"
               v-model="modelData.timeout"
               id="schedJobTimeout"
               class="form-control"/>
        <span class="help-block">
                    {{ $t('scheduledExecution.property.timeout.description') }}
                </span>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label">
        <label for="schedJobRetry">Retry</label>
      </div>

      <div :class="fieldColShortSize">
          <input type='text'
               name="retry"
               v-model="modelData.retry"
               id="schedJobRetry"
               class="form-control"/>

        <span class="help-block">
                    {{ $t('scheduledExecution.property.retry.description') }}
                </span>
      </div>

      <label class="col-sm-2 control-label text-form-label">
        Retry Delay
      </label>

      <div :class="fieldColShortSize">
        <input type='text'
               name="retryDelay"
               v-model="modelData.retryDelay"
               id="schedJobRetryDelay"
               class="form-control"/>
        <span class="help-block">
                    {{ $t('scheduledExecution.property.retry.delay.description') }}
                </span>
      </div>
    </div>
    <div class="form-group">
      <label class="col-sm-2 control-label text-form-label" for="schedJobLogOutputThreshold">
        {{ $t('scheduledExecution.property.logOutputThreshold.label') }}
      </label>

      <div :class="fieldColShortSize">

        <input type='text' name="logOutputThreshold" v-model="modelData.logOutputThreshold"
               id="schedJobLogOutputThreshold" class="form-control"
               :placeholder="$t('scheduledExecution.property.logOutputThreshold.placeholder')"/>
        <span class="help-block">
                    {{ $t('scheduledExecution.property.logOutputThreshold.description') }}
                </span>
      </div>
      <label class="col-sm-2 control-label text-form-label" for="logOutputThresholdAction">
        {{ $t('scheduledExecution.property.logOutputThresholdAction.label') }}
      </label>

      <div :class="fieldColShortSize">
        <div class="radio">
          <input type="radio" name="logOutputThresholdAction"
            :value="'halt'"
            v-model="modelData.logOutputThresholdAction"
            id="logOutputThresholdAction"/>
          <label for="logOutputThresholdAction" >
          {{ $t('scheduledExecution.property.logOutputThresholdAction.halt.label') }}
          </label>
        </div>

        <div class="input-group">
          <input type='text' name="logOutputThresholdStatus"
                 v-model="modelData.logOutputThresholdStatus"
                 id="schedJobLogOutputThresholdStatus" class="form-control"
                 :placeholder="$t('scheduledExecution.property.logOutputThresholdStatus.placeholder')"
                 v-tooltip.left.hover="'Enter a status, such as \'failed\', \'aborted\' or any custom status.'"/>
        </div>
        <div class="radio">
          <input type="radio" name="logOutputThresholdAction"
                 :value="'truncate'"
                 v-model="modelData.logOutputThresholdAction"
                 id="logOutputThresholdActionTruncateAndContinue"/>
          <label for="logOutputThresholdActionTruncateAndContinue">
            {{ $t('scheduledExecution.property.logOutputThresholdAction.truncate.label') }}
          </label>
        </div>
        <span class="help-block">
                    {{ $t('scheduledExecution.property.logOutputThresholdAction.description') }}
                </span>
      </div>
    </div>
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t('scheduledExecution.property.defaultTab.label') }}
      </div>

      <div :class="fieldColSize">
        <div class="radio radio-inline">
          <input type="radio" name="defaultTab"
                 :value="'nodes'"
                 v-model="modelData.defaultTab"
                 id="tabSummary"/>
          <label for="tabSummary">
            {{ $t('execution.page.show.tab.Nodes.title') }}

          </label>
        </div>

        <div class="radio radio-inline">
          <input type="radio" name="defaultTab"
                 :value="'output'"
                 v-model="modelData.defaultTab"
                 id="tabOutput"/>
          <label for="tabOutput">
            {{ $t('execution.show.mode.Log.title') }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input type="radio" name="defaultTab"
                 :value="'html'"
                 v-model="modelData.defaultTab"
                 id="tabHTML"/>
          <label for="tabHTML">
            HTML
          </label>
        </div>



        <span class="help-block">
          {{ $t('scheduledExecution.property.defaultTab.description') }}
                </span>
      </div>
    </div>

    <div class="form-group" id="schedJobUuidLabel">
      <label for="schedJobUuid" class=" col-sm-2 control-label text-primary">
        UUID
      </label>

      <div :class="fieldColSize" v-if="modelData.uuid">
        <p class="form-control-static text-primary">
            {{modelData.uuid}}
        </p>
      </div>
      <div :class="fieldColSize" v-if="!modelData.uuid">
          <input type='text' name="uuid" v-model="modelData.uuid"
                 id="schedJobUuid" size="36" class="form-control"/>
      </div>

    </div>
  </div>
</template>
<script lang="ts">
import InlineValidationErrors from '../../form/InlineValidationErrors.vue'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop, Watch} from 'vue-property-decorator'


@Component({components: {InlineValidationErrors}})
export default class OtherEditor extends Vue {
  @Prop({required: true})
  value: any

  @Prop({required: true})
  eventBus!: Vue

  labelColClass = 'col-sm-2 control-label'
  fieldColSize = 'col-sm-10'
  fieldColHalfSize = 'col-sm-5'
  fieldColShortSize = 'col-sm-4'

  modelData: any = {}

  async mounted() {
    this.modelData = Object.assign({}, this.value)
    if(!this.modelData.defaultTab || this.modelData.defaultTab in ['summary','monitor','nodes']) {
      this.modelData.defaultTab = 'nodes'
    }

    if(!this.modelData.logOutputThresholdAction) {
      this.modelData.logOutputThresholdAction = 'halt'
    }
  }

  @Watch('modelData', {deep: true})
  wasChanged() {
    this.$emit('input', this.modelData)
  }
}
</script>
