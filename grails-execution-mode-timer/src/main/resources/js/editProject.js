//= require lib/support
//= require lib/knockout.unobtrusive.min

var projectModeSupport = new ExecutionModeSupport()


jQuery(function () {
  "use strict";
  var pagePath = rundeckPage.path();
  var pluginName = RDPLUGIN['exec-mode-ui'];
  var pluginBase = rundeckPage.pluginBaseUrl(pluginName);

  if(pagePath === 'menu/projectHome'){
        var project = rundeckPage.project();
        var status = getProjectStatus(project)
        var messages = []

        if(status && status.execution && status.execution.active){
            messages.push(status.execution.msg)
        }

        if(status && status.schedule && status.schedule.active){
            messages.push(status.schedule.msg)

        }
        appendListMessage(messages)
  }

    if (pagePath === 'framework/editProject') {

        jQuery(".projectConfigurableTitle").css('font-weight', 'bold');
        jQuery(".projectConfigurableTitle").after("<br>");


        var project = rundeckPage.project();

        var status = getProjectStatus(project)
        var messages = []

        if(status && status.execution && status.execution.active){
            messages.push(status.execution.msg)
        }

        if(status && status.schedule && status.schedule.active){
            messages.push(status.schedule.msg)

        }
        appendListMessage(messages)

        var disableExecutionId = jQuery( "input[name^='extraConfig.scheduledExecutionService.disableExecution']" ).attr("id");
        var disableScheduledId = jQuery( "input[name^='extraConfig.scheduledExecutionService.disableSchedule']" ).attr("id");

        var disableExecutionLaterId = jQuery( "input[name^='extraConfig.updateModeProjectService.executionLaterDisable']" ).attr("id");
        var disableExecutionLaterValueId = jQuery( "input[name^='extraConfig.updateModeProjectService.executionLaterDisableValue']" ).attr("id");
        var enableExecutionLaterId = jQuery( "input[name^='extraConfig.updateModeProjectService.executionLaterEnable']" ).attr("id");
        var enableExecutionLaterValueId = jQuery( "input[name^='extraConfig.updateModeProjectService.executionLaterEnableValue']" ).attr("id");

        var disableScheduleLaterId = jQuery( "input[name^='extraConfig.updateModeProjectService.scheduledLaterDisable']" ).attr("id");
        var disableScheduleLaterValueId = jQuery( "input[name^='extraConfig.updateModeProjectService.scheduledLaterDisableValue']" ).attr("id");
        var enableScheduleLaterId = jQuery( "input[name^='extraConfig.updateModeProjectService.scheduledLaterEnable']" ).attr("id");
        var enableScheduleLaterValueId = jQuery( "input[name^='extraConfig.updateModeProjectService.scheduledLaterEnableValue']" ).attr("id");

        jQuery('label[for='+  disableExecutionLaterValueId  +']').css('font-weight', 'normal');
        jQuery('label[for='+  enableExecutionLaterValueId  +']').css('font-weight', 'normal');
        jQuery('label[for='+  disableScheduleLaterValueId  +']').css('font-weight', 'normal');
        jQuery('label[for='+  enableScheduleLaterValueId  +']').css('font-weight', 'normal');

        var executionDisable = jQuery('#' + disableExecutionId).is(":checked");
        var scheduleDisable = jQuery('#' + disableScheduledId).is(":checked");

        jQuery('#' + disableExecutionId).change(function() {
            enableDisableExecutionLater(this.checked, disableExecutionLaterId, disableExecutionLaterValueId, enableExecutionLaterId, enableExecutionLaterValueId)
        });

        jQuery('#' + disableScheduledId).change(function() {
            enableDisableScheduleLater(this.checked, disableScheduleLaterId,disableScheduleLaterValueId,  enableScheduleLaterId, enableScheduleLaterValueId)
        });

        enableDisableExecutionLater(executionDisable, disableExecutionLaterId, disableExecutionLaterValueId, enableExecutionLaterId, enableExecutionLaterValueId)
        enableDisableScheduleLater(scheduleDisable, disableScheduleLaterId,disableScheduleLaterValueId,  enableScheduleLaterId, enableScheduleLaterValueId)

        var ids = [{
          id: disableExecutionLaterId,
          value: disableExecutionLaterValueId,
          action: "disable",
          type: "execution"
        }, {
          id: enableExecutionLaterId,
          value: enableExecutionLaterValueId,
          action: "enable",
          type: "execution"
        }, {
          id: disableScheduleLaterId,
          value: disableScheduleLaterValueId,
          action: "disable",
          type: "schedule"
        }, {
          id: enableScheduleLaterId,
          value: enableScheduleLaterValueId,
          action: "enable",
          type: "schedule"
        }]

        ids.forEach((element, index, array) => {
            enableDisableValues(project, element.id, element.value, element.action, element.type, status)
        })
  }

  function enableDisableValues(project, id, valueId, action, type, status) {
        var enable = jQuery('#' + id).is(":checked");

        if(enable){
            var msg = getNextTimeMessage(status, project, type)
            jQuery( '#' + valueId ).parent().nextUntil('help-block').html(msg)
        }

        jQuery('#' + valueId).prop('disabled', true);

        if(enable){
            jQuery('#' + valueId).prop('disabled', false);
        }

        jQuery('#' + id).change(function() {
            if(this.checked) {
                jQuery('#' + valueId).prop('disabled', false);
            }else{
                jQuery('#' + valueId).prop('disabled', true);
                jQuery('#' + valueId).val("");
            }
        });
  }


  function enableDisableExecutionLater(executionDisable, disableExecutionLaterId, disableExecutionLaterValueId, enableExecutionLaterId, enableExecutionLaterValueId){
        if(executionDisable){
            jQuery( '#' + disableExecutionLaterId ).closest('.form-group').hide();
            jQuery( '#' + disableExecutionLaterValueId ).closest('.form-group').hide();
            jQuery( '#' + disableExecutionLaterId ).prop("checked", false);
            jQuery( '#' + disableExecutionLaterValueId ).val("");

            jQuery( '#' + enableExecutionLaterId ).closest('.form-group').show();
            jQuery( '#' + enableExecutionLaterValueId ).closest('.form-group').show();
        }else{
            jQuery( '#' + enableExecutionLaterId ).closest('.form-group').hide();
            jQuery( '#' + enableExecutionLaterValueId ).closest('.form-group').hide();
            jQuery( '#' + enableExecutionLaterId ).prop("checked", false);
            jQuery( '#' + enableExecutionLaterValueId ).val("");

            jQuery( '#' + disableExecutionLaterId ).closest('.form-group').show();
            jQuery( '#' + disableExecutionLaterValueId ).closest('.form-group').show();
        }
  }

  function enableDisableScheduleLater(scheduleDisable, disableScheduleLaterId,disableScheduleLaterValueId,  enableScheduleLaterId, enableScheduleLaterValueId){
        if(scheduleDisable){
            jQuery( '#' + disableScheduleLaterId ).closest('.form-group').hide();
            jQuery( '#' + disableScheduleLaterValueId ).closest('.form-group').hide();
            jQuery( '#' + disableScheduleLaterId ).prop("checked", false);
            jQuery( '#' + disableScheduleLaterValueId ).val("");

            jQuery( '#' + enableScheduleLaterId ).closest('.form-group').show();
            jQuery( '#' + enableScheduleLaterValueId ).closest('.form-group').show();
        }else{
            jQuery( '#' + enableScheduleLaterId ).closest('.form-group').hide();
            jQuery( '#' + enableScheduleLaterValueId ).closest('.form-group').hide();
            jQuery( '#' + enableScheduleLaterId ).prop("checked", false);
            jQuery( '#' + enableScheduleLaterValueId ).val("");

            jQuery( '#' + disableScheduleLaterId ).closest('.form-group').show();
            jQuery( '#' + disableScheduleLaterValueId ).closest('.form-group').show();
         }

  }

  function getProjectStatus(project){
      var appBase = projectModeSupport._app_base_url(rundeckPage.baseUrl());
      var url = appBase + "/project/"+project+"/configure/executionLater/nextTime"

      var status = null

      jQuery.ajax({
                url: url,
                method: 'GET',
                contentType: 'application/json',
                dataType: 'json',
                async: false,
                success: function (data) {
                    status = data
                }
            });

      return status
  }

  function getNextTimeMessage(status, project, type){

    var msg = null
    var action = null

    if(type == "execution"){
        if(status && status.execution && status.execution.active == true){
            msg = status.execution.msg
            action = status.execution.action
        }
    }

    if(type == "schedule"){
        if(status && status.schedule && status.schedule.active == true){
            msg = status.schedule.msg
            action = status.schedule.action
        }
    }

    if(msg == null){
        return null
    }

    var alertType = "alert-success"
    if(action == "disable"){
        alertType = "alert-danger"
    }

    return "<br><div class='alert "+alertType+"'><span class=''>" +msg+ "</span></div>"
  }

  function appendMessage(action, message){
    var para = document.createElement("div");

    var executionModeDiv = document.createElement("div");
    executionModeDiv.id = "executionModeLater";
    para.appendChild(executionModeDiv);

    var x = jQuery("#section-content").prepend(para);

    var type = "alert-success"
    if(action == "disable"){
        type = "alert-danger"
    }

    jQuery("#executionModeLater").prepend("<div class='alert "+type+"'><a class='close' data-dismiss='alert' href='#' aria-hidden='true'>&times;</a> <h5> <i class='far fa-clock'></i>  "+message+"</h5></div>");

  }

  function appendListMessage(messages){

   if(messages.length == 0){
    return
   }
    var para = document.createElement("div");

    var executionModeDiv = document.createElement("div");
    executionModeDiv.id = "projectExecutionInfo";
    para.appendChild(executionModeDiv);

    var x = jQuery("#section-content").prepend(para);

    var type = "alert-info"

    var html = [
        "<div class='alert alert-info'>",
        "<h4 class='alert-heading'>Project Execution Status",
        "<a class='close' data-dismiss='alert' href='#' aria-hidden='true'>&times;</a></h4>",
        "<ul>"
    ];

    for (var index = 0; index < messages.length; index++) {
        html.push("<li><h5> <i class='far fa-clock'></i> "+messages[index]+"</h5></li>")
    }
    html.push("</ul>")
    html.push("</div>")
    jQuery("#projectExecutionInfo").prepend(html.join(''));



  }


});
