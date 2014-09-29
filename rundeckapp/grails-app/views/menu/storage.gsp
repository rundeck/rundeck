<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/29/14
  Time: 3:04 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html xmlns="http://www.w3.org/1999/html">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title>Storage</title>
    <asset:javascript src="storageBrowseKO.js"/>
    <g:javascript>
        var storageBrowse;
        function init() {
            var rootPath = '';
//            if (!rootPath.startsWith("keys/")) {
//                rootPath = "keys";
//            }
            storageBrowse = new StorageBrowser(appLinks.storageKeysApi, rootPath);
            storageBrowse.allowUpload(true);
            storageBrowse.allowNotFound(true);
            ko.applyBindings(storageBrowse);
            jQuery('#storageupload').find('.obs-storageupload-select').on('click', function (evt) {
                var file = jQuery('#storageuploadfile').val();
                console.log("upload: " + file);
                jQuery('#uploadForm')[0].submit();
            });
        }
        jQuery(init);
    </g:javascript>
</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>

<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'storage']"/>
    </div>

    <div class="col-sm-9">

        <span class="h3">Storage</span>



        <g:render template="/framework/storageBrowser"/>

        <g:uploadForm controller="storage" action="keyStorageUpload" id="uploadForm" useToken="true">
        <div class="modal" id="storageupload" tabindex="-1" role="dialog"
             aria-labelledby="storageuploadtitle"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="storageuploadtitle">Upload a file:</h4>
                    </div>

                    <div class="modal-body" style="max-height: 500px; overflow-y: scroll">
                        <div>
                            Destination path:
                            <h4><span data-bind="text: selectedPath()"></span></h4>
                        </div>
                        <input type="file" name="storagefile" id="storageuploadfile"/>
                        <input type="hidden" name="resourcePath" data-bind="value: selectedPath()"/>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-sm btn-default"
                                data-dismiss="modal">Cancel</button>
                        %{--<button type="button"
                                class="btn btn-sm btn-success obs-storageupload-select"
                                xdata-bind="css: selectedPath()?'active':'disabled' "
                                data-dismiss="modal">
                            Upload
                        </button>--}%
                        <input type="submit"
                                class="btn btn-sm btn-success obs-storageupload-select"
                                name="Upload"/>
                    </div>
                </div>
            </div>
        </div>
        </g:uploadForm>

    </div>
</div>
</body>
