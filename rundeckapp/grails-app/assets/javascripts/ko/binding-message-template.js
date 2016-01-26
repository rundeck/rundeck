//= require knockout.min
/*
 * Useful for i18n messages; replace the text content of an element, by substituting values into placeholders.
 * Placeholders are in the form '{0}','{1}', etc.  The "messageTemplate" binding value can be a single value, which
 * will be used for {0}, or it can be an object with a 'value' property, possibly observable, containing an array
 * for the replacement values.
 *
 */
ko.bindingHandlers.messageTemplate = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {

        var text=jQuery(element).text();
        jQuery(element).data('ko-message-template',text);
        return { 'controlsDescendantBindings': true };
    },
    update:function(element, valueAccessor, allBindings, viewModel, bindingContext){
        var data=ko.utils.unwrapObservable(valueAccessor());
        var template=jQuery(element).data('ko-message-template');
        var values=[];
        if(typeof(data)!='object'){
            values=[data];
        }else if(typeof(data)=='object'){
            values=ko.utils.unwrapObservable(data['value']);
            if(typeof(values)!='array'){
                values=[values];
            }
        }
        var text = template.replace(/\{(\d+)\}/g,function(match, g1, offset, string){
            var val= parseInt(g1);
            if(val>=0 && val<values.length){
                return values[val];
            }else{
                return string;
            }
        });
        ko.utils.setTextContent(element, text);
    }
};
