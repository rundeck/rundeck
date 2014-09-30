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
            var rootPath = 'keys';
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
            var data = loadJsonData('storageData');
            storageBrowse.browse(null,null,data.resourcePath?data.resourcePath:null);
        }
        jQuery(init);
    </g:javascript>
</head>

<body>
<g:embedJSON id="storageData" data="[resourcePath:params.resourcePath]"/>
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

        <g:uploadForm controller="storage" action="keyStorageUpload" id="uploadForm" useToken="true" class="form-horizontal">
        <div class="modal" id="storageupload" tabindex="-1" role="dialog"
             aria-labelledby="storageuploadtitle"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="storageuploadtitle">Upload a File</h4>
                    </div>

                    <div class="modal-body" style="max-height: 500px; overflow-y: scroll">
                        <div class="form-group">
                            <label for="uploadResourcePath" class="col-sm-3 control-label">
                            Directory path:
                            </label>
                            <div class="col-sm-9">
                                <input data-bind="value: inputPath, valueUpdate: 'keyup' " id="uploadResourcePath" name="resourcePath" class="form-control" placeholder="Enter the directory name"/>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="uploadResourceName" class="col-sm-3 control-label">
                                File name:
                            </label>
                            <div class="col-sm-9">
                                <input data-bind="value: inputFilename, valueUpdate: 'keyup' "
                                       id="uploadResourceName" name="fileName" class="form-control" placeholder="Optional: specify a file name."/>
                                <div class="help-block">
                                    If not set, the name of the uploaded file is used.
                                </div>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="storageuploadfile" class="col-sm-3 control-label">
                               Choose a file:
                            </label>
                            <div class="col-sm-9">
                                <input type="file" name="storagefile" id="storageuploadfile"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="col-sm-12">
                                <div class="help-block">
                                    <p>You can reference this stored File using the storage path:</p>

                                    <p><strong data-bind="text: inputFullpath()"
                                               class="text-info"></strong></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-sm btn-default"
                                data-dismiss="modal">Cancel</button>
                        <input type="submit"
                                class="btn btn-sm btn-success obs-storageupload-select"
                                name="Upload"/>
                    </div>
                </div>
            </div>
        </div>
        </g:uploadForm>

        <g:form controller="storage" action="keyStorageUpload" id="uploadKeyForm" useToken="true" class="form-horizontal" role="form">
        <div class="modal" id="storageuploadkey" tabindex="-1" role="dialog"
             aria-labelledby="storageuploadtitle2"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="storageuploadtitle2">Enter a Key</h4>
                    </div>

                    <div class="modal-body" style="max-height: 500px; overflow-y: scroll">
                        <div class="form-group">
                            <label for="uploadResourcePath2" class="col-sm-3 control-label">
                            Directory path:
                            </label>
                            <div  class="col-sm-9">
                                <input data-bind="value: inputPath, valueUpdate: 'keyup' " id="uploadResourcePath2" name="resourcePath" class="form-control" placeholder="Enter the directory name"/>
                            </div>
                        </div>
                        <div class="form-group"
                             data-bind="css: { 'has-warning': !inputFilename(), 'has-success': inputFilename() }">
                            <label for="uploadResourceName2" class="col-sm-3 control-label">
                            File name:
                            </label>
                            <div class="col-sm-9">
                                <input  id="uploadResourceName2"
                                        data-bind="value: inputFilename, valueUpdate: 'keyup' " name="fileName" class="form-control" placeholder="Specify a file name."/>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="storageuploadtype" class="col-sm-3 control-label">
                               Key Type:
                            </label>
                            <div class="col-sm-9">
                                <select name="uploadKeyType" id="storageuploadtype" class="form-control">
                                    <option value="private">Private Key</option>
                                    <option value="public">Public Key</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="storageuploadtext" class="col-sm-3 control-label">
                               Paste Key Data:
                            </label>
                            <div class="col-sm-9">
                                <textarea class="form-control" rows="5" id="storageuploadtext" name="uploadText"></textarea>
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="col-sm-12">
                                <div class="help-block">
                                    <p>You can reference this stored Key using the storage path:</p>

                                    <p><strong data-bind="text: inputFullpath()"
                                               class="text-info"></strong></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-sm btn-default"
                                data-dismiss="modal">Cancel</button>

                        <input type="submit"
                                class="btn btn-sm btn-success obs-storageupload-select"
                                name="Save"/>
                    </div>
                </div>
            </div>
        </div>
        </g:form>

        <g:form controller="storage" action="keyStorageUpload" id="uploadPasswordForm" useToken="true" class="form-horizontal" role="form">
        <div class="modal" id="storageuploadpassword" tabindex="-1" role="dialog"
             aria-labelledby="storageuploadtitle3"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal"
                                aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="storageuploadtitle3">Save a Password</h4>
                    </div>

                    <div class="modal-body" style="max-height: 500px; overflow-y: scroll">

                        <div class="form-group">
                            <div  class="col-sm-12">
                                <div class="help-block">
                                    <p>Enter a password, and set a directory path and filename location to store it.</p>
                                </div>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="uploadResourcePath3" class="col-sm-3 control-label">
                            Directory path:
                            </label>
                            <div  class="col-sm-9">
                                <input data-bind="value: inputPath, valueUpdate: 'keyup' " id="uploadResourcePath3" name="resourcePath" class="form-control" placeholder="Enter the directory name"/>
                            </div>
                        </div>
                        <div class="form-group" data-bind="css: { 'has-warning': !inputFilename(), 'has-success': inputFilename() }">
                            <label for="uploadResourceName3" class="col-sm-3 control-label">
                            File name:
                            </label>
                            <div class="col-sm-9">
                                <input data-bind="value: inputFilename, valueUpdate: 'keyup' "
                                        id="uploadResourceName3" name="fileName" class="form-control" placeholder="Specify a file name."/>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="uploadpasswordfield" class="col-sm-3 control-label">
                               Password:
                            </label>
                            <div class="col-sm-9">
                                <input name="uploadPassword" type="password" id="uploadpasswordfield" class="form-control"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <div class="col-sm-12">
                                <div class="help-block">
                                    <p>You can reference this Password value using the storage path:</p>

                                    <p><strong data-bind="text: inputFullpath()"
                                               class="text-info"></strong></p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-sm btn-default"
                                data-dismiss="modal">Cancel</button>

                        <input type="submit"
                                class="btn btn-sm btn-success obs-storageupload-select"
                                name="Save"/>
                    </div>
                </div>
            </div>
        </div>
        </g:form>

    </div>
</div>
</body>
