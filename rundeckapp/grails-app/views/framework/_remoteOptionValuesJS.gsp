<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
    _remoteOptionValuesJS.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jun 14, 2010 5:48:00 PM
    $Id$
 --%>

<script type="text/javascript">
        //add a reload button for remote option values
        function _addReloadRemoteOptionValues(elem, schedId, optName, prefix, value,reloader,obs,form) {
            var btn;
            var icn;
            if($(reloader)){
                btn=$(reloader);
                icn=$(reloader).down('span.remotestatus');
                icn.show();
            }else{
                btn= new Element('span');
                icn= new Element('span');
            }
            btn.addClassName('action');
            btn.addClassName('textbtn');
            if($(elem).down("._error_detail")) {
                icn.removeClassName('ok');
                icn.addClassName('error');
            } else {
                icn.removeClassName('error');
                icn.addClassName('ok');
            }

            btn.setAttribute('title','Click to reload the remote option values for: '+optName);
            if(obs){
                if(! $(btn).retrieve('_reloadObserver')){
                    //only add observer once
                    var observer= function (e) { _remoteOptionControl(form).loadRemoteOptionValues(optName); };
                    Event.observe(btn,'click',observer);
                    $(btn).store('_reloadObserver', observer);
                }
            }
            if(!$(reloader)){
                setText(btn,'reload');
                $(btn).insert({bottom:icn});
                $(elem).insert({bottom:btn});
            }
        }
        //load remote values
        function _loadRemoteOptionValues(elem,schedId,optName,prefix,value,reloader, obs, form){
            if($(elem).down('.loading')){
                $(elem).down('.loading').loading('');
            }
            var params;
            if(null!=form){
                params=Form.serialize(form);
            }
            var paramsObj= {option:optName, id:schedId, fieldPrefix:prefix, selectedvalue:value};
            new Ajax.Updater(
                elem,
                "${createLink(controller:'scheduledExecution',action:'loadRemoteOptionValues')}",
                {
                    parameters:params ? params + '&' + Object.toQueryString(paramsObj):paramsObj,
                    evalScripts:true,
                    onComplete: function(transport) {
                        if (transport.request.success()) {
                            $(elem).show();
                            _addReloadRemoteOptionValues(elem,schedId,optName,prefix,value,reloader, obs, form);
                        }
                    }
                }
            );
        }

/**
 * Manages cascading reloads of remote option values based on a dependency graph
 */
