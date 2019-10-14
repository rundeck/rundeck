  <%@ page contentType="text/html;charset=UTF-8" %>
    <html xmlns="http://www.w3.org/1999/html">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="base"/>
        <meta name="tabpage" content="configure"/>
        <meta name="tabtitle" content="${g.message(code: 'gui.menu.FileStorage')}"/>
        <title><g:message code="gui.menu.FileStorage"/></title>
        <asset:javascript src="storageBrowseKO.js"/>
        <g:javascript>
          var storageBrowse; function init() { var rootPath = 'files'; storageBrowse = new StorageBrowser(appLinks.storageFilesApi, rootPath, undefined, appLinks.storageFilesBrowse, appLinks.storageFilesDownload, appLinks.storageFilesDelete); storageBrowse.staticRoot(true); storageBrowse.browseMode('browse'); storageBrowse.allowUpload(true);
          storageBrowse.allowNotFound(true); ko.applyBindings(storageBrowse); jQuery('#storageupload').find('.obs-storageupload-select').on('click', function (evt) { var file = jQuery('#storageuploadfile').val(); console.log("upload: " + file);
          jQuery('#uploadForm')[0].submit(); }); var data = loadJsonData('storageData'); storageBrowse.browse(null,null,data.resourcePath?data.resourcePath:null); } jQuery(init);
        </g:javascript>
      </head>

      <body>
        <g:embedJSON id="storageData" data="[resourcePath:params.resourcePath]"/>
        <div id="page_storage" class="container-fluid">
          <div class="row">
            <div class="col-sm-12">
              <g:render template="/common/messages"/>
            </div>
          </div>
          <div class="row">

              <div class="col-xs-12">
                <div class="card">
                  <div class="card-content">
                    <g:render template="/framework/storageBrowser" model="[isFileStorage: true]"/>
                  </div>
                  <div class="card-footer">
                    <hr>
%{--                    <span class=" text-info">--}%
%{--                      <g:message code="page.keyStorage.description"/>--}%
%{--                    </span>--}%
                  </div>
                </div>
              </div>
          </div>
        </div>
        %{--modal file delete confirmation--}%
        <div class="modal" id="storageconfirmdelete" tabindex="-1" role="dialog" aria-labelledby="storageconfirmdeletetitle" aria-hidden="true">
          <div class="modal-dialog">
            <div class="modal-content">
              <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                <h4 class="modal-title" id="storageconfirmdeletetitle"><g:message code="storage.delete.selected.file"/></h4>
              </div>

              <div class="modal-body">
                <p><g:message code="storage.delete.selected.file.confirm"/></p>

                <p>
                  <strong data-bind="text: selectedPath()" class="text-info"></strong>
                </p>
              </div>

              <div class="modal-footer">
                <button type="button" class="btn btn-sm btn-default" data-dismiss="modal"><g:message code="cancel"/></button>

                <button data-bind=" click: $root.delete" data-dismiss="modal" class="btn btn-sm btn-danger obs-storagedelete-select"><g:message code="delete"/></button>
              </div>
            </div>
          </div>
        </div>
      </body>
