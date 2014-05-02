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
function StorageBrowser(baseUrl, rootPath, fileSelect) {
    var self = this;
    self.baseUrl = baseUrl;
    self.fileSelect=fileSelect;
    self.rootPath = ko.observable(rootPath);
    self.errorMsg = ko.observable();
    self.path = ko.observable('');
    self.inputPath = ko.observable('');
    self.selectedPath=ko.observable();
    self.fileFilter=ko.observable();
    self.fieldTarget=ko.observable();
    self.resources = ko.observableArray([]);
    self.loading=ko.observable(false);
    self.invalid=ko.observable(false);
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
        if(self.selectedPath()==res.path()){
            self.selectedPath(null);
        }else{
            self.selectedPath(res.path());
        }
    }
    self.browseToInputPath = function(){
        self.path(self.inputPath());
    }
    self.path.subscribe(function (val) {
        if(val==''){
            return;
        }
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
                ko.mapping.fromJS(data, mapping, self);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                self.loading(false);
                if(jqXHR.status==404){
                    self.invalid(true);

                    self.errorMsg("Path not found: "+val);
                }else{
                    self.errorMsg(textStatus + ": " + errorThrown);
                }
            }
        });
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
}
