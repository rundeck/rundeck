    <g:jsonToken id="storage_browser_token" url="${request.forwardURI}"/>
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
        <button type="button" class="btn btn-sm btn-default"
                data-bind="click: function(){$root.loadDir(upPath())}, css: {disabled: ( !upPath() || invalid() ) }" >
            <i class="glyphicon glyphicon-folder-open"></i>
            <i class="glyphicon glyphicon-arrow-up"></i>
            <span data-bind="text: upPath() ? $root.dirName(upPath()) : '' "></span>
        </button>

        <div class="btn-group" >
            <button type="button" class="btn btn-sm btn-default dropdown-toggle"
                    data-bind="css: { disabled: !selectedPath() }"
                    data-toggle="dropdown">
                Action <span class="caret"></span>
            </button>
            <ul class="dropdown-menu" role="menu">
                <li><a href="#" data-bind=" click: $root.delete"><i class="glyphicon glyphicon-remove"></i> Delete File</a></li>

                <li class="" data-bind=" if: selectedIsDownloadable()">
                    <a href="#" data-bind="click: download"><i class="glyphicon glyphicon-download"></i> Download File</a>
                </li>
            </ul>
        </div>

        <div class="btn-group" data-bind="if: allowUpload() ">
            <button type="button" class="btn btn-sm btn-success dropdown-toggle"
                    data-toggle="dropdown">
                <i class="glyphicon glyphicon-plus"></i> Add a File <span class="caret"></span>
            </button>
            <ul class="dropdown-menu" role="menu">
                <li>
                    <a href="#storageuploadkey" data-toggle="modal">Add a Public/Private Key</a>
                </li>
                <li>
                    <a href="#storageuploadpassword" data-toggle="modal">Add a Password</a>
                </li>
                <li class="divider"></li>
                <li>
                    <a data-toggle="modal" href="#storageupload">
                        <i class="glyphicon glyphicon-upload"></i> Upload
                    </a>
                </li>
            </ul>
        </div>
    </div>
    <div class="loading-area text-info " data-bind="visible: loading()"
         style="width: 100%; height: 200px; padding: 50px; background-color: #eee;">
        <i class="glyphicon glyphicon-time"></i> Loading...
    </div>
    <table class="table table-hover table-condensed" data-bind="if: !invalid() && !loading()">
        <tbody data-bind="if: !notFound()">
        <tr>
            <td colspan="2" class="text-muted">
                <span data-bind="if: filteredFiles().length < 1">
                    No files
                </span>
                <span data-bind="if: filteredFiles().length > 0">
                    <span data-bind="text: filteredFiles().length"></span> files
                </span>
            </td>
        </tr>
        </tbody>
        <tbody data-bind="foreach: filteredFiles()">
        <tr data-bind="click: $root.selectFile, css: $root.selectedPath()==path() ? 'success' : '' " class="action">
            <td >
                <i class="glyphicon "
                   data-bind="css: $root.selectedPath()==path() ? 'glyphicon-ok' : 'glyphicon-unchecked' "></i>
                <i class="glyphicon glyphicon-file"></i>
                <span data-bind="text: name"></span>
            </td>
            <td class="text-muted">
                <span class="pull-right">
                <span data-bind="if: $data.meta['Rundeck-key-type'] && $data.meta['Rundeck-key-type']()=='private'"
                    title="This path contains a private key that can be used for remote node execution."
                >
                    Private key
                    <i class="glyphicon glyphicon-lock"></i>
                </span>
                <span data-bind="if: $data.meta['Rundeck-key-type'] && $data.meta['Rundeck-key-type']()=='public'">
                    Public key
                    <i class="glyphicon glyphicon-eye-open"></i>
                </span>
                <span data-bind="if: $data.meta['Rundeck-data-type'] && $data.meta['Rundeck-data-type']()=='password'"
                    title="This path contains a password that can be used for remote node execution.">
                    Password
                    <i class="glyphicon glyphicon-lock"></i>
                </span>
                </span>
            </td>
        </tr>
        </tbody>

        <tbody data-bind="if: notFound()">
        <tr>
            <td colspan="2">
                <span class="text-muted">Nothing found at this path. Select "Add a File" if you would like to upload a new file.</span>
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
