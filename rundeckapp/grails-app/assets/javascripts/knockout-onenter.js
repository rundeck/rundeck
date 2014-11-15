//= require knockout.min
/*
 */
ko.bindingHandlers.executeOnEnter = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {
        var handler = allBindings.get('executeOnEnter');
        jQuery(element).keypress(function (event) {
            var keyCode = (event.which ? event.which : event.keyCode);
            if (keyCode === 13) {
                handler.call(bindingContext.$data,event);
                return false;
            }
            return true;
        });
    }
};
