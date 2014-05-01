
    <div class="alert alert-warning" data-bind="visible: errorMsg()">
        <span data-bind="text: errorMsg"></span>
    </div>
    <div class="text-info">
        Path: <input type="text" data-bind="value: path"/>
        <span data-bind="if: loading()" class="text-info">
            <g:img file="icon-tiny-disclosure-waiting.gif"/>
            Loading...
        </span>
    </div>
    <div data-bind="if: upPath()">
        <button type="button" class="btn btn-sm btn-default" data-bind="click: function(){$root.loadDir(upPath())}" >
            <i class="glyphicon glyphicon-folder-open"></i>
            <i class="glyphicon glyphicon-arrow-up"></i>
            <span data-bind="text: $root.dirName(upPath())"></span>
        </button>
    </div>
    <table class="table table-hover table-condensed">
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
        <tbody data-bind="if: filteredFiles().length<1">
        <tr>
            <td colspan="2" class="text-muted">
                No files
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
