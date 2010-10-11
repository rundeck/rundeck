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
        //load remote values
        function _loadRemoteOptionValues(elem,schedId,optName,prefix,value){
            $(elem).loading('Loading option values&hellip;');
            new Ajax.Updater(
                elem,
                "${createLink(controller:'scheduledExecution',action:'loadRemoteOptionValues')}",
                {
                    parameters:{option:optName,id:schedId,fieldPrefix:prefix,selectedvalue:value},
                    evalScripts:true,
                    onSuccess: function(transport) {
                        $(elem).show();

                    },
                }
            );
        }
</script>