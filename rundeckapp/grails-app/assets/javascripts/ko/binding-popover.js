//= require knockout.min
/**
 * hook to initialize custom bootstrap popover mechanism
 * &ltdiv data-bind="bootstrapPopover: true, bootstrapPopoverContentRef: '#elemid' &gt;
 * or
 * &ltdiv data-bind="bootstrapPopover: true, bootstrapPopoverContentFor: '#elemid' &gt;
 */
ko.bindingHandlers.bootstrapPopover = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        if(allBindings.get('bootstrapPopoverContentRef')){
            _initPopoverContentRef(null,{element:element,contentRef:ko.unwrap(allBindings.get('bootstrapPopoverContentRef'))});
        }else if (allBindings.get('bootstrapPopoverContentFor')) {
            _initPopoverContentFor(null,{element:element,target:ko.unwrap(allBindings.get('bootstrapPopoverContentFor'))});
        }
    }
};

/**
 * Initializes bootstrap tooltip on the dom element. Usage: &lt;div data-bind="bootstrapTooltip: true" title="blah" &gt;
 * tip: if the title of the element is bound to an observable, pass the same one as the binding, like
 * &lt;div data-bind="bootstrapTooltip: mytooltipObservable" title="blah" &gt;, to trigger updates when it changes.
 * @type {{init: ko.bindingHandlers.bootstrapTooltip.init}}
 */
ko.bindingHandlers.bootstrapTooltip = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        "use strict";
        jQuery(element).tooltip({});
    },
    update: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        "use strict";
        var val = valueAccessor();
        if(ko.isObservable(val)){
            val = ko.unwrap(val);
            jQuery(element).tooltip('destroy');
            jQuery(element).data('original-title',null);
            jQuery(element).tooltip({});
        }
    }
};
