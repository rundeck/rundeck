import { client } from '../../services/rundeckClient'
import * as moment from 'moment';

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

import axios from 'axios'

export interface ScheduleDefinition {
  id: number
  name : string
  description : string
  project : string
  minute : string
  hour : string
  dayOfMonth : string
  month : string
  dayOfWeek : string
  seconds : string
  year : string
  crontabString : string
  type : string
  scheduledExecutions: Object[]
}

export interface ScheduleSearchResult {
  schedules: ScheduleDefinition []
  maxRows: number
  offset: number
  totalRecords: number
}

export interface StandardResponse {
  messages: string[]
  success: boolean
}

export interface Day{
  name: string,
  shortName: string
}

export interface Month{
  name: string,
  shortName: string
}

export function getMonths(){
  let monthIndexes = [ 0, 1,2,3,4,5,6,7,8,9,10,11 ];

  let ans = [] as Month[];
  let chosenLocale = moment.localeData('en');
  for( let idx of monthIndexes){
    ans.push ( {
      name: chosenLocale.months()[idx],
      shortName: chosenLocale.monthsShort()[idx].toUpperCase()
    });
  }

  return ans;
}

export function getDays(){
  let daysIndexes = [ 0,1,2,3,4,5,6 ];

  let ans = [] as Day[];
  let chosenLocale = moment.localeData('en');
  for( let idx of daysIndexes){
    ans.push ( {
      name: chosenLocale.weekdays()[idx],
      shortName: chosenLocale.weekdaysShort()[idx].toUpperCase()
    });
  }

  return ans;
}

export async function getAllProjectSchedules(offset: number, scheduleName: any, filteredNames: Array<string>): Promise<ScheduleSearchResult> {

  const rundeckContext = getRundeckContext()
  let offsetString = String(offset)
  const resp = await client.sendRequest({
    pathTemplate: `/api/${rundeckContext.apiVersion}/project/{project}/schedules`,
    pathParameters: {project: rundeckContext.projectName},
    queryParameters: {project: rundeckContext.projectName, offset: offsetString, name: scheduleName},
    body: {filteredNames : filteredNames},
    baseUrl: rundeckContext.rdBase,
    method: 'POST'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error getting schedule definitions for project  ${rundeckContext.projectName}`)
  }
  else {
    return resp.parsedBody as ScheduleSearchResult
  }
}

export async function bulkDeleteSchedules(schedulesId: []): Promise<StandardResponse> {
  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: `/api/${rundeckContext.apiVersion}/project/{project}/schedules/bulkScheduleDelete`,
    pathParameters: {project: rundeckContext.projectName},
    body: { schedulesId: schedulesId},
    baseUrl: rundeckContext.rdBase,
    method: 'POST'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error execution bulk delete for project  ${rundeckContext.projectName}`)
  } else {
    return resp.parsedBody as StandardResponse
  }
}

export async function deleteSchedule(schedule: any): Promise<any> {
  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: `/api/${rundeckContext.apiVersion}/project/{project}/schedules/deleteSchedule`,
    pathParameters: {project: rundeckContext.projectName},
    body: { schedule: schedule},
    baseUrl: rundeckContext.rdBase,
    method: 'POST'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error deleting schedule ${schedule.name}`)
  } else {
    return resp.parsedBody
  }
}

export async function reassociate(scheduleDefId: any, jobUuidsToAssociate: any, jobUuidsToDeassociate: any): Promise<any> {
  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: `/api/${rundeckContext.apiVersion}/project/{project}/schedules/reassociate`,
    pathParameters: {project: rundeckContext.projectName},
    body: { scheduleDefId: scheduleDefId,
      jobUuidsToAssociate: jobUuidsToAssociate,
      jobUuidsToDeassociate: jobUuidsToDeassociate},
    baseUrl: rundeckContext.rdBase,
    method: 'POST'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error Associating Jobs to Schedule for project  ${rundeckContext.projectName}`)
  } else {
    return resp.parsedBody
  }
}

export async function persistSchedule(scheduleToPersist: any): Promise<any> {
  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: `/api/${rundeckContext.apiVersion}/project/{project}/schedules/persistSchedule`,
    pathParameters: {project: rundeckContext.projectName},
    body: { schedule: scheduleToPersist},
    baseUrl: rundeckContext.rdBase,
    method: 'POST'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error persistSchedule for project  ${rundeckContext.projectName}`)
  } else {
    return resp.parsedBody
  }
}

export async function getJobsAssociated(offset: any, scheduleName: any): Promise<any>{
  const rundeckContext = getRundeckContext()
  let offsetString = String(offset)
  const resp = await client.sendRequest({
    pathTemplate: '/projectSchedules/getJobsAssociated',
    queryParameters: {project: rundeckContext.projectName, offset: offsetString, scheduleName: scheduleName},
    baseUrl: rundeckContext.rdBase,
    method: 'GET'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error jobs associated to schedule ${scheduleName}`)
  } else {
    return resp.parsedBody
  }
}

