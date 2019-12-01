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
      deleteConfirm: 'Do you really want to delete {0} Schedule Definitions? This will affect the following jobs.'
    },
    title:{
      assignedJobs: 'Assigned Jobs To Schedule {0}'
    },
    validation:{
      noMonthSelected : 'No month selected',
      noDaySelected : 'No day selected'
    }
  }
}

module.exports = {
  messages: translationStrings
}
