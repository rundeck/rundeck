//= require knockout.min
/*
 source: http://stackoverflow.com/questions/14838135/how-to-use-knockout-to-iterate-over-an-object-not-array
 */
ko.bindingHandlers.foreachprop = {
    transformObject: function (obj) {
        var properties = [];
        for (var key in obj) {
            if (obj.hasOwnProperty(key)) {
                properties.push({ key: key, value: obj[key] });
            }
        }
        return properties;
    },
    init: function (element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        var value = ko.utils.unwrapObservable(valueAccessor()),
            properties = ko.bindingHandlers.foreachprop.transformObject(value);
        var childBindingContext = bindingContext.createChildContext(
            bindingContext.$rawData,
            null // Optionally, pass a string here as an alias for the data item in descendant contexts
        );
        ko.applyBindingsToNode(element, { foreach: properties }, childBindingContext);
        return { controlsDescendantBindings: true };
    }
};
