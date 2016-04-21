//= require knockout.min
/*
 * Useful for i18n messages; replace the text content of an element, by substituting values into placeholders.
 * Placeholders are in the form '{0}','{1}', etc.  The "messageTemplate" binding value can be a single value, which
 * will be used for {0}, or it can be an object with a 'value' property, possibly observable, containing an array
 * for the replacement values.
 * If a binding "messageTemplatePluralize: true" is set, then the template text is treated as a singular and a plural
 * version of the same text, separated by "|" character.  If the first bound data value is "1", then singular form
 * is used, otherwise the plural form is used.
 *
 */
ko.bindingHandlers.messageTemplate = {
    init: function (element, valueAccessor, allBindings, viewModel, bindingContext) {

        var text=jQuery(element).text();
        jQuery(element).data('ko-message-template',text);
        return { 'controlsDescendantBindings': true };
    },
    update:function(element, valueAccessor, allBindings, viewModel, bindingContext){
        var pluralize=allBindings.get('messageTemplatePluralize');
        var data=ko.utils.unwrapObservable(valueAccessor());
        var template=jQuery(element).data('ko-message-template');
        var pluralTemplate=null;
        if(pluralize){
            var arr = template.split('|');
            template = arr[0];
            pluralTemplate = arr[1];
        }
        var values=[];
        if(typeof(data)!='object'){
            values=[data];
        }else if(jQuery.isArray(data)){
            values=data;
        }else if(typeof(data)=='object'){
            values=ko.utils.unwrapObservable(data['value']);
            if(!jQuery.isArray(values)){
                values=[values];
            }
        }
        for(var i=0;i<values.length;i++){
            values[i] = ko.utils.unwrapObservable(values[i]);
        }
        if(pluralize && values[0]!=1){
            template=pluralTemplate;
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
