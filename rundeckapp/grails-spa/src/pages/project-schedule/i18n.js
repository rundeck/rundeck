// Ready translated locale messages
const translationStrings = {
  en_US: {
    message: {
      communityNews: 'Community News'
    },
    button:{
      actions: 'Actions',
      uploadScheduleDefinitions: 'Upload Schedule Definitions',
      editSchedule: 'Edit Schedule',
      assignToJobs: 'Assign To Job',
      createDefinition: 'Create Definition',
      deleteSchedule: 'Delete',
      bulkDelete: 'Bulk Delete',
      cancelBulkDelete: 'Cancel Bulk Delete',
      deleteSelected: 'Delete Selected Schedules',
      deleteConfirm: 'Do you really want to delete {0} Schedule Definitions? This will affect the following jobs.',
      close: 'Close',
      add: 'Add'
    },
    title:{
      assignedJobs: 'Assigned Jobs To Schedule {0}',
      bulkDeleteSchedules: 'Bulk Delete Schedule Definitions',
      deleteSchedules: 'Delete Schedule Definitions',
      persistSchedules: 'Persist Schedules'
    },
    validation:{
      noMonthSelected : 'No month selected',
      noDaySelected : 'No day selected'
    },
    placeholder:{
      searchByName : "Search by name",
      scheduleName : "Schedule Name",
      scheduleDescription : 'Schedule Description'
    },
    label : {
      assignUnassign : 'To assign/unassign',
      currentlyAssigned : 'Currently assigned',
      simpleCron: 'Simple',
      fullCrontab: 'Crontab',
      everyDay: 'Every day',
      everyMonth: 'Every month',
      cronSyntax1: 'Ranges',
      cronSyntax2: 'Lists',
      cronSyntax3: 'Increments',
      cronSyntax4: 'every 15 units starting at 0',
      cronHelp1: 'See',
      cronHelp2: 'Cron reference',
      cronHelp3: 'for formatting help',
      selectScheduleDefinitionFile: 'Select a Schedule definition file',
      delete: 'Delete',
      cancel: 'Cancel',
      questionDeleteSchedule: 'Delete this schedule?',
      assignSchedules: 'Assign Schedules to job',
      associateSchedule: 'Associate Schedule Definition'
    },
    href : {
      cronDocumentation: 'http://www.quartz-scheduler.org/documentation/quartz-2.2.2/tutorials/tutorial-lesson-06.html'
    }
  }
}

module.exports = {
  messages: translationStrings
}
