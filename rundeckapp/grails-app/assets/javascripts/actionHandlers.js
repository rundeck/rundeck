/**
 * Created by greg on 2/25/16.
 */
function ActionHandlers() {
    var self = this;
    self.handlers = {};
    self.registerHandler = function (key, func) {
        self.handlers[key] = func;
    };
    self.triggerHandler = function (key, data) {
        self.handlers[key](data);
    };

    /**
     * Register the handler for an action as a modal toggle
     * @param key
     */
    self.registerModalHandler=function(key,target,data){
        self.registerHandler(key,function(el){
           jQuery(target).modal(data);
        });
    };

    self.init = function () {
        jQuery(document.body).on('click', '.page_action', function (e) {

            var el = jQuery(this);
            if (el.is('a')) e.preventDefault();
            var handler = el.data('action');
            self.triggerHandler(handler, el);
        });
    };

}
var PageActionHandlers;
jQuery(function () {
    PageActionHandlers = new ActionHandlers();
    PageActionHandlers.init();
});