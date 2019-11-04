export default {

  getCronExpression: function (scheduleDef) {
    return [scheduleDef.schedule.seconds ? scheduleDef.schedule.seconds : '0', scheduleDef.schedule.minute, scheduleDef.schedule.hour, scheduleDef.schedule.dayOfMonth.toUpperCase(), scheduleDef.schedule.month.toUpperCase(), scheduleDef.schedule.dayOfMonth == '?' ? scheduleDef.schedule.dayOfWeek.toUpperCase() : '?', scheduleDef.schedule.year ? scheduleDef.schedule.year : '*'].join(" ")
  },

  getSimpleDecomposition: function (hour, minute, dayOfWeek, month) {
    var daysofweeklist = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    var monthsofyearlist = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

    var decomposedDayOfWeek = this.decomposeUsingValues(
      dayOfWeek ? dayOfWeek : '*', daysofweeklist, true);

    var decomposedMonthsOfYear = this.decomposeUsingValues(
      month ? month : '*', monthsofyearlist, false);


    return {
      hour: hour ? hour : '00',
      minute: minute ? minute : '00',
      days: decomposedDayOfWeek,
      months: decomposedMonthsOfYear
    }
  },

  flatten: function (arr) {
    var thiz = this;
    return arr.reduce(function (flat, toFlatten) {
      return flat.concat(Array.isArray(toFlatten) ? thiz.flatten(toFlatten) : toFlatten);
    }, [])
  },

  decomposeUsingValues: function (value, listOfLongValues, indexStartAtZero) {
    if (!value) return [];
    if (value == '*') return listOfLongValues;
    if (value == '?') return listOfLongValues;

    var splitItems = value.split(',');

    if (splitItems.length == 1) {
      var item = value;
      var isRange = item.indexOf('-') > -1;

      if (isRange) {
        var rangeComponents = item.split('-');

        var startIndex = this.valueToIndex(rangeComponents[0], listOfLongValues, indexStartAtZero);
        var endIndex = this.valueToIndex(rangeComponents[1], listOfLongValues, indexStartAtZero);

        return listOfLongValues.slice(startIndex, endIndex + 1);
      } else {
        return [this.valueToName(item, listOfLongValues, indexStartAtZero)];
      }

    } else {
      var smallResults = splitItems.map(item => this.decomposeUsingValues(item, listOfLongValues, indexStartAtZero));
      return this.flatten(smallResults);
    }
  },

  valueToIndex: function (value, listOfNames, indexStartAtZero) {
    if (/\d/.test(value)) {
      return +value;
    } else {
      return (indexStartAtZero) ? listOfNames.indexOf(value) : (listOfNames.indexOf(value) + 1);
    }
  },
  valueToName: function (value, listOfNames, indexStartAtZero) {
    if (/\d/.test(value)) {
      return (indexStartAtZero) ? (listOfNames[value]) : listOfNames[value - 1];
    } else {
      return value;
    }
  },
  fromSimpleToCronExpression(hourSelected, minuteSelected, selectedDays, selectedMonths, allDays, allMonths) {
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
};
