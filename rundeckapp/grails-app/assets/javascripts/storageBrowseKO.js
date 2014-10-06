//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter


function StorageResource(browser, path, data) {
    var self = this;

}
function StorageDir(browser, path, data) {
    var self = this;
    self.load = function () {
        browser.loadDir(self.name());
    }
}
function StorageUpload(storage){
    var self = this;
    self.storage=storage;
    self.keyType=ko.observable('private');
    self.inputType=ko.observable('text');
    self.file=ko.observable('');
    self.textArea=ko.observable('');
    self.password=ko.observable('');
    self.fileName=ko.observable('');

    //computed
    self.fileInputName=ko.computed(function(){
        var file = self.file();
        if(file){
            return file.lastIndexOf('/')>=0 ? file.substring(file.lastIndexOf('/')+1)
                : file.lastIndexOf('\\')>=0 ? file.substring(file.lastIndexOf('\\')+1)
                : file;

        }else{
            return '';
        }
    });
    self.validInput = ko.computed(function(){
        var intype = self.inputType();
        var file = self.file();
        var textarea=self.textArea();
        var pass=self.password();
        if(intype=='text'){
            return (textarea || pass )? true:false;
        }else{
            return file?true:false;
        }
    });
    /**
     * Returns the full path for the inputPath (dir) and inputFilename
     * @type {*}
     */
    self.inputFullpath = ko.computed(function () {
        var name = self.fileName();
        var file = self.fileInputName();
        var path = self.storage.inputPath();
        return (path ? (path.lastIndexOf('/') == path.length - 1 ? path : path + '/') : '') + (name?name: file);
    });

    //subscriptions to clear values when one input type is selected
    self.inputType.subscribe(function(newvalue){
       if(newvalue=='text'){
           self.file('');
       } else{
           self.textArea('');
           self.password('');
       }
    });
    //subscriptions to clear values when one input type is selected
    self.keyType.subscribe(function(newvalue){
       if(newvalue=='password'){
           self.textArea('');
       } else{
           self.password('');
       }
    });
}
function StorageBrowser(baseUrl, rootPath, fileSelect) {
    var self = this;
    self.baseUrl = baseUrl;
    self.fileSelect=fileSelect;
    self.rootPath = ko.observable(rootPath);
    self.errorMsg = ko.observable();
    self.path = ko.observable('');
    self.inputPath = ko.observable('');
    self.selectedPath=ko.observable();
    self.selectedResource=ko.observable();
    self.selectedIsDownloadable=ko.observable(false);
    self.fileFilter=ko.observable();
    self.fieldTarget=ko.observable();
    self.resources = ko.observableArray([]);
    self.loading=ko.observable(false);
    self.invalid=ko.observable(false);
    self.browseMode=ko.observable('browse');
    self.allowUpload=ko.observable(false);
    self.allowSelection=ko.observable(true);
    self.allowNotFound=ko.observable(false);
    self.notFound=ko.observable(false);

    //computed properties
    self.files = ko.computed(function () {
        return ko.utils.arrayFilter(self.resources(), function (res) {
            return res.type() == 'file';
        }).sort(function (a, b) {
                return a.path() == b.path() ? 0 : (a.path() < b.path() ? -1 : 1)
            });
    });
    self.filteredFiles=ko.computed(function(){
        return ko.utils.arrayFilter(self.files(), function (res) {
            if (self.fileFilter()) {
                var filt=self.fileFilter().split("=");
                if(filt.length>1){
                    var key=filt[0];
                    var value=filt[1];
                    return res.meta ? res.meta[key]!=null && res.meta[key]()==value : true;
                }
            }
            return true;
        });
    });
    self.directories = ko.computed(function () {
        return ko.utils.arrayFilter(self.resources(), function (res) {
            return res.type() == 'directory';
        }).sort(function(a,b){
                return a.path() == b.path() ? 0 : (a.path() < b.path() ? -1 : 1)
            });
    });

    self.selectedPathUrl=ko.computed(function(){
       return _genUrl(appLinks.storageKeysBrowse,{resourcePath:self.selectedPath()});
    });

    //functions

    self.dirNameString = function(dir){
        if (dir.lastIndexOf('/') >= 0) {
            return dir.substring(dir.lastIndexOf('/') + 1);
        } else {
            return dir;
        }
    }
    self.parentDirString = function (path) {
        if (null!=path && path.lastIndexOf('/') >= 0) {
            return path.substring(0,path.lastIndexOf('/'));
        } else {
            return '';
        }
    };
    self.dirName = function (elem) {
        if(typeof(elem)=='string'){
            return self.dirNameString(elem);
        }
        if (elem.type() == 'directory') {
            return self.dirNameString(elem.path())
        } else {
            return elem.name();
        }
    };
    self.upPath = ko.computed(function(){
        if(self.path()!=self.rootPath() && self.path() != self.rootPath()+'/'){
            if(self.path().indexOf('/')>=0){
                return self.path().substring(0,self.path().lastIndexOf('/'));
            }else{
                return self.rootPath();
            }
        }
        return null;
    });
    self.loadDir = function (dir) {
        var path = ((typeof(dir) == 'string') ? dir : dir.path());
        if (dir != self.path()) {
            self.selectedPath(null);
        }
        self.path(path);
    }
    self.initialLoad = function () {
        self.path(self.rootPath());
    }
    self.selectFile = function(res){
        if(self.allowSelection()){
            var candownload=false;
            if(self.selectedPath() == res.path()){
                self.selectedPath(null);
                self.selectedResource(null);
            }else{
                self.selectedPath(res.path());
                self.selectedResource(res);
                candownload = ! ( (res.meta['Rundeck-key-type'] && res.meta['Rundeck-key-type']()=='private')
                    || (res.meta['Rundeck-data-type'] && res.meta['Rundeck-data-type']() =='password') ) ;
            }
            self.selectedIsDownloadable(candownload);
        }
    }
    self.download = function(){
        if(self.selectedPath()){
            document.location = _genUrl(appLinks.storageKeysDownload, {resourcePath:self.selectedPath()});
        }
    };
    self.delete = function(){
        if(!self.selectedPath()){
            return;
        }
        jQuery.ajax({
            dataType: "json",
            method: 'post',
            url: _genUrl(appLinks.storageKeysDelete, {resourcePath: self.selectedPath() } ),
            beforeSend: _ajaxSendTokens.curry('storage_browser_token'),
            data: {},
            success: function (data, status, jqXHR) {
                self.selectedPath(null);
                self.loadPath(self.path());
            },
            error: function (jqXHR, textStatus, errorThrown) {
                self.loading(false);
                if (jqXHR.status == 404) {
                    self.pathNotFound(val);
                } else {
                    if(jqXHR.responseJSON && jqXHR.responseJSON.message){
                        self.errorMsg(jqXHR.responseJSON.message);
                    }else{
                        self.errorMsg(textStatus + ": " + errorThrown);
                    }
                }
            }
        }).success(_ajaxReceiveTokens.curry('storage_browser_token'));
    };
    self.browseToInputPath = function(){
        self.path(self.inputPath());
    };
    self.pathNotFound=function(path){
        self.notFound(true);
        if(!self.allowNotFound()){
            self.invalid(true);
            self.errorMsg("Path not found: " + path);
        }else{
            self.resources([]);
            var reload=false;
            self.selectedPath(null);
            self.inputPath(path)
            if(reload){
                self.browseToInputPath();
            }
        }
    };
    self.loadPath = function (val) {
//        if(val==''){
//            return;
//        }
        var mapping = {
            'resources': {
                key: function (data) {
                    return ko.utils.unwrapObservable(data.path);
                }
            }
        };
        self.loading(true);
        self.inputPath(val);
        jQuery.ajax({
            dataType: "json",
            url: self.baseUrl + val,
            data: {},
            success: function (data, status, jqXHR) {
                self.loading(false);
                if (data.type == 'file') {
                    //select the path and load the parent dir
                    self.selectedPath(val);
                    self.inputPath(self.parentDirString(val))
                    self.browseToInputPath();
                    return;
                }
                self.errorMsg(null);
                self.invalid(false);
                self.notFound(false);
                if (!data.resources) {
                    data.resources=[];
                }
                ko.mapping.fromJS(data, mapping, self);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                self.loading(false);
                if (jqXHR.status == 404) {
                    self.pathNotFound(val);
                } else {
                    self.errorMsg(textStatus + ": " + errorThrown);
                }
            }
        });
    };
    self.path.subscribe(function (val){
        self.loadPath(val);
    });

    self.browse=function(rootPath, filter, selectedPath){
        if(rootPath){
            self.rootPath(rootPath);
        }
        if (filter) {
            self.fileFilter(filter);
        }else{
            self.fileFilter(null);
        }
        if (selectedPath) {
            self.selectedPath(selectedPath);
            self.path(self.parentDirString(selectedPath));
        } else {
            self.initialLoad();
        }
    };

    //upload link

    self.upload = new StorageUpload(self);
}