export async function jobsSearchJson(pagination: any, searchName: any): Promise<any>{
  const rundeckContext = getRundeckContext()
  let offsetString = String(pagination.offset)
  let maxString = String(pagination.max)
  const resp = await client.sendRequest({
    pathTemplate: '/menu/jobsSearchJson',
    queryParameters: {projFilter: rundeckContext.projectName, offset: offsetString, max: maxString, jobFilter: searchName},
    baseUrl: rundeckContext.rdBase,
    method: 'GET'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error Searching for jobs with name ${searchName}`)
  } else {
    return resp.parsedBody
  }
}

export async function persistUploadedDefinitions(formData: any){
  const rundeckContext = getRundeckContext()
  return axios({
    method: "post",
    headers: {
      "x-rundeck-ajax": true,
      "Content-Type": "multipart/form-data"
    },
    data: formData,
    params: { project: rundeckContext.projectName},
    url: `${window._rundeck.rdBase}projectSchedules/uploadFileDefinition`,
    withCredentials: true
  }).then(response => {
    if (response.data.errors) {
      return response.data.errors
    }else{
      return true
    }
  })
}


export function getCronExpression(scheduleDef :any) {
  return [scheduleDef.schedule.seconds ? scheduleDef.schedule.seconds : '0', scheduleDef.schedule.minute, scheduleDef.schedule.hour, scheduleDef.schedule.dayOfMonth.toUpperCase(), scheduleDef.schedule.month.toUpperCase(), scheduleDef.schedule.dayOfMonth == '?' ? scheduleDef.schedule.dayOfWeek.toUpperCase() : '?', scheduleDef.schedule.year ? scheduleDef.schedule.year : '*'].join(" ")
}

function padLeft(value: string, filler: string, targetLength: number) {
  if( !value ) value = "";
  if( targetLength < 0 ) return value;
  if( !filler || filler.length < 1) return value;

  let ans = value.trim();
  while( ans.length < targetLength ){
    ans = filler + ans;
  }

  return ans;
}

export function getSimpleDecomposition(hour : string, minute : string, dayOfWeek : string, month : string) {
  var daysofweeklist = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  var monthsofyearlist = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

  var decomposedDayOfWeek = decomposeUsingValues(
    dayOfWeek ? dayOfWeek : '*', daysofweeklist, true);

  var decomposedMonthsOfYear = decomposeUsingValues(
    month ? month : '*', monthsofyearlist, false);


  return {
    hour: hour ? padLeft(hour, "0",2) : '00',
    minute: minute ? padLeft(minute, "0", 2) : '00',
    days: decomposedDayOfWeek,
    months: decomposedMonthsOfYear
  }
}

export function flatten(arr : any) {
  return arr.reduce(function (flat : any, toFlatten :any) {
    return flat.concat(Array.isArray(toFlatten) ? flatten(toFlatten) : toFlatten);
  }, [])
}

export function decomposeUsingValues(value : string, listOfLongValues : string[], indexStartAtZero : boolean) {
  if (!value) return [];
  if (value == '*') return listOfLongValues;
  if (value == '?') return listOfLongValues;

  var splitItems = value.split(',');

  if (splitItems.length == 1) {
    var item = value;
    var isRange = item.indexOf('-') > -1;

    if (isRange) {
      var rangeComponents = item.split('-');

      var startIndex = valueToIndex(rangeComponents[0], listOfLongValues, indexStartAtZero);
      var endIndex = valueToIndex(rangeComponents[1], listOfLongValues, indexStartAtZero);

      return listOfLongValues.slice(startIndex, endIndex + 1);
    } else {
      return [valueToName(item, listOfLongValues, indexStartAtZero)];
    }

  } else {
    let smallResults = splitItems.map(item => decomposeUsingValues(item, listOfLongValues, indexStartAtZero))  as any ;
    return flatten(smallResults);
  }
}

export function valueToIndex(value : string, listOfNames : string[], indexStartAtZero : boolean) {
  if (/\d/.test(value)) {
    return +value;
  } else {
    return (indexStartAtZero) ? listOfNames.indexOf(value) : (listOfNames.indexOf(value) + 1);
  }
}

export function valueToName(value : string, listOfNames : string[], indexStartAtZero : boolean) {
  if (/\d/.test(value)) {
    return (indexStartAtZero) ? (listOfNames[+value]) : listOfNames[(+value) - 1];
  } else {
    return value;
  }
}

export function fromSimpleToCronExpression(hourSelected : string , minuteSelected : string, selectedDays : string[], selectedMonths : string[], allDays : string[], allMonths :string[]) {
  return [
    '0',
    minuteSelected,
    hourSelected,
    '?',
    allMonths? '*' :
      selectedMonths.length == 12 ? '*' : selectedMonths.join(','),
    allDays? '*' :
      selectedDays.length == 7 ? '*' : selectedDays.join(','),
    '*'
  ].join(" ")
}

export function loadJsonData(id : string) {
  var dataElement = document.getElementById(id);
  // unescape the content of the span
  if (!dataElement) {
    return null;
  }
  var jsonText = dataElement.textContent || dataElement.innerText;
  return jsonText && jsonText != '' ? JSON.parse(jsonText) : null;
}
