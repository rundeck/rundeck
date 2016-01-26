//= require knockout.min
/*
 * hook to initialize custom bootstrap popover mechanism
 */
ko.bindingHandlers.bootstrapPopover = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        console.log("bootstrapPopover init for elem:",jQuery(element),allBindings.get('bootstrapPopoverContentRef'));
        if(allBindings.get('bootstrapPopoverContentRef')){
            _initPopoverContentRef(null,{element:element,contentRef:ko.unwrap(allBindings.get('bootstrapPopoverContentRef'))});
        }else if (allBindings.get('bootstrapPopoverContentFor')) {
            _initPopoverContentFor(null,{element:element,contentRef:ko.unwrap(allBindings.get('bootstrapPopoverContentFor'))});
        }
    }
};
