<template>
  <div>
    <modal id="persistScheduleModal" v-model="showModal" :title="$t('Persist Schedules')" size="lg" :footer=false @hide="close(false)">
      <div class="alert alert-danger" v-if="persistErrors">
        <ul>
          <li>
            <span v-model="persistErrors">{{persistErrors}}</span>
          </li>
        </ul>
      </div>
      <div class="base-filters">
        <div class="row">
          <div class="col-xs-12 col-sm-4">
            <div class="form-group">
              <input
                id="name"
                name="name"
                type="text"
                class="form-control"
                v-model="name"
                placeholder="Schedule Name"
              >
            </div>
          </div>
          <div class="col-xs-12 col-sm-4">
            <div class="form-group">
              <input
                id="description"
                name="description"
                type="text"
                class="form-control"
                v-model="description"
                placeholder="Schedule Description"
              >
            </div>
          </div>
        </div>
        <div class="row">
          <ul class="nav nav-tabs">
            <li id="simpleLi" v-bind:class="{active: !isCronExpression? true: false}">
              <a data-toggle="tab"
                 data-crontabstring="false"
                 href="#cronsimple"
                 @click="showSimpleCron"
              >{{$t('Simple')}}</a>
            </li>
            <li id="cronLi" v-bind:class="{active: isCronExpression? true: false}">
              <a data-toggle="tab"
                 data-crontabstring="true"
                 href="#cronstrtab"
                 @click="showCronExpression"
              >{{$t('Crontab')}}</a>
            </li>
          </ul>
          <div class="col-xs">
            <div class="form-group">

              <div id="cronsimple" v-if="!isCronExpression">
                <div class="panel panel-default panel-tab-content form-inline crontab tabtarget" >
                  <div class="panel-body">
                    <div class="col-sm-4" id="hourTab">
                      <select
                        name="hour"
                        v-model="hourSelected"
                        class="form-control"
                        style="width:auto;"
                      >
                        <option v-for="hour in this.hours" :key="hour" v-bind:value="hour">{{hour}}</option>
                      </select>
                      :
                      <select
                        name="minutes"
                        v-model="minuteSelected"
                        class="form-control"
                        style="width:auto;"
                      >
                        <option v-for="minute in minutes" :key="minute" v-bind:value="minute">{{minute}}</option>
                      </select>
                    </div>

                    <div class="col-sm-4">
                      <div  class="checklist sepT"
                            id="DayOfWeekDialog">
                        <input
                          id="everyDay"
                          type="checkbox"
                          value="all"
                          v-model="allDays"
                        >
                        {{$t('Every Day')}}
                        <div v-if="!allDays" class="_defaultInput" v-for="shortDay in shortDays">
                          <input
                            id="dayCheckbox"
                            type="checkbox"
                            :value="shortDay"
                            v-model="selectedDays"
                          >
                          {{shortDay}}
                        </div>
                      </div>
                    </div>
                    <div class="col-sm-4">
                      <div  class="checklist sepT"
                            id="MonthDialog">
                        <input
                          id="everyMoth"
                          type="checkbox"
                          value="all"
                          v-model="allMonths"
                        >
                        {{$t('Every Month')}}
                        <div v-if="!allMonths" class="_defaultInput" v-for="shortMonth in shortMonths">
                          <input
                            id="mothCheckbox"
                            type="checkbox"
                            :value="shortMonth"
                            v-model="selectedMonths"
                          >
                          {{shortMonth}}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div id="cronstrtab" v-if="isCronExpression">
                <div class="panel panel-default"  >
                  <div class="panel-body">
                    <div class="container">
                      <div class="row">
                        <div class="col-sm-6">
                          <div class="form-group">
                            <input
                              type="text"
                              name="crontabString"
                              autofocus="true"
                              class="form-control input-sm"
                              size="50"
                              @change="validateCronExpression"
                              @blur="validateCronExpression"
                              v-model="scheduleToPersist.cronString"
                            >
                          </div>
                        </div>
                        <div class="col-sm-2" v-if="false">
                          <span v-if="false" id="crontooltip" class="label label-info form-control-static" style="padding-top:10px;" v-model="name">{{name}}</span>
                        </div>
                      </div>
                      <div class="row">
                        <div class="col-sm-6">
                          <template>
                            <div v-html="errors">
                            </div>
                          </template>
                        </div>
                      </div>
                      <div class="row">
                        <div class="text-primary col-sm-12">
                          <div>
                            Ranges: <code>1-3</code>.  Lists: <code>1,4,6</code>. Increments: <code>0/15</code> "every 15 units starting at 0".
                          </div>
                          See: <a href="${g.message(code:'documentation.reference.cron.url')}" class="external" target="_blank">Cron reference</a> for formatting help
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-xs-12 col-sm-4">
            <div class="form-group">
              <button type="button" class="btn btn-default" @click="close(false)">Close</button>
              <button type="button" class="btn btn-default" @click="save">Save</button>
            </div>
          </div>
        </div>
      </div>
    </modal>
  </div>
</template>

