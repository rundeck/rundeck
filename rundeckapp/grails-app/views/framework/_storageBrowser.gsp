
    <div class="alert alert-warning" data-bind="visible: errorMsg()">
        <span data-bind="text: errorMsg"></span>
    </div>
    <div class="row text-info form-inline">
        <div class="form-group col-sm-12" data-bind="css: invalid()?'has-error':'' ">
            <input type="text" class="form-control"
                   data-bind="value: inputPath, valueUpdate: 'input', attr: {disabled: loading() }, executeOnEnter: browseToInputPath"
                   placeholder="Enter a path"/>
        </div>
    </div>
    <div class="row row-space">
        <div class="col-sm-12">
    <div >
        <span data-bind="if: upPath() && !invalid()">
        <button type="button" class="btn btn-sm btn-default" data-bind="click: function(){$root.loadDir(upPath())}" >
            <i class="glyphicon glyphicon-folder-open"></i>
            <i class="glyphicon glyphicon-arrow-up"></i>
            <span data-bind="text: $root.dirName(upPath())"></span>
        </button>
        </span>
        <span data-bind="if: allowUpload() && !notFound()" class="pull-right">
            <button class="btn btn-sm btn-default"
                    data-toggle="modal"
                    data-target="#storageupload">Upload <i class="glyphicon glyphicon-upload"></i>
            </button>
        </span>
    </div>
    <div class="loading-area text-info " data-bind="visible: loading()"
         style="width: 100%; height: 200px; padding: 50px; background-color: #eee;">
        <i class="glyphicon glyphicon-time"></i> Loading...
    </div>
    <table class="table table-hover table-condensed" data-bind="if: !invalid() && !loading()">
        <tbody data-bind="foreach: filteredFiles()">
        <tr data-bind="click: $root.selectFile, css: $root.selectedPath()==path() ? 'success' : '' ">
            <td >
                <i class="glyphicon "
                   data-bind="css: $root.selectedPath()==path() ? 'glyphicon-ok' : 'glyphicon-unchecked' "></i>
                <i class="glyphicon glyphicon-file"></i>
                <span data-bind="text: name"></span>
            </td>
            <td>
                <span data-bind="if: $data.meta['Rundeck-key-type']">
                    <i class="glyphicon"
                       data-bind="css: $data.meta['Rundeck-key-type']()=='private'?'glyphicon-ban-circle':'glyphicon-ok'"></i>
                    <span data-bind="text: $data.meta['Rundeck-key-type']"></span> key
                </span>
            </td>
        </tr>
        </tbody>
        <tbody data-bind="if: filteredFiles().length<1 && !notFound()">
        <tr>
            <td colspan="2" class="text-muted">
                No files
            </td>
        </tr>
        </tbody>
        <tbody data-bind="if: notFound()">
        <tr>
            <td colspan="2">
                <span class="text-muted">Nothing found at this path. Would you like to upload a new file?</span>
                <div
                        data-bind="if: allowUpload()" >
                    <button class="btn btn-sm btn-default"
                            data-toggle="modal"
                            data-target="#storageupload">Upload <i
                            class="glyphicon glyphicon-upload"></i>
                    </button>
                </div>
            </td>
        </tr>
        </tbody>
        <tbody data-bind="foreach: directories()">
        <tr>
            <td class="action" data-bind="click: $root.loadDir" colspan="2">
                <i class="glyphicon glyphicon-arrow-down"></i>
                <i class="glyphicon glyphicon-folder-close"></i>
                <span data-bind="text: $root.dirName($data)"></span>
            </td>
        </tr>
        </tbody>
    </table>
        </div>
    </div>
