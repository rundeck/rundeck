//= require lib/support
//= require lib/knockout.unobtrusive.min

var executionModeSupport = new ExecutionModeSupport()

jQuery(function () {
  "use strict";
  var pagePath = rundeckPage.path();
  var pluginName = RDPLUGIN['exec-mode-ui'];
  var pluginBase = rundeckPage.pluginBaseUrl(pluginName);

  var ViewModel = function(executionMode, activeLater, activeLaterValue, passiveLater, passiveLaterValue, message) {
        this.executionMode = ko.observable(executionMode)
        this.activeLater = ko.observable(activeLater)
        this.passiveLater = ko.observable(passiveLater)
        this.activeLaterValue = ko.observable(activeLaterValue)
        this.passiveLaterValue = ko.observable(passiveLaterValue)

        this.message = ko.computed(function() {
            return message;
        }, this);

        this.setupKnockout = function () {
            executionModeSupport.setup_ko_loader('ui-execution-mode-later', pluginBase, pluginName);

            //custom bindings
            ko.bindingHandlers.bootstrapTooltipTrigger = {
                update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
                    var val = valueAccessor();
                    if (ko.isObservable(val)) {
                        val = ko.unwrap(val);
                        jQuery(element).tooltip(val ? 'show' : 'hide');
                    }
                }
            };
        };
    };

    var ViewProjectModel = function(value) {
        this.disableExecution = ko.observable(value)
        this.disableSchedule = ko.observable(value)

        this.setupKnockout = function () {
            executionModeSupport.setup_ko_loader('ui-execution-mode-later', pluginBase, pluginName);

            //custom bindings
            ko.bindingHandlers.bootstrapTooltipTrigger = {
                update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
                    var val = valueAccessor();
                    if (ko.isObservable(val)) {
                        val = ko.unwrap(val);
                        jQuery(element).tooltip(val ? 'show' : 'hide');
                    }
                }
            };
        };
    };

    let appBase = executionModeSupport._app_base_url(rundeckPage.baseUrl());

    if(pagePath === 'menu/projectHome' || pagePath === 'menu/home' || pagePath === 'menu/executionMode'){

        jQuery.ajax({
                url: appBase + "/menu/executionMode/executionLater/nextTime",
                method: 'GET',
                contentType: 'json',
                success: function (data) {
                    let status = data
                    if(status && status.active === true) {
                        appendMessage(status.action, status.msg)
                    }
                }
            })


   }


  if (pagePath === 'menu/executionMode') {
        var url = appBase + "/menu/executionMode/executionLater"

        var savedValues = null

        jQuery.ajax({
                url: url,
                method: 'GET',
                contentType: 'application/json',
                dataType: 'json',
                async: false,
                success: function (data) {
                    savedValues = data
                }
            });
        //console.log(savedValues)

        var activemode = jQuery('#activemode').is(":checked");
        var passiveemode = jQuery('#passivemode').is(":checked");
        var activeLater = false
        var passiveLater = false
        var activeLaterValue = ""
        var passiveLaterValue = ""

        if(savedValues && savedValues.active){
            if(savedValues.action == "disable"){
                passiveLater = true
                passiveLaterValue = savedValues.value
            }
            if(savedValues.action == "enable"){
                activeLater = true
                activeLaterValue = savedValues.value
            }
        }


       var bindings = {
            custom: {
                activemode: 'checked: executionMode',
                passivemode: 'checked: executionMode'
            }
        };


        var mode = "active"
        if(passiveemode){
            mode = "passive"
        }

        let executionModeForm = jQuery(`
            <ui-execution-mode-later-execution-mode params="executionMode: executionMode, activeLater: activeLater, passiveLater:passiveLater , activeLaterValue: activeLaterValue, passiveLaterValue: passiveLaterValue, message: message()"></ui-execution-mode-later-execution-mode>
            `);

        jQuery( ".card-content" ).append( executionModeForm );

        let viewModel = new ViewModel(mode, activeLater, activeLaterValue, passiveLater, passiveLaterValue, status.msg)
        viewModel.setupKnockout()
        ko.unobtrusive.createBindings(bindings);
        ko.applyBindings(viewModel);

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

    jQuery("#executionModeLater").prepend("<div class='alert "+type+"'><a class='close' data-dismiss='alert' href='#' aria-hidden='true'>&times;</a> <h4 class='alert-heading'>Execution Mode Status</h4> <span><i class='far fa-clock'></i> "+message+"</span></div>");

  }


});