var RemoteOptionControl = Class.create({
    /**
    * initialize with ID of a form or element containing all the option value fields
     * @param formId
     */
    initialize:function (formId) {
        /**
         * ID of container element/form for all input fields
         */
        this.formId=formId;
        /**
         * container for form input field IDs for options, keyed by option name
         */
        this.ids={};
        /**
         * container for array of params used for ajax reloads, keyed by option name
         */
        this.options={};
        /**
         * container for array of dependent option names, keyed by option name
         */
        this.dependents={};
        /**
         * container for array of depdency option names, keyed by option name
         */
        this.dependencies={};
        /**
         * list of option names
         */
        this.names= new Array();
        this.nameset= {};
        /**
         * indicates if observing was started
         */
        this.observing=false;
        /**
         * container of observer functions, keyed by option name
         */
        this.observers={};
        /**
         * container of true/false whether the option should automatically trigger reload of dependents at startup, keyed by option name
         */
        this.autoreload={};
        /**
         * container of true/false whether the option should automatically load at startup, keyed by option name
         */
        this.loadonstart = {};
        /**
         * field observer frequency (in seconds)
         */
        this.observeFreq=0.5;
        /**
         * indicates cyclic dependencies
         */
        this.cyclic=false;
    },
    /**
    * register an option with parameters used for Ajax reload of the field (used to call _loadRemoteOptionValues function)
    * @param name option name
    * @param elem element id containing ajax field content
    * @param schedId Job ID
    * @param optName option name
    * @param prefix field name prefix string
    * @param value existing value of the field
    * @param reloader element ID for a button to reload the remote option value ajax
    * @param obs true/false whether to enable reload button
     */
    addOption:function (name, elem, schedId, optName, prefix, value, reloader, obs) {
        this.options[name]= new Array(elem, schedId, optName, prefix, value, reloader, obs,this.formId);
        this.names.push(name);
        this.nameset[name]=true;
    },
    addLocalOption:function(name){
        this.names.push(name);
    },
    /**
    * reload the values for an option by name (calls _loadRemoteOptionValues)
    * @param name
     */
    loadRemoteOptionValues:function(name){
        //stop observing option name if doing so
        this.stopObserving(name);
        _loadRemoteOptionValues.apply(null, this.options[name]);
    },
    /**
    * register new value of an option into the reload ajax params
    * @param name
    * @param value
     */
    setOptionValue:function(name,value){
        if(this.options[name]){
            this.options[name][4]=value;
        }
    },
    /**
    * define dependent option names for an option
    * @param name
    * @param depsArr
     */
    addOptionDeps: function(name, depsArr){
        this.dependents[name]=depsArr;
    },
    /**
    * define dependency option names for an option
    * @param name
    * @param depsArr
     */
    addOptionDependencies: function(name, depsArr){
        this.dependencies[name]=depsArr;
    },
    /**
    * notify that a value changed for an option by name, will reload dependents if any
    * @param name
    * @param value
     */
    optionValueChanged: function(name,value){
        this.setOptionValue(name,value);
        //trigger reload
        if(this.dependents[name] && !this.cyclic){
            var formOpts;
            for(var i=0;i<this.dependents[name].length;i++){
                if(!formOpts) { formOpts = Form.serialize(this.formId,true); }
                var dependentName = this.dependents[name][i];
                var skip = false;
                // if any of the dependencies does not have value, and is required, then skip.
                for(var j=0;j<this.dependencies[dependentName].length;j++){
                    var dependencyName = this.dependencies[dependentName][j];
                    if(! formOpts['extra.option.'+dependencyName]) {
                        var reqSigns = $$('#'+dependencyName+'_state .reqwarning');
                        if (reqSigns.length > 0 && reqSigns[0].visible()) {
                            skip = true;
                            break;
                        }
                    }
                }
                if (!skip) {
                    this.loadRemoteOptionValues(this.dependents[name][i])
                }
            }
        }
    },
    /**
    * sets the input field ID for an option name, used for observing changes, will restart observation if already observing
    * @param name
    * @param id
     */
    setFieldId:function(name,id){
        this.ids[name]=id;
        if(this.observing){
            if(this.observers[name]){
               this.observers[name].stop();
            }
            this.observeChangesFor(name);
            var auto=this.doOptionAutoReload(name);
            if (!auto && this.options[name]) {
                //if already observing, and value now differs, trigger reload
                var lastValue = this.options[name][4];
                var value = $F(id);
                if (Object.isString(lastValue) && Object.isString(value) ?
                    lastValue != value : String(lastValue) != String(value)) {
                    this.optionValueChanged(name, value);
                }
            }
        }
    },
    observeMultiCheckbox:function (name,e) {
        var roc = this;
        if(!$(e)){
           throw "not found: "+e;
        }
        Element.observe(e, 'change', function (evt, value) {
            roc.optionValueChanged(name, value);
        });
    },
    setFieldMultiId: function(name,id){

        if (this.observing) {
            var found= $(id).select("input[type='checkbox']");
            if(found){
                found.each(this.observeMultiCheckbox.bind(this,name));
            }
            var auto = this.doOptionAutoReload(name);
            if (!auto && this.options[name]) {
                //if already observing, and value now differs, trigger reload
                this.optionValueChanged(name, '');
            }
        }
    },
    setFieldRemoteEmpty:function(name){
        if(this.ids[name] && this.options[name] && this.dependencies[name]){
            var elem = $(this.options[name][0]);
            if(!elem.down('div.emptyMessage')){
                //wrap elem contents and hide it
                jQuery(elem).wrapInner("<div style='display:none' class='fieldcontent'></div>");
                //insert note
                var note = new Element('div',{'class':'info note emptyMessage'});
                note.appendChild(document.createTextNode('No values to choose from. '));
                elem.insert({top:note});
            }
        }
    },
    doOptionAutoReload: function(name){
        if (this.autoreload[name]) {
            if(!this.ids[name]){
                return;
            }
            if(!$(this.ids[name])){
                return;
            }
            //trigger change immediately
            var value = $F(this.ids[name]);
            this.optionValueChanged(name, value);
            return true;
        }
        return false;
    },
    /**
    * set autoreload value for the option
    * @param name
    * @param value
     */
    setOptionAutoReload: function(name,value){
        this.autoreload[name]=value;
    },
    /**
    * starts observing changes for option field by name
    * @param name
     */
    observeChangesFor:function(name){
        this.stopObserving(name);
        var id = this.ids[name];

        if (!id || !$(id)) {
            return;
        }
        var roc = this;
        //observe field value change and trigger reloads
        this.observers[name] = Event.observe(id, 'change', function(evt) {
            roc.optionValueChanged(name, this.value);
        });
    },
    onStartObserve:function(){
        for (var i = 0; i < this.names.length; i++) {
            var name=this.names[i];
            if(this.loadonstart[name]){
                this.ids[name]=null;
                this.loadRemoteOptionValues(name);
            }
        }
    },
    /**
    * starts observing all option fields for changes
     */
    observeChanges:function(){
        this.onStartObserve();
        for(var i=0;i<this.names.length;i++){
            this.observeChangesFor(this.names[i]);
        }
        this.observing=true;
        for (var i = 0; i < this.names.length; i++) {
            this.doOptionAutoReload(this.names[i]);
        }
    },
    stopObserving: function(name){
        if (this.observers[name]) {
            var id = this.ids[name];
            Event.stopObserving(id,'change');
            this.observers[name]=null;
        }
    },
    /**
     * stop all observing
     */
    unload:function(){
        this.observing=false;
        for(var i = 0 ; i < this.names.length ; i ++){
            var name=this.names[i];
            this.stopObserving(name);
        }
        this.observers={};
        _unloadRemoteOptionControl(this.formId);
    },
    /**
    * load remote option dataset from json data
    * @param data
     */
    loadData:function(data){
        for(var opt in data){
            var params=data[opt];
            if(params['optionDependencies']){
                this.addOptionDependencies(opt,params['optionDependencies']);
            }
            if(params['optionDeps']){
                this.addOptionDeps(opt,params['optionDeps']);
            }
            if(params['optionAutoReload']){
                this.setOptionAutoReload(opt,params['optionAutoReload']);
            }
            if(params['hasUrl']){
                this.addOption(opt, params['holder'], params['scheduledExecutionId'], opt, params['usePrefix'], params['selectedOptsMap'], params['fieldNameKey'], true);
                if(params['loadonstart']){
                    this.loadonstart[opt]=true;
                }
                if(params['optionAutoReload']){
                    this.setOptionAutoReload(opt, true);
                }
                if(params['fieldMultiId']){
                    this.setFieldMultiId(opt,params['fieldMultiId']);
                }
                if(params['fieldId']){
                    this.setFieldId(opt,params['fieldId']);
                }
            }else{
                this.addLocalOption(opt);
            }
        }
    }
});
        /**
        *  create or get a RemoteOptionControl for the given element ID
        * @param id element id
        * @return instance of RemoteOptionControl
        * @private
         */
        function _remoteOptionControl(id){
            var roc=$(id).retrieve('_remoteOptionControl');
            if(!roc){
                roc=new RemoteOptionControl(id);
                $(id).store('_remoteOptionControl',roc);
            }
            return roc;
        }
        function _unloadRemoteOptionControl(id){
            $(id).store('_remoteOptionControl', null);
        }
</script>
