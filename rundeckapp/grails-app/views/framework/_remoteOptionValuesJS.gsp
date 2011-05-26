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
        function _addReloadRemoteOptionValues(elem, schedId, optName, prefix, value) {
            var btn = new Element('span');
            btn.addClassName('action');
            btn.addClassName('textbtn');
            if($(elem).innerHTML.indexOf("_error_detail")<0){
                btn.addClassName('minor');
            }

            btn.setAttribute('title','Click to reload the remote option values for: '+optName);
            Event.observe(btn,'click',function(e){
                //look for selected value
                var newvalue=value;
                if ($(elem).down('input')) {
                    newvalue = $(elem).down('input').value;
                }else if ($(elem).down('select')) {
                    newvalue = $(elem).down('select').value;
                }
                _loadRemoteOptionValues(elem,schedId,optName,prefix,newvalue);
            });
            btn.innerHTML='reload';
            $(elem).insert({bottom:btn});
        }
        //load remote values
        function _loadRemoteOptionValues(elem,schedId,optName,prefix,value){
            $(elem).loading('Loading option values&hellip;');
            new Ajax.Updater(
                elem,
                "${createLink(controller:'scheduledExecution',action:'loadRemoteOptionValues')}",
                {
                    parameters:{option:optName,id:schedId,fieldPrefix:prefix,selectedvalue:value},
                    evalScripts:true,
                    onComplete: function(transport) {
                        if (transport.request.success()) {
                            $(elem).show();
                            _addReloadRemoteOptionValues(elem,schedId,optName,prefix,value);
                        }
                    }
                }
            );
        }
</script>