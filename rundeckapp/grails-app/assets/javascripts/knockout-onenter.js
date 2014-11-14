//= require knockout.min
/*
 */
ko.bindingHandlers.executeOnEnter = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        jQuery(element).keypress(function (event) {
            var keyCode = (event.which ? event.which : event.keyCode);
            if (keyCode === 13) {
                allBindings.get('executeOnEnter').call(bindingContext.$data,event);
                return false;
            }
            return true;
        });
    }
};
