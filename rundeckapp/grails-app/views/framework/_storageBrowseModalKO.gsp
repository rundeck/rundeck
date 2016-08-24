<div class="modal" id="storagebrowse" tabindex="-1" role="dialog" aria-labelledby="storagebrowsetitle"
     aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="storagebrowsetitle">%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<g:message code="storage.select.a.file" /></h4>
            </div>

            <div class="modal-body" style="max-height: 500px; overflow-y: scroll">
                <g:render template="/framework/storageBrowser"/>
            </div>

            <div class="modal-footer">
                <button type="button" class="btn btn-sm btn-default" data-dismiss="modal"><g:message code="cancel" /></button>
                <button type="button" class="btn btn-sm btn-success obs-storagebrowse-select"
                        data-bind="css: selectedPath()?'active':'disabled' "
                        data-dismiss="modal">
                    <g:message code="storage.choose.selected.key" />
                </button>
            </div>
        </div>
    </div>
</div>
<script lang="text/javascript">
    jQuery(function(){
        var storagebrowsemodal = jQuery('#storagebrowse');
        storagebrowsemodal.on('show.bs.modal',function (evt) {
            var rootPath = jQuery(evt.relatedTarget).data('storage-root');
            if (!rootPath.startsWith("keys/")) {
                rootPath = "keys";
            }
            var storageBrowse = jQuery(evt.delegateTarget).data('storageBrowser');
            var storageBrowseTarget = jQuery(evt.relatedTarget).data('field');
            if (storageBrowse == null) {
                storageBrowse= new StorageBrowser(appLinks.storageKeysApi, rootPath);
                storageBrowse.browseMode('select');
                storageBrowse.staticRoot(true);
                jQuery('body').data('storageBrowser', storageBrowse );
                jQuery(evt.delegateTarget).data('storageBrowser', storageBrowse);
                ko.applyBindings(storageBrowse,jQuery('#storagebrowse')[0]);
            }
            storageBrowse.fieldTarget(storageBrowseTarget);
        }).on('shown.bs.modal', function (evt) {
            var storageBrowseTarget = jQuery(evt.relatedTarget).data('field');
            var filter = jQuery(evt.relatedTarget).data('storage-filter');
            var selectedPath = jQuery(storageBrowseTarget).val();
            var storageBrowse = jQuery(evt.delegateTarget).data('storageBrowser');
            storageBrowse.browse(null, filter, selectedPath);
        });

        //modal "save" button should find target input field and set value
        storagebrowsemodal.find('.obs-storagebrowse-select').on('click',function(evt){
            if(jQuery(evt.delegateTarget).hasClass('active')){
                var storageBrowse = jQuery('#storagebrowse').data('storageBrowser');
                var storageBrowseTarget = storageBrowse.fieldTarget();
                if(storageBrowse && storageBrowse.selectedPath()){
                    jQuery(storageBrowseTarget).val(storageBrowse.selectedPath());
                    if(typeof(_storageBrowseSelected)=='function'){
                        _storageBrowseSelected(storageBrowseTarget,storageBrowse.selectedPath());
                    }
                    storageBrowse.selectedPath(null);
                }
            }
        });
    });
</script>
