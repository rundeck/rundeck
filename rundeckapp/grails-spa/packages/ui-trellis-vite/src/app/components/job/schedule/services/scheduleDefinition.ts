import * as moment from 'moment'

import {getRundeckContext} from '@/library/rundeckService'

const rundeckContext = getRundeckContext()

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

export function getCronExpression(scheduleDef :any) {
  return scheduleDef.crontabString || scheduleDef.schedule && [scheduleDef.schedule.seconds ? scheduleDef.schedule.seconds : '0', scheduleDef.schedule.minute, scheduleDef.schedule.hour, scheduleDef.schedule.dayOfMonth.toUpperCase(), scheduleDef.schedule.month.toUpperCase(), scheduleDef.schedule.dayOfMonth == '?' ? scheduleDef.schedule.dayOfWeek.toUpperCase() : '?', scheduleDef.schedule.year ? scheduleDef.schedule.year : '*'].join(" ") || ''
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
    dayOfWeek ? dayOfWeek : '*', daysofweeklist, false);

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
    let smallResults = splitItems.map(item => valueToName(item, listOfLongValues, indexStartAtZero)) as any
    return flatten(smallResults);
  }
}

export function valueToIndex(value : string, listOfNames : string[], indexStartAtZero : boolean) {
  if (/\d/.test(value)) {
    return indexStartAtZero ? parseInt(value) : parseInt(value) - 1
  } else {
    return indexStartAtZero ? listOfNames.indexOf(value) : (listOfNames.indexOf(value) + 1)
  }
}

export function valueToName(value : string, listOfNames : string[], indexStartAtZero : boolean) {
  if (/\d/.test(value)) {
    const index = parseInt(value)
    return indexStartAtZero ? listOfNames[index] : listOfNames[index - 1]
  } else {
    return value;
  }
}

export function fromSimpleToCronExpression(hourSelected : string , minuteSelected : string, selectedDays : string[], selectedMonths : string[], allDays : boolean, allMonths :boolean) {
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
