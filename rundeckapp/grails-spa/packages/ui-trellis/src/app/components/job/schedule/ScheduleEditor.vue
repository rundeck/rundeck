<template>
  <div v-if="modelData">
    <div class="form-group">
      <label class="col-sm-2 control-label">
        Schedule to run repeatedly?
      </label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="scheduledFalse"
            v-model="modelData.scheduled"
            type="radio"
            name="scheduled"
            :value="false"
          />
          <label for="scheduledFalse">
            {{ $t("no") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="scheduledTrue"
            v-model="modelData.scheduled"
            type="radio"
            name="scheduled"
            :value="true"
          />
          <label for="scheduledTrue">
            {{ $t("yes") }}
          </label>
        </div>
      </div>
    </div>
    <div id="scheduledExecutionEditTZ" class="form-group">
      <template v-if="modelData.scheduled">
        <div class="form-group">
          <div class="col-sm-10 col-sm-offset-2">
            <div class="row">
              <div class="col-xs-12">
                <div class="base-filters">
                  <div class="row">
                    <div class="col-xs-10">
                      <div class="vue-tabs">
                        <div class="nav-tabs-navigation">
                          <ul class="nav nav-tabs">
                            <li
                              id="simpleLi"
                              :class="{
                                active: !modelData.useCrontabString,
                              }"
                            >
                              <a
                                data-crontabstring="false"
                                href="#cronsimple"
                                @click="showSimpleCron"
                                >Simple</a
                              >
                            </li>
                            <li
                              id="cronLi"
                              :class="{
                                active: !!modelData.useCrontabString,
                              }"
                            >
                              <a
                                data-crontabstring="true"
                                href="#cronstrtab"
                                @click="showCronExpression"
                                >Crontab</a
                              >
                            </li>
                          </ul>
                        </div>
                      </div>
                    </div>
                    <div class="col-xs-10">
                      <div class="form-group">
                        <div v-if="!modelData.useCrontabString" id="cronsimple">
                          <div class="crontab tabtarget">
                            <div class="panel-body">
                              <div id="hourTab" class="col-sm-4 form-inline">
                                <label
                                  for="hourNumber"
                                  aria-hidden="false"
                                  style="display: none"
                                  >Hour</label
                                >
                                <select
                                  id="hourNumber"
                                  v-model="modelData.hourSelected"
                                  name="hour"
                                  class="form-control"
                                  style="width: auto"
                                >
                                  <option
                                    v-for="hour in hours"
                                    :key="hour"
                                    :value="hour"
                                  >
                                    {{ hour }}
                                  </option>
                                </select>
                                :
                                <label
                                  for="minuteNumber"
                                  aria-hidden="false"
                                  style="display: none"
                                  >Minute</label
                                >
                                <select
                                  id="minuteNumber"
                                  v-model="modelData.minuteSelected"
                                  name="minute"
                                  class="form-control"
                                  style="width: auto"
                                >
                                  <option
                                    v-for="minute in minutes"
                                    :key="minute"
                                    :value="minute"
                                  >
                                    {{ minute }}
                                  </option>
                                </select>
                              </div>
                              <div class="col-sm-4">
                                <div
                                  id="DayOfWeekDialog"
                                  class="checklist checkbox"
                                >
                                  <input
                                    id="everyDay"
                                    v-model="modelData.everyDayOfWeek"
                                    name="everyDay"
                                    type="checkbox"
                                    value="all"
                                  />
                                  <label for="everyDay">Every Day</label>
                                  <template v-if="!modelData.everyDayOfWeek">
                                    <div
                                      v-for="(day, n) in days"
                                      class="_defaultInput checkbox"
                                    >
                                      <input
                                        :id="'dayCheckbox_' + n"
                                        v-model="modelData.selectedDays"
                                        type="checkbox"
                                        :value="day.shortName"
                                      />
                                      <label :for="'dayCheckbox_' + n">{{
                                        day.name
                                      }}</label>
                                    </div>
                                  </template>
                                  <input
                                    type="hidden"
                                    name="selectedDaysOfWeek"
                                    :value="modelData.selectedDays.join(',')"
                                  />
                                </div>
                              </div>
                              <div class="col-sm-4">
                                <div
                                  id="MonthDialog"
                                  class="checklist checkbox"
                                >
                                  <input
                                    id="everyMonth"
                                    v-model="modelData.allMonths"
                                    name="everyMonth"
                                    type="checkbox"
                                    value="all"
                                  />
                                  <label for="everyMonth">Every Month</label>
                                  <template v-if="!modelData.allMonths">
                                    <div
                                      v-for="(month, n) in months"
                                      class="_defaultInput checkbox"
                                    >
                                      <input
                                        :id="'monthCheckbox_' + n"
                                        v-model="modelData.selectedMonths"
                                        type="checkbox"
                                        :value="month.shortName"
                                      />
                                      <label :for="'monthCheckbox_' + n">{{
                                        month.name
                                      }}</label>
                                    </div>
                                  </template>
                                </div>
                                <input
                                  type="hidden"
                                  name="selectedMonths"
                                  :value="modelData.selectedMonths.join(',')"
                                />
                              </div>
                            </div>
                          </div>
                        </div>

                        <div v-if="modelData.useCrontabString" id="cronstrtab">
                          <div>
                            <div class="panel-body">
                              <div class="container">
                                <div class="row">
                                  <div class="col-sm-6">
                                    <div class="form-group">
                                      <input
                                        ref="crontabInput"
                                        v-model="modelData.crontabString"
                                        type="text"
                                        name="crontabString"
                                        autofocus="true"
                                        class="form-control input-sm"
                                        size="50"
                                        @click="updateCrontabPosition"
                                        @keyup="updateCrontabPosition"
                                        @change="validateCronExpression"
                                        @blur="crontabBlur"
                                      />
                                      <input
                                        v-model="modelData.useCrontabString"
                                        type="hidden"
                                        name="useCrontabString"
                                      />
                                    </div>
                                    <span
                                      v-if="crontabHint"
                                      class="text-muted"
                                      style="font-style: italic"
                                      >{{ crontabHint }}</span
                                    >
                                  </div>
                                  <div class="col-sm-6">
                                    <div v-html="errors"></div>
                                  </div>
                                </div>
                                <div class="row">
                                  <div class="text-strong col-sm-12">
                                    <div>
                                      <p>
                                        Ranges: <code>1-3</code>. Lists:
                                        <code>1,4,6</code>. Increments:
                                        <code>0/15</code> "every 15 units
                                        starting at 0".
                                      </p>
                                      <p>
                                        Valid values of Day of Week: 1-7 or
                                        SUN-SAT
                                      </p>
                                      <p>
                                        Valid values of Month: 1-12 or JAN-DEC
                                      </p>
                                    </div>
                                    See:
                                    <a
                                      :href="
                                        $t('documentation.reference.cron.url')
                                      "
                                      class="external"
                                      target="_blank"
                                      >Cron reference</a
                                    >
                                    for formatting help
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!--------------------------->
        <div class="form-group">
          <div class="col-sm-2 control-label text-form-label">
            {{ $t("scheduledExecution.property.timezone.prompt") }}
          </div>
          <div class="col-sm-5">
            <input
              id="timeZone"
              v-model="modelData.timeZone"
              type="text"
              name="timeZone"
              autofocus="true"
              class="form-control input-sm"
              size="50"
            />
            <typeahead
              v-model="modelData.timeZone"
              target="#timeZone"
              :data="modelData.timeZones"
            />
            <span class="help-block">
              {{ $t("scheduledExecution.property.timezone.description") }}
            </span>
          </div>
        </div>
      </template>
    </div>
    <ui-socket
      section="job-edit-schedules-editor"
      location="scheduledExecutionEditTZ:after"
      :event-bus="eventBus"
    />
    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t("scheduledExecution.property.scheduleEnabled.label") }}
      </div>

      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="scheduleEnabledTrue"
            v-model="modelData.scheduleEnabled"
            type="radio"
            name="scheduleEnabled"
            :value="true"
          />
          <label for="scheduleEnabledTrue">
            {{ $t("yes") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="scheduleEnabledFalse"
            v-model="modelData.scheduleEnabled"
            type="radio"
            :value="false"
            name="scheduleEnabled"
          />
          <label for="scheduleEnabledFalse">
            {{ $t("no") }}
          </label>
        </div>

        <span class="help-block">
          {{ $t("scheduledExecution.property.scheduleEnabled.description") }}
        </span>
      </div>
    </div>

    <div class="form-group">
      <div class="col-sm-2 control-label text-form-label">
        {{ $t("scheduledExecution.property.executionEnabled.label") }}
      </div>

      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="executionEnabledTrue"
            v-model="modelData.executionEnabled"
            type="radio"
            name="executionEnabled"
            :value="true"
          />
          <label for="executionEnabledTrue">
            {{ $t("yes") }}
          </label>
        </div>

        <div class="radio radio-inline">
          <input
            id="executionEnabledFalse"
            v-model="modelData.executionEnabled"
            type="radio"
            :value="false"
            name="executionEnabled"
          />
          <label for="executionEnabledFalse">
            {{ $t("no") }}
          </label>
        </div>

        <span class="help-block">
          {{ $t("scheduledExecution.property.executionEnabled.description") }}
        </span>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import axios from "axios";
import UiSocket from "../../../../library/components/utils/UiSocket.vue";
import { defineComponent, PropType } from "vue";

import {
  getDays,
  getMonths,
  getSimpleDecomposition,
} from "./services/scheduleDefinition";
import { EventBus } from "../../../../library";

export default defineComponent({
  name: "ScheduleEditor",
  components: {
    UiSocket,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
    },
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: true,
    },
    useCrontabString: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      modelData: {},
      name: "",
      errors: "",
      hours: [],
      minutes: [],
      days: [],
      months: [],
      crontabpos: -1,
    };
  },
  computed: {
    labelColClass() {
      return "col-sm-2 control-label";
    },
    fieldColSize() {
      return "col-sm-10";
    },
    crontabHint() {
      return this.modelData.useCrontabString
        ? this.$t(getHintText(this.crontabpos, this.modelData.crontabString))
        : "";
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
  beforeMount() {
    const _hours: string[] = [];
    const _minutes: string[] = [];
    for (let x = 0; x < 24; x++) {
      _hours.push(x < 10 ? "0" + x.toString() : x.toString());
    }
    for (let x = 0; x < 60; x++) {
      _minutes.push(x < 10 ? "0" + x.toString() : x.toString());
    }
    this.hours = _hours;
    this.minutes = _minutes;
    this.days = getDays();
    this.months = getMonths();
  },
  mounted() {
    this.modelData = Object.assign(
      {
        selectedDays: [],
        selectedMonths: [],
        useCrontabString: this.useCrontabString,
      },
      this.modelValue,
    );

    if (this.modelData.useCrontabString) {
      this.showCronExpression();
      return;
    }
    this.showSimpleCron();
  },
  methods: {
    loadScheduleIntoSimpleTab(decomposedSchedule: any) {
      this.modelData.hourSelected = decomposedSchedule.hour;
      this.modelData.minuteSelected = decomposedSchedule.minute;
      this.modelData.selectedDays =
        decomposedSchedule.days.length <= 7 ? decomposedSchedule.days : [];
      this.modelData.selectedMonths =
        decomposedSchedule.months.length <= 12 ? decomposedSchedule.months : [];
      this.modelData.everyDayOfWeek = decomposedSchedule.days.length === 7;
      this.modelData.allMonths = decomposedSchedule.months.length === 12;
    },
    showSimpleCron() {
      if (this.modelData.crontabString) {
        const cronComponents = this.modelData.crontabString.split(" ");

        const decomposedSchedule = getSimpleDecomposition(
          cronComponents[2],
          cronComponents[1],
          cronComponents[5],
          cronComponents[4],
        );
        this.loadScheduleIntoSimpleTab(decomposedSchedule);
      }
      this.modelData.useCrontabString = false;
    },
    showCronExpression() {
      this.modelData.useCrontabString = true;
    },
    validateCronExpression() {
      this.errors = "";
      axios({
        method: "post",
        headers: {},
        params: {
          project: window._rundeck.projectName,
          crontabString: this.modelData.crontabString,
        },
        url: new URL(
          `scheduledExecution/checkCrontab`,
          window._rundeck.rdBase,
        ).toString(),
        withCredentials: true,
      }).then((response) => {
        this.errors = response.request.response;
      });
    },
    crontabBlur() {
      this.validateCronExpression();
      this.crontabpos = -1;
    },
    updateCrontabPosition() {
      this.crontabpos = getCaretPos();
    },
  },
});

/**
 * Get crontab position hint string
 * @param pos caret position within string
 * @param text string
 */
function getHintText(pos: number, text: string) {
  const c = getCrontabSection(pos, text);
  if (c >= 0 && c <= 6) {
    return `cron.section.${c}`;
  }
  return "";
}

/**
 * Get crontab position hint string
 * @param pos caret position within string
 * @param text string
 */
function getCrontabSection(pos: number, text: string): number {
  if (pos < 0) {
    return -1;
  }
  //find # of space chars prior to pos
  const sub = text.substring(0, pos);
  const c = sub.split(" ").length;
  if (c >= 1 && c <= 7) {
    return c - 1;
  }
  return -1;
}

/**
 * Determine selection caret position within the input field
 * nb: there's probably a better/modern way to do this, this is old code
 * @param el input element
 */
function getCaretPos() {
  const el = document.getElementsByName("crontabString")[0] as HTMLInputElement;
  let rng,
    ii = -1;
  if (typeof el.selectionStart == "number") {
    return el.selectionStart;
    //@ts-ignore
  } else if (document.selection && el.createTextRange) {
    //@ts-ignore
    rng = document.selection.createRange();
    rng.collapse(true);
    rng.moveStart("character", -el.value.length);
    return rng.text.length;
  }
  return ii;
}
</script>
