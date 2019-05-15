/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//= require momentutil
//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter


function StorageResource() {
    var self = this;
    self.meta = ko.observable({});
    self.wasDownloaded=ko.observable(false);
    self.downloadError=ko.observable(null);
    self.metaValue=function(key){
        return self.meta()&& self.meta()[key]?self.meta()[key]():null;
    };
    self.wasModified=ko.computed(function(){
        if (self.meta() && self.meta()['Rundeck-content-creation-time'] && self.meta()['Rundeck-content-modify-time']()) {
            return self.meta()['Rundeck-content-creation-time']() != self.meta()['Rundeck-content-modify-time']();
        }
        return false;
    });
    self.isPrivateKey=ko.computed(function(){
        //$data.meta['Rundeck-key-type'] && $data.meta['Rundeck-key-type']()=='private'
        if (self.meta() && self.meta()['Rundeck-key-type'] && self.meta()['Rundeck-key-type']()=='private') {
            return true;
        }
        return false;
    });
    self.isPublicKey=ko.computed(function(){
        //$data.meta['Rundeck-key-type'] && $data.meta['Rundeck-key-type']()=='private'
        if (self.meta() && self.meta()['Rundeck-key-type'] && self.meta()['Rundeck-key-type']()=='public') {
            return true;
        }
        return false;
    });
    self.isPassword=ko.computed(function(){
        if (self.meta() && self.meta()['Rundeck-data-type'] && self.meta()['Rundeck-data-type']()=='password') {
            return true;
        }
        return false;
    });
    self.contentSize=ko.computed(function(){
        var value='';
        if(self.meta() && self.meta()['Rundeck-content-size'] && self.meta()['Rundeck-content-size']()){
            return self.meta()['Rundeck-content-size']();
        }
        return value;
    });
    self.createdUsername=ko.computed(function(){
        var value='';
        if(self.meta() && self.meta()['Rundeck-auth-created-username'] && self.meta()['Rundeck-auth-created-username']()){
            return self.meta()['Rundeck-auth-created-username']();
        }
        return value;
    });
    self.createdTime=ko.computed(function(){
        var value='';
        if(self.meta() && self.meta()['Rundeck-content-creation-time'] && self.meta()['Rundeck-content-creation-time']()){
            value = MomentUtil.formatTimeAtDate(self.meta()['Rundeck-content-creation-time']());
        }
        return value;
    });
    self.modifiedTime=ko.computed(function(){
        var value='';
        if(self.meta() && self.meta()['Rundeck-content-modify-time'] && self.meta()['Rundeck-content-modify-time']()){
            value = MomentUtil.formatTimeAtDate(self.meta()['Rundeck-content-modify-time']());
        }
        return value;
    });
    self.modifiedTimeAgoText = ko.computed(function () {
        var value = '';
        if (self.meta() && self.meta()['Rundeck-content-modify-time'] && self.meta()['Rundeck-content-modify-time']()) {
            var time = self.meta()['Rundeck-content-modify-time']();
            value = MomentUtil.formatDurationHumanize(MomentUtil.duration(time));
        }
        return value;
    });
    self.modifiedUsername = ko.computed(function () {
        var value = '';
        if (self.meta() && self.meta()['Rundeck-auth-modified-username'] && self.meta()['Rundeck-auth-modified-username']()) {
            return self.meta()['Rundeck-auth-modified-username']();
        }
        return value;
    });
    self.modifiedTimeAgo = function (label) {
        var value = self.modifiedTimeAgoText();
        if(value){
            return value +" "+label;
        }
        return "";
    };
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
    //if true, modifying existing path, otherwise, false
    self.modifyMode=ko.observable(false);
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
        var path = self.storage.absolutePath(self.storage.inputPath());
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
           self.inputType('text');
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
    self.staticRoot = ko.observable(false);
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
                    return res.metaValue(key) == value;
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
        return _genUrl(appLinks.storageKeysBrowse + '/' + self.selectedPath());
    });

    //functions

    self.cleanPath = function (path) {
        if(path != null){
            while(path.indexOf('/')==0){
                path = path.substring(1);
            }
        }else{
            return '';
        }
        return path;
    };
    self.relativePath = function (path) {
        var root = self.rootPath();
        var statroot = self.staticRoot();
        if(!statroot){
            return path;
        }
        var newpath='';
        if(path && root){
            path = self.cleanPath(path);
            newpath = self.cleanPath(path.substring(root.length));
        }
        return newpath;
    };
    self.absolutePath=function(relpath){
        var root = self.rootPath();
        var statroot = self.staticRoot();
        if (!statroot) {
            return relpath;
        }
        return root+'/'+relpath;
    };
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
                candownload = ( res.metaValue('Rundeck-key-type') != 'private' && res.metaValue('Rundeck-data-type')!='password') ;
            }
            self.selectedIsDownloadable(candownload);
        }
    }
    self.actionUploadModify=function(){
        if(self.selectedResource()){
            self.upload.fileName(self.selectedResource().name());
            self.inputPath(self.relativePath(self.parentDirString(self.selectedResource().path())));
            self.upload.keyType(self.selectedResource().isPrivateKey()?'private': self.selectedResource().isPublicKey()?'public':'password');
            self.upload.modifyMode(true);
            // jQuery("#storageuploadkey").modal({backdrop:false});
            jQuery("#storageuploadkey").modal('show');
        }
    };
    self.actionUpload=function(){
        self.upload.modifyMode(false);
        self.upload.fileName('');
        // jQuery("#storageuploadkey").modal({backdrop: false});
        jQuery("#storageuploadkey").modal('show');
    };
    self.actionLoadContents=function(destid,btn){
        if(self.selectedResource() && self.selectedResource().isPublicKey()){
            jQuery(btn).button('loading');
            jQuery.ajax({
                url: _genUrl(appLinks.storageKeysDownload, {relativePath: self.relativePath(self.selectedPath())}),
                success:function(data,jqxhr){
                    jQuery(btn).button('reset');
                    var found = jQuery('#' + destid);
                    setText(found[0],data);
                    self.selectedResource().wasDownloaded(true);
                },
                error:function (jqXHR, textStatus, errorThrown) {
                    jQuery(btn).button('reset');
                    self.selectedResource().downloadError(errorThrown+" : "+jqXHR.responseText);
                }
            });
        }
    };
    self.download = function(){
        if(self.selectedPath()){
            document.location = _genUrl(appLinks.storageKeysDownload, {relativePath:self.relativePath(self.selectedPath())});
        }
    };
    self.delete = function(){
        if(!self.selectedPath()){
            return;
        }
        jQuery.ajax({
            dataType: "json",
            method: 'post',
            url: _genUrl(appLinks.storageKeysDelete, {relativePath: self.relativePath(self.selectedPath()) } ),
            beforeSend: _createAjaxSendTokensHandler('storage_browser_token'),
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
        }).success(_createAjaxReceiveTokensHandler('storage_browser_token'));
    };
    self.browseToInputPath = function(){
        self.path(self.absolutePath(self.inputPath()));
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
            self.inputPath(self.relativePath(path));
            if(reload){
                self.browseToInputPath();
            }
        }
    };
    self.loadPath = function (val) {
        var mapping = {
            'resources': {
                key: function (data) {
                    return ko.utils.unwrapObservable(data.path);
                },
                create: function (options) {
                    var res=new StorageResource();
                    return ko.mapping.fromJS(options.data,{},res);
                }
            }
        };
        self.loading(true);
        self.inputPath(self.relativePath(val));
        jQuery.ajax({
            dataType: "json",
            url: _genUrl(self.baseUrl ,{relativePath: self.relativePath(val) } ),
            data: {},
            success: function (data, status, jqXHR) {
                self.loading(false);
                if (data.type == 'file') {
                    //select the path and load the parent dir
                    self.selectedPath(val);
                    self.inputPath(self.relativePath(self.parentDirString(val)))
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
                if(self.selectedPath()){
                    //select correct resource
                    var selected=ko.utils.arrayFirst(self.resources(),function(a){
                       return a.path()==self.selectedPath();
                    });
                    if(selected){
                        self.selectedPath(null);//otherwise gets deselected
                        self.selectFile(selected);
                    }else{
                        self.selectedPath(null);
                    }
                }
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
    self.selectedResource.subscribe(function (oldval) {
        if(oldval){
            //mark previous selected resource as not downloaded when another one is selected
            oldval.wasDownloaded(false);
        }
    }, null, "beforeChange");

    self.browse=function(rootPath, filter, selectedPath){
        if(rootPath){
            self.rootPath(self.cleanPath(rootPath));
        }
        if (filter) {
            self.fileFilter(filter);
        }else{
            self.fileFilter(null);
        }
        if (selectedPath) {
            var selpath= self.cleanPath(selectedPath);
            self.selectedPath(selpath);
            self.path(self.parentDirString(selpath));
        } else {
            self.initialLoad();
        }
    };
    self.pathInRoot = ko.computed(function () {
        var root = self.rootPath();
        var statroot = self.staticRoot();
        var path = self.path();
        return self.relativePath(path);
    });
    self.selectedPathInRoot = ko.computed(function () {
        var root = self.rootPath();
        var statroot = self.staticRoot();
        var path = self.selectedPath();
        return self.relativePath(path);
    });

    //upload link

    self.upload = new StorageUpload(self);
}
