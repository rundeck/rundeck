//= require moment.min
//= require bootstrap-datetimepicker.min

/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the 'License');
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an 'AS IS' BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/**
 * Verify whether the date and time selected in the date and time picker
 * is valid.
 *
 * It returns false if the crontab string is unset, or if the date is in
 * the past.
 **/
function isValidDate() {
    var isValid     = false;
    var runAtTime   = jQuery('#runAtTime').val()

    if (runAtTime !== undefined && runAtTime !== '') {
        // Get the last date which was set
        var $picker     = jQuery('#datetimepicker');
        var startTime   = $picker.data('DateTimePicker').date();

        if (startTime !== null && startTime.isAfter(moment())) {
            isValid     = true;
        }
    }

    return isValid;
}

/**
 * Toggle display of the alert div which signifies that the date/time is invalid.
 **/
function toggleAlert(invalid) {
    if (invalid) {
        jQuery('#dateAlert').css('display', 'block');
        jQuery('#dateAlert').addClass('in');
        jQuery('.schedule-button').prop('disabled', true);
    } else {
        jQuery('#dateAlert').css('display', 'none');
        jQuery('#dateAlert').removeClass('in');
        jQuery('.schedule-button').prop('disabled', false);
    }
}

/**
 * Handle each modification to the date/time on the date and time picker.
 * Convert the currently selected date and time to a valid crontab string
 * and store it in a hidden input for submission later.
 **/
function onDateChanged(event) {
    if (event.date !== undefined) {
        var formattedDate   = event.date.format();
        if (formattedDate !== 'Invalid date') {
            jQuery('#runAtTime').val(event.date.format());
        }
    }
    toggleAlert(!isValidDate());
}

/**
 * Called when the 'Schedule' button is clicked.
 *
 * Checks the date and time to ensure it is valid, if it is, the enclosing
 * form is submitted with a custom action.
 **/
function onScheduleSubmit() {
    var $btn = jQuery('#execFormRunButton');
    var $form = $btn.closest('form');

    if (!isValidDate()) {
        return false;
    }
    $form.submit(function onScheduleFormSubmit() {
        var $tempElement = jQuery('<input type="hidden"/>');
        $tempElement.attr('name', '_action_runJobLater')
            .appendTo($form);
    });
    $form.submit();
}

/**
 * Called when the scheduler popover is displayed. Initialises the date and time
 * picker to the current time.
 **/
function onSchedulerPopover() {
    jQuery('#scheduleSubmitButton').click(onScheduleSubmit);

    jQuery('#datetimepicker').datetimepicker({
        format: 'ddd, MMM D YYYY, HH:mm',
        minDate: moment()
    }).on('dp.change', onDateChanged);
}

// Initialise the popover for scheduling
jQuery(document).ready(function() {
    jQuery('#showScheduler').popover({
        html: true
    }).on('shown.bs.popover', onSchedulerPopover);
});
