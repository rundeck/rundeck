// Ready translated locale messages
const translationStrings = {
    en_US: {
        Edit: 'Edit',
        Save: 'Save',
        Delete: 'Delete',
        Cancel: 'Cancel',
        Revert: 'Revert',
        jobAverageDurationPlaceholder:'leave blank for Job Average duration',
        resourcesEditor: {
            'Dispatch to Nodes':'Dispatch to Nodes',
            Nodes:'Nodes'
        },
        uiv: {
            modal: {
                cancel: "Cancel",
                ok: "OK"
            }
        },
        cron:{
            section:{
                0:'Seconds',
                1:'Minutes',
                2:'Hours',
                3:'Day of Month',
                4:'Month',
                5:'Day of Week',
                6:'Year'
            }
        }
    }
}

module.exports = {
    messages: translationStrings
}
