<template>
  <div v-if="modelData">
    <div class="form-group">
      <label class="col-sm-2 control-label">
        Schedule to run repeatedly?
      </label>
      <div class="col-sm-10 ">
        <div class="radio radio-inline">
          <input id="scheduledFalse"
                 type="radio"
                 name="scheduled"
                 :value="false"
                 v-model="modelData.scheduled"/>
          <label for="scheduledFalse">
            {{ $t('no') }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input type="radio"
                 name="scheduled"
                 :value="true"
                 v-model="modelData.scheduled"
                 id="scheduledTrue"/>
          <label for="scheduledTrue">
            {{ $t('yes') }}
          </label>
        </div>
      </div>
    </div>
    <div class="form-group" id="scheduledExecutionEditTZ">
    <template v-if="modelData.scheduled">
      <div class="form-group">
        <div class="col-sm-10 col-sm-offset-2">
          <div class="row">
            <div class="col-xs-12">
              <div class="base-filters">
                <div class="row">
                  <div class="col-xs-10">
                    <div class="vue-tabs"><div class="nav-tabs-navigation">
                      <ul class="nav nav-tabs ">
                        <li id="simpleLi" v-bind:class="{active: !modelData.useCrontabString}">
                          <a data-crontabstring="false"
                             href="#cronsimple"
                             @click="showSimpleCron"
                          >Simple</a>
                        </li>
                        <li id="cronLi" v-bind:class="{active: !!modelData.useCrontabString}">
                          <a data-crontabstring="true"
                             href="#cronstrtab"
                             @click="showCronExpression"
                          >Crontab</a>
                        </li>
                      </ul>
                    </div></div>
                  </div>
                  <div class="col-xs-10">
                    <div class="form-group">

                      <div id="cronsimple" v-if="!modelData.useCrontabString">
                        <div class="crontab tabtarget" >
                          <div class="panel-body">
                            <div class="col-sm-4 form-inline" id="hourTab">
                              <label for="hourNumber" aria-hidden="false" style="display: none;">Hour</label>
                              <select
                                id="hourNumber"
                                name="hour"
                                v-model="modelData.hourSelected"
                                class="form-control"
                                style="width:auto;"
                              >
                                <option v-for="hour in this.hours" :key="hour" v-bind:value="hour">{{hour}}</option>
                              </select>
                              :
                              <label for="minuteNumber" aria-hidden="false" style="display: none;">Minute</label>
                              <select
                                id="minuteNumber"
                                name="minute"
                                v-model="modelData.minuteSelected"
                                class="form-control"
                                style="width:auto;"
                              >
                                <option v-for="minute in this.minutes" :key="minute" v-bind:value="minute">{{minute}}</option>
                              </select>
                            </div>
                            <div class="col-sm-4">
                              <div class="checklist checkbox" id="DayOfWeekDialog">
                                <input
                                  id="everyDay"
                                  name="everyDay"
                                  type="checkbox"
                                  value="all"
                                  v-model="modelData.everyDayOfWeek"
                                >
                                <label for="everyDay">Every Day</label>
                                <div v-if="!modelData.everyDayOfWeek" class="_defaultInput checkbox" v-for="(day,n) in days">
                                  <input
                                    :id="'dayCheckbox_'+n"
                                    type="checkbox"
                                    :value="day.shortName"
                                    v-model="modelData.selectedDays"
                                  >
                                  <label :for="'dayCheckbox_'+n">{{day.name}}</label>
                                </div>
                                <input type="hidden" name="selectedDaysOfWeek" :value="modelData.selectedDays.join(',')" >
                              </div>
                            </div>
                            <div class="col-sm-4">
                              <div class="checklist checkbox" id="MonthDialog">
                                <input
                                  id="everyMonth"
                                  name="everyMonth"
                                  type="checkbox"
                                  value="all"
                                  v-model="modelData.allMonths"
                                >
                                <label for="everyMonth">Every Month</label>
                                <div v-if="!modelData.allMonths" class="_defaultInput checkbox" v-for="(month,n) in months">
                                  <input
                                    :id="'monthCheckbox_'+n"
                                    type="checkbox"
                                    :value="month.shortName"
                                    v-model="modelData.selectedMonths"
                                  >
                                  <label :for="'monthCheckbox_'+n">{{month.name}}</label>
                                </div>
                              </div>
                              <input type="hidden" name="selectedMonths" :value="modelData.selectedMonths.join(',')" >
                            </div>
                          </div>
                        </div>
                      </div>

                      <div id="cronstrtab" v-if="modelData.useCrontabString">
                        <div>
                          <div class="panel-body">
                            <div class="container">
                              <div class="row">
                                <div class="col-sm-6">
                                  <div class="form-group">
                                    <input
                                      ref="crontabInput"
                                      type="text"
                                      name="crontabString"
                                      autofocus="true"
                                      class="form-control input-sm"
                                      size="50"
                                      @click="updateCrontabPosition"
                                      @keyup="updateCrontabPosition"
                                      @change="validateCronExpression"
                                      @blur="crontabBlur"
                                      v-model="modelData.crontabString"
                                    >
                                    <input type="hidden" name="useCrontabString" v-model="modelData.useCrontabString">
                                  </div>
                                  <span class="text-muted" style="font-style: italic" v-if="crontabHint">{{crontabHint}}</span>
                                </div>
                                <div class="col-sm-6">
                                  <template>
                                    <div v-html="errors">
                                    </div>
                                  </template>
                                </div>
                              </div>
                              <div class="row">
                                <div class="text-strong col-sm-12">
                                  <div>
                                    <p>Ranges: <code>1-3</code>.  Lists: <code>1,4,6</code>. Increments: <code>0/15</code> "every 15 units starting at 0".</p>
                                    <p>Valid values of Day of Week: 1-7 or SUN-SAT</p>
                                    <p>Valid values of Month: 1-12 or JAN-DEC</p>
                                  </div>
                                  See: <a :href="$t('documentation.reference.cron.url')" class="external" target="_blank">Cron reference</a> for formatting help
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
          {{$t('scheduledExecution.property.timezone.prompt')}}
        </div>
        <div class="col-sm-5">
          <input
            type="text"
            name="timeZone"
            id="timeZone"
            autofocus="true"
            class="form-control input-sm"
            size="50"
            v-model="modelData.timeZone"
          >
          <typeahead  v-model="modelData.timeZone"  target="#timeZone" :data="modelData.timeZones"  />
          <span class="help-block">
            {{$t('scheduledExecution.property.timezone.description')}}
          </span>
        </div>
      </div>
    </template>
    </div>
    <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
          {{ $t('scheduledExecution.property.scheduleEnabled.label') }}
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input type="radio"
                   name="scheduleEnabled"
                   :value="true"
                   v-model="modelData.scheduleEnabled"
                   id="scheduleEnabledTrue"/>
            <label for="scheduleEnabledTrue">
              {{ $t('yes') }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input type=radio
                   :value="false"
                   name="scheduleEnabled"
                   v-model="modelData.scheduleEnabled"
                   id="scheduleEnabledFalse"/>
            <label for="scheduleEnabledFalse">
              {{ $t('no') }}
            </label>
          </div>

          <span class="help-block">
                {{ $t('scheduledExecution.property.scheduleEnabled.description') }}
          </span>
        </div>
      </div>

      <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
          {{ $t('scheduledExecution.property.executionEnabled.label') }}
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input type="radio"
                   name="executionEnabled"
                   :value="true"
                   v-model="modelData.executionEnabled"
                   id="executionEnabledTrue"/>
            <label for="executionEnabledTrue">
              {{ $t('yes') }}
            </label>
          </div>

          <div class="radio radio-inline">
            <input type=radio
                   :value="false"
                   name="executionEnabled"
                   v-model="modelData.executionEnabled"
                   id="executionEnabledFalse"/>
            <label for="executionEnabledFalse">
              {{ $t('no') }}
            </label>
          </div>

          <span class="help-block">
                {{ $t('scheduledExecution.property.executionEnabled.description') }}
          </span>
        </div>
      </div>
</div>
</template>
<script lang="ts">
import axios from 'axios'
import InlineValidationErrors from '../../form/InlineValidationErrors.vue'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop, Watch} from 'vue-property-decorator'

import {
  getDays,
  getMonths,
  getSimpleDecomposition,
} from "./services/scheduleDefinition"


@Component({components: {InlineValidationErrors}})
export default class ScheduleEditor extends Vue {
  @Prop({required: true})
  value: any

  @Prop({required: true})
  eventBus!: Vue

  @Prop({required: false, default: false})
  useCrontabString!: boolean

  labelColClass = 'col-sm-2 control-label'
  fieldColSize = 'col-sm-10'

  modelData: any = {}

  name: string = ""
  errors: string = ""
  hours: any = []
  minutes:any = []
  days:any = []
  months:any = []
  crontabpos:number = -1

  async beforeMount() {
    var hours = <any>[]
    var minutes = <any>[]
    let _win = window as any
    _win.jQuery.each([...Array(24).keys()], function(index:any, value:any){
      hours.push(value< 10? '0'+value.toString(): value.toString())
    });
    _win.jQuery.each([...Array(60).keys()], function(index:any, value:any){
      minutes.push(value< 10? '0'+value.toString(): value.toString())
    });
    this.hours = hours;
    this.minutes = minutes;
    this.days = getDays();
    this.months = getMonths();
  }
  async mounted() {
    this.modelData = Object.assign({
      selectedDays: [],
      selectedMonths: [],
      useCrontabString: this.useCrontabString
    }, this.value)
    
    if(this.modelData.useCrontabString)
      this.showCronExpression()
    else
      this.showSimpleCron()
  }

  loadScheduleIntoSimpleTab (decomposedSchedule:any){
    this.modelData.hourSelected = decomposedSchedule.hour;
    this.modelData.minuteSelected = decomposedSchedule.minute;
    this.modelData.selectedDays = decomposedSchedule.days.length <= 7 ? decomposedSchedule.days : [];
    this.modelData.selectedMonths = decomposedSchedule.months.length <= 12 ? decomposedSchedule.months : [];
    this.modelData.everyDayOfWeek = decomposedSchedule.days.length === 7;
    this.modelData.allMonths = decomposedSchedule.months.length === 12;
  }

  showSimpleCron(){
    if(this.modelData.crontabString) {
      let cronComponents = this.modelData.crontabString.split(" ");

      let decomposedSchedule = getSimpleDecomposition(
        cronComponents[2],
        cronComponents[1],
        cronComponents[5],
        cronComponents[4],
      );
      this.loadScheduleIntoSimpleTab(decomposedSchedule);
    }
    this.modelData.useCrontabString = false;
  }
  showCronExpression(){
    this.modelData.useCrontabString = true;
  }
  async validateCronExpression(){
    this.errors = ''
    axios({
      method: "post",
      headers: {
      },
      params: {
        project: window._rundeck.projectName,
        crontabString: this.modelData.crontabString
      },
      url: new URL(`scheduledExecution/checkCrontab`, window._rundeck.rdBase).toString(),
      withCredentials: true
    }).then(response => {
      this.errors = response.request.response
    })
  }
  @Watch('modelData', {deep: true})
  wasChanged() {
    this.$emit('input', this.modelData)
  }
  crontabBlur(){
    this.validateCronExpression()
    this.crontabpos=-1
  }
  updateCrontabPosition(){
    this.crontabpos = this.getCaretPos(this.$refs['crontabInput']);
  }

  get crontabHint(){
    return this.modelData.useCrontabString ?
      this.getHintText(this.crontabpos, this.modelData.crontabString)
      : ''
  }

  /**
   * Get crontab position hint string
   * @param pos caret position within string
   * @param text string
   */
  getHintText(pos:number, text:string){
    let c = this.getCrontabSection(pos,text)
    if (c >= 0 && c <= 6) {
      return this.$t(`cron.section.${c}`)
    }
    return ''
  }

  /**
   * Get crontab position hint string
   * @param pos caret position within string
   * @param text string
   */
  getCrontabSection(pos:number, text:string):number{
    if (pos < 0) {
      return -1
    }
    //find # of space chars prior to pos
    let sub = text.substring(0, pos)
    let c = sub.split(' ').length
    if (c >= 1 && c <= 7) {
      return c-1
    }
    return -1
  }

  /**
   * Determine selection caret position within the input field
   * nb: there's probably a better/modern way to do this, this is old code
   * @param el input element
   */
  getCaretPos(el:any) {
    let rng, ii = -1;
    if (typeof el.selectionStart == "number") {
      return  el.selectionStart;
      //@ts-ignore
    } else if (document.selection && el.createTextRange) {
      //@ts-ignore
      rng = document.selection.createRange();
      rng.collapse(true);
      rng.moveStart("character", -el.value.length);
      return  rng.text.length;
    }
    return ii;
  }
}
</script>
