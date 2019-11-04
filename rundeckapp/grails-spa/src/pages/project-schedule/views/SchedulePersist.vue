<template>
  <div>
    <modal id="persistScheduleModal" v-model="showModal" :title="$t('title.persistSchedules')" size="lg" :footer=false @hide="close(false)">
      <div class="row">
        <div class="col-xs-12">
          <div class="alert alert-danger" v-if="persistErrors">
            <ul>
              <li>
                <span v-model="persistErrors">{{persistErrors}}</span>
              </li>
            </ul>
          </div>
          <div class="alert alert-danger" v-if="validationErrors.length > 0 ">
            <ul>
              <li v-for="validationError in validationErrors">
                <span>{{validationError}}</span>
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
                    :placeholder="$t('placeholder.scheduleName')"
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
                    :placeholder="$t('placeholder.scheduleDescription')"
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
                  >{{$t('label.simpleCron')}}</a>
                </li>
                <li id="cronLi" v-bind:class="{active: isCronExpression? true: false}">
                  <a data-toggle="tab"
                     data-crontabstring="true"
                     href="#cronstrtab"
                     @click="showCronExpression"
                  >{{$t('label.fullCrontab')}}</a>
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
                            {{$t('label.everyDay')}}
                            <div v-if="!allDays" class="_defaultInput" v-for="day in days">
                              <label>
                              <input
                                id="dayCheckbox"
                                type="checkbox"
                                :value="day.shortName"
                                v-model="selectedDays"
                              ></label>
                              {{day.name}}
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
                            {{$t('label.everyMonth')}}
                            <div v-if="!allMonths" class="_defaultInput" v-for="month in months">
                              <input
                                id="mothCheckbox"
                                type="checkbox"
                                :value="month.shortName"
                                v-model="selectedMonths"
                              >
                              {{month.name}}
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
                                  v-model="scheduleToPersist.crontabString"
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
                                {{$t('label.cronSyntax1')}}: <code>1-3</code>.  {{$t('label.cronSyntax2')}}: <code>1,4,6</code>. {{$t('label.cronSyntax3')}}: <code>0/15</code> "{{$t('label.cronSyntax4')}}".
                              </div>
                              {{$t('label.cronHelp1')}}: <a :href="$t('href.cronDocumentation')" class="external" target="_blank">{{$t('label.cronHelp2')}}</a> {{$t('label.cronHelp3')}}
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
        </div>
      </div>
    </modal>
  </div>
</template>

<script>

    import axios from 'axios'
    import {
        fromSimpleToCronExpression,
        getCronExpression,
        getDays,
        getMonths,
        getSimpleDecomposition, persistSchedule, validateCronExpression
    } from "../scheduleDefinition";

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
                validationErrors: [],
                selectedDays: [],
                selectedMonths: []
            }
        },
        methods: {
            save(){
                if(!this.isCronExpression && !this.validateSimpleCronSelection()){
                   return;
                }

                if(this.errors === null || !this.errors.trim()){
                    this.scheduleToPersist.type = this.isCronExpression? 'CRON' : 'SIMPLE'
                    this.scheduleToPersist.name = this.name
                    this.scheduleToPersist.description = this.description
                    this.scheduleToPersist.id = this.schedule? this.schedule.id : null
                    this.scheduleToPersist.project = window._rundeck.projectName;
                    if( !this.isCronExpression ) this.mapSimpleToCronExpression();
                    this.persistSchedule()
                }
            },
            validateSimpleCronSelection() {
              this.validationErrors = [];
              if(!this.allDays && this.selectedDays.length == 0 ){
                this.validationErrors.push (this.$i18n.t("validation.noDaySelected"));
              }
              if(!this.allMonths && this.selectedMonths.length == 0){
                this.validationErrors.push( this.$i18n.t("validation.noMonthSelected"));
              }
              if(this.validationErrors.length == 0){
                  return true
              }
              return false
            },
            close(reload){
                this.eventBus.$emit('closeSchedulePersistModal', {'reload': reload, result: reload});
                this.$emit('closeSchedulePersistModal', true)
            },
            mapSimpleToCronExpression(){
              this.scheduleToPersist.crontabString = fromSimpleToCronExpression(
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
                        this.scheduleToPersist.crontabString = getCronExpression(this.schedule)
                    } else if(this.schedule.type === 'SIMPLE'){
                        this.isCronExpression = false;

                        var decomposedSchedule = getSimpleDecomposition(
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
                var cronComponents = this.scheduleToPersist.crontabString.split(" ");

                var decomposedSchedule = getSimpleDecomposition(
                    cronComponents[2],
                    cronComponents[1],
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
            async persistSchedule() {
                var result = await persistSchedule(this.scheduleToPersist)
                if(result.errors != null){
                    this.persistErrors = result.errors
                }else{
                    this.persistErrors = null
                    this.close(true)
                }
            },
            async validateCronExpression(){
                this.errors = null
                return axios({
                    method: 'post',
                    headers: {'x-rundeck-ajax': true},
                    url: `/scheduledExecution/checkCrontab`,
                    params: {
                        project: window._rundeck.projectName,
                        crontabString: this.scheduleToPersist.crontabString
                    },
                    evalScripts:true,
                    withCredentials: true
                }).then((response) => {
                    this.errors = response.request.response
                })
            }

        },
        beforeMount() {
            var hours = [];
            var minutes = [];
            jQuery.each([...Array(24).keys()], function(index, value){
                hours.push(value< 10? '0'+value.toString(): value.toString())
            });
            jQuery.each([...Array(60).keys()], function(index, value){
                minutes.push(value< 10? '0'+value.toString(): value.toString())
            });
            this.hours = hours;
            this.minutes = minutes;
            this.days = getDays();
            this.months = getMonths();
        },
        mounted(){
            this.populateEditValues()
        }
    }
</script>

<style lang="scss" scoped>

</style>
