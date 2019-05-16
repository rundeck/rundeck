// Ready translated locale messages
const translationStrings = {
  en_US: {
    'bulk.edit': 'Bulk Edit',
    'in.of': 'in',
    'execution': 'Execution | Executions',
    'execution.count': '1 Execution | {0} Executions',
    'uiv.modal.ok': 'OK',
    'uiv.modal.cancel': 'Cancel',
    'Bulk Delete Executions: Results': 'Bulk Delete Executions: Results',
    'Requesting bulk delete, please wait.': 'Requesting bulk delete, please wait.',
    'bulkresult.attempted.text': '{0} Executions were attempted.',
    'bulkresult.success.text': '{0} Executions were successfully deleted.',
    'bulkresult.failed.text': '{0} Executions could not be deleted:',
    'delete.confirm.text': 'Really delete {0} {1}?',
    'clearselected.confirm.text': 'Clear all {0} selected items, or only items shown on this page?',
    'bulk.selected.count': '{0} selected',
    'results.empty.text': 'No results for the query',
    'Only shown executions': 'Only shown executions',
    'Clear bulk selection': 'Clear Bulk Selection',
    'Click to edit Search Query': 'Click to edit Search Query',
    'error.message.0': 'An Error Occurred: {0}',

    period: {
      label: {
        All: 'any time',
        Hour: 'in the last Hour',
        Day: 'in the last Day',
        Week: 'in the last Week',
        Month: 'in the last Month'
      }
    }
  }
}

module.exports = {
  messages: translationStrings
}