<script>

    import moment from 'moment'
    import ScheduleUtils from '../utils/ScheduleUtils'
    import axios from 'axios'

    export default {
        name: "SchedulePersist",
        props: ['editOpen', 'eventBus','schedule'],
        data : function() {
            return {
                showModal: true,
                isCronExpression: false,
                hours: [],
                hourSelected: "00",
                minutes:[],
                minuteSelected: "00",
                days: [],
                months: [],
                allDays: true,
                allMonths: true,
                name: "",
                description: "",
                scheduleToPersist: {},
                errors: "",
                persistErrors: null,
                selectedDays: [],
                selectedMonths: [],
                shortMonths: [],
                shortDays: []
            }
        },
        methods: {
            save(){
                if(this.errors === null || !this.errors.trim()){
                    console.log(this.errors)
                    this.scheduleToPersist.type = this.isCronExpression? 'CRON' : 'SIMPLE'
                    this.scheduleToPersist.name = this.name
                    this.scheduleToPersist.description = this.description
                    this.scheduleToPersist.id = this.schedule? this.schedule.id : null
                    this.scheduleToPersist.project = window._rundeck.projectName;
                    if( !this.isCronExpression ) this.mapSimpleToCronExpression();
                    this.persistSchedule()
                }
            },
            close(reload){
                this.eventBus.$emit('closeSchedulePersistModal', {'reload': reload});
                this.$emit('closeSchedulePersistModal', true)
            },
            mapSimpleToCronExpression(){
              this.scheduleToPersist.cronString = ScheduleUtils.fromSimpleToCronExpression(
                  this.hourSelected,
                  this.minuteSelected,
                  this.selectedDays,
                  this.selectedMonths,
                  this.allDays,
                  this.allMonths
              );
            },
            populateEditValues(){
                if(this.schedule){
                    if(this.schedule.type === 'CRON'){
                        this.isCronExpression = true
                        this.scheduleToPersist.cronString = ScheduleUtils.getCronExpression(this.schedule)
                    } else if(this.schedule.type === 'SIMPLE'){
                        this.isCronExpression = false;

                        var decomposedSchedule = ScheduleUtils.getSimpleDecomposition(
                            this.schedule.schedule.hour,
                            this.schedule.schedule.minute,
                            this.schedule.schedule.dayOfWeek,
                            this.schedule.schedule.month,
                        );

                        this.loadScheduleIntoSimpleTab(decomposedSchedule);
                    }

                    this.name = this.schedule.name
                    this.description = this.schedule.description
                }
            },
            loadScheduleIntoSimpleTab (decomposedSchedule){
                this.hourSelected = decomposedSchedule.hour;
                this.minuteSelected = decomposedSchedule.minute;
                this.selectedDays = decomposedSchedule.days.length < 7 ? decomposedSchedule.days : [];
                this.selectedMonths = decomposedSchedule.months.length < 12 ? decomposedSchedule.months : [];
                this.allDays = decomposedSchedule.days.length == 7;
                this.allMonths = decomposedSchedule.months.length == 12;
            },
            showSimpleCron(){
                var cronComponents = this.scheduleToPersist.cronString.split(" ");

                var decomposedSchedule = ScheduleUtils.getSimpleDecomposition(
                    cronComponents[1],
                    cronComponents[2],
                    cronComponents[5],
                    cronComponents[4],
                );
                this.loadScheduleIntoSimpleTab(decomposedSchedule);

                this.isCronExpression = false;
            },
            showCronExpression(){
                this.mapSimpleToCronExpression();
                this.isCronExpression = true;
            },
            persistSchedule() {
                return axios({
                    method: 'post',
                    headers: {'x-rundeck-ajax': true},
                    url: `/projectSchedules/persistSchedule`,
                    params: {
                        project: window._rundeck.projectName
                    },
                    data: {
                        schedule: this.scheduleToPersist
                    },
                    withCredentials: true
                }).then((response) => {
                    if(response.data.errors != null){
                        this.persistErrors = response.data.errors
                    }else{
                        this.persistErrors = null
                        this.close(true)
                    }
                })
            },
            validateCronExpression(){
                this.errors = null
                return axios({
                    method: 'post',
                    headers: {'x-rundeck-ajax': true},
                    url: `/scheduledExecution/checkCrontab`,
                    params: {
                        project: window._rundeck.projectName,
                        crontabString: this.scheduleToPersist.cronString
                    },
                    evalScripts:true,
                    withCredentials: true
                }).then((response) => {
                    this.errors = response.request.response
                })
            }

        },
        beforeMount() {
            var hours = []
            var minutes = []
            jQuery.each([...Array(24).keys()], function(index, value){
                hours.push(value< 10? '0'+value.toString(): value.toString())
            })
            jQuery.each([...Array(60).keys()], function(index, value){
                minutes.push(value< 10? '0'+value.toString(): value.toString())
            })
            this.hours = hours
            this.minutes = minutes
            this.days = moment.localeData('en').weekdays()
            this.months = moment.localeData('en').months()

            var shortMonths = []
            jQuery.each(moment.localeData('en').monthsShort(), function(index, item){shortMonths.push(item.toUpperCase())})
            this.shortMonths = shortMonths

            var shortDays = [];
            jQuery.each(moment.localeData('en').weekdaysShort(), function(index, item){shortDays.push(item.toUpperCase())})
            this.shortDays = shortDays;
        },
        mounted(){
            this.populateEditValues()
        }
    }
</script>

<style lang="scss" scoped>

</style>
