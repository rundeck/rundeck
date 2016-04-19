//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter
//= require ko/binding-url-path-param
//= require ko/binding-message-template
//= require jquery.waypoints.min
function ProjectAuth(data) {
    "use strict";
    var self = this;
    self.jobCreate = ko.observable(false);
    self.admin = ko.observable(false);
    self.mapping = {};
    if (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    }
}
function ProjectReadme(data) {
    "use strict";
    var self = this;
    self.readmeHTML = ko.observable(null);
    self.motdHTML = ko.observable(null);

    self.mapping = {};
    if (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    }
}
function Project(data) {
    var self = this;
    self.name = ko.observable(data.name);
    self.execCount = ko.observable(data.execCount || 0);
    self.userCount = ko.observable(data.userCount || 0);
    self.description = ko.observable(data.description);
    self.auth = ko.observable(new ProjectAuth());
    self.readme = ko.observable(new ProjectReadme());
    self.loaded = ko.observable(false);
    self.mapping = {
        auth: {
            create: function (options) {
                "use strict";
                return new ProjectAuth(options.data);
            }
        },
        readme: {
            create: function (options) {
                "use strict";
                return new ProjectReadme(options.data);
            }
        }
    };
    if (data) {
        ko.mapping.fromJS(data, self.mapping, self);
    }
}
function HomeData(data) {
    var self = this;
    self.baseUrl = data.baseUrl;
    self.summaryUrl = data.summaryUrl;
    self.projectNamesUrl = data.projectNamesUrl;
    self.loaded = ko.observable(data.loaded?true:false);
    self.jobCount = ko.observable(0);
    self.execCount = ko.observable(data.execCount||0);
    self.projectNames = ko.observableArray(data.projectNames || []);
    self.projectNamesTotal = ko.observable(data.projectNamesTotal || 0);
    self.loadedProjectNames = ko.observable(false);
    self.projects = ko.observableArray([]);
    self.projects.extend({rateLimit: 500});

    self.recentUsers = ko.observableArray(data.recentUsers||[]);
    self.recentProjects = ko.observableArray(data.recentProjects||[]);
    self.frameworkNodeName = ko.observable(null);
    self.doRefresh = ko.observable(false);
    self.doPaging = ko.observable(false);
    self.pagingMax = ko.observable(20);
    self.pagingRepeatMax = ko.observable(20);
    self.pagingDelay = ko.observable(2000);
    self.pagingOffset = ko.observable(0);
    self.refreshDelay = ko.observable(30000);
    self.search=ko.observable(null);

    //batch of project data to load
    self.batchList=ko.observableArray([]);
    self.batchList.extend({ rateLimit: 500 });
    self.batches=ko.observableArray([]);

    self.recentUsersCount = ko.pureComputed(function () {
        return self.recentUsers().length;
    });
    self.recentProjectsCount = ko.pureComputed(function () {
        return self.recentProjects().length;
    });
    self.searchedProjects=ko.pureComputed(function(){
        "use strict";
        var search=self.search();
        var names=self.projectNames();
        if(null==search){
            return names;
        }
        return ko.utils.arrayFilter(names,function(val,ndx){
            return val.search(search)>=0;
        });
    });
    self.searchedProjectsCount = ko.pureComputed(function () {
        return self.searchedProjects().length;
    });
    self.projectCount = ko.pureComputed(function () {
        return self.projectNames().length;
    });
    self.projectsByName = ko.pureComputed(function () {
        var obj = {};
        ko.utils.arrayForEach(self.projects(), function (elem, ndx) {
            obj[ko.unwrap(elem.name)] = elem;
        });
        return obj;
    });
    self.searchProjectsByName = ko.pureComputed(function () {
        var obj = {};
        ko.utils.arrayForEach(self.searchedProjects(), function (elem, ndx) {
            obj[ko.unwrap(elem.name)] = elem;
        });
        return obj;
    });
    self.projectInSearch=function(name){
        "use strict";
        return !self.search() || self.searchProjectsByName()[name]!=null;
    };
    self.prototypeProject=new Project({});
    self.projectForName = function (name) {
        var projectsByName = self.projectsByName()[name];
        if(null==projectsByName){
            return self.prototypeProject;
        }
        return projectsByName;
    };
    self.mapping = {
        'projects': {
            key: function (data) {
                return ko.utils.unwrapObservable(data.name);
            },
            create: function (data) {
                "use strict";
                return new Project(options.data);
            },
            update: function (options) {
                var updateArray = [];
                var found = options.parent.projectForName(options.data.name);
                if (found) {
                    return ko.mapping.fromJS(options.data, {}, found);
                } else {
                    return ko.mapping.fromJS(options.data, {}, {});
                }
            }
        }
    };
    self.loadSummary = function () {
        return jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers: {'x-rundeck-ajax': 'true'},
            url: _genUrl(self.summaryUrl, {}),
            success: function (data, status, jqxhr) {

                ko.mapping.fromJS(data, self.mapping, self);
                self.loaded(true);

                if (self.doRefresh()) {
                    setTimeout(self.loadSummary, self.refreshDelay());
                }
            }
        });
    };
    self.loadProjects = function (projs) {
        for (var i = 0; i < projs.length; i++) {
            var newproj = projs[i];
            var found = self.projectForName(newproj.name);
            if (found && found.name()==newproj.name) {
                ko.mapping.fromJS(newproj, null, found);
                found.loaded(true);
            } else {
                self.projects.push(
                    new Project(jQuery.extend(newproj, {loaded: true}))
                );
            }
        }
    };
    self.load = function (refresh) {
        var params = {};//refresh?{refresh:true}:{};
        if (self.doPaging()) {
            jQuery.extend(params, {
                offset: self.pagingOffset(),
                max: self.pagingMax()
            });
            self.pagingMax(self.pagingRepeatMax());
        }
        return jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers: {'x-rundeck-ajax': 'true'},
            url: _genUrl(self.baseUrl, params),
            success: function (data, status, jqxhr) {
                if (data.projects) {
                    self.loadProjects(data.projects);
                }
                // ko.mapping.fromJS(data, self.mapping, self);
                if (self.doPaging() && data.nextoffset) {
                    self.pagingOffset(data.nextoffset);
                    if (self.pagingOffset() === -1) {
                        self.pagingOffset(0);
                    } else {
                        setTimeout(self.load.curry(true), self.pagingDelay());
                        return;
                    }
                }
            }
        });
    };
    self.pullBatch=function(){
        "use strict";
        //pull up to max number of items from batchList
        var items=self.batchList.splice(0,self.pagingMax());
        if(items.length>0) {
            self.batches.push(items);
        }
    };
    self.batchList.subscribe(function(newValue){
        "use strict";
        if(newValue.length>0){
            setTimeout(self.pullBatch,100);
        }
    });
    self.batches.subscribe(function(newValue){
        if(!self.batchLoading && newValue.length>0){
            self.batchLoading=true;
            setTimeout(self.loadBatch,100);
        }
    });
    self.clearBatchLoad=function(){
        "use strict";

        self.batchList([]);
        self.batches([]);
    };
    self.search.subscribe(self.clearBatchLoad);
    self.batchLoading=false;
    self.loadBatch = function () {
        self.batchLoading=true;
        var projects=self.batches.shift();
        if(null==projects || projects.length<1){
            self.batchLoading=false;
            return;
        }
        var params = {projects:projects.join(',')};//refresh?{refresh:true}:{};
        return jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers: {'x-rundeck-ajax': 'true'},
            url: _genUrl(self.baseUrl, params),
            success: function (data, status, jqxhr) {
                if (data.projects) {
                    self.loadProjects(data.projects);
                }
                //...
                if(self.batches().length>0){
                    self.loadBatch();
                }else{
                    self.batchLoading=false;
                }
            }
        });
    };

    /**
     * bound via the ko/binding-waypoints knockout binding
     * example:
     * &lt;span data-bind="waypoints: $root.waypointHandler" &gt;
     */
    self.waypointHandler=function(){
        "use strict";
        var projectName = jQuery(this.element).data('project');
        if(!self.projectForName(projectName).loaded()) {
            self.batchList.push(projectName);
        }

        this.destroy();
    };
    //initial setup
    self.beginLoad = function () {

        if (self.doRefresh()) {
            setTimeout(self.loadSummary, self.refreshDelay());
        }
    };
    self.loadProjectNames = function () {
        return jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers: {'x-rundeck-ajax': 'true'},
            url: _genUrl(self.projectNamesUrl, {}),
            success: function (data, status, jqxhr) {
                self.projectNames(data.projectNames);
                self.projectNamesTotal(data.projectNames.length);
                self.loadedProjectNames(true);
            }
        });
    };
    self.init = function () {
        self.beginLoad();
        if (self.projectCount() != self.projectNamesTotal()) {
            self.loadProjectNames();
        }
    };
}
var _waypointBatchTimer;
function batchInitWaypoints(arr,handler,count){
    "use strict";
    var arr2=arr.splice(0,count);
    if(arr2.length>0) {
        jQuery(arr2).waypoint(handler, {offset: '100%'});
        if (arr.length > 0) {
            _waypointBatchTimer=setTimeout(batchInitWaypoints.curry(arr, handler,count), 1500);
        }
    }
}
function initWaypoints(homedata,reset){
    "use strict";
    if(reset){
        if(_waypointBatchTimer){
            clearTimeout(_waypointBatchTimer);
            _waypointBatchTimer=null;
        }
        Waypoint.destroyAll();
    }

    batchInitWaypoints(jQuery('.list-group-item.project_list_item[data-project]'),homedata.waypointHandler,50);
}

/**
 * START page init
 */
var homedata;
function init() {
    // ko.options.deferUpdates = true;
    var pageparams = loadJsonData('homeDataPagingParams') || {};
    var projectNamesData = loadJsonData('projectNamesData');
    if (projectNamesData.projectNames == null) {
        projectNamesData.projectNames = [];
    }
    var statsdata = loadJsonData('statsData') || {};
    homedata = new HomeData(jQuery.extend({
        baseUrl: appLinks.menuHomeAjax,
        summaryUrl: appLinks.menuHomeSummaryAjax,
        projectNamesUrl: appLinks.menuProjectNamesAjax,
        projectNames: projectNamesData.projectNames.sort(),
        projectNamesTotal: projectNamesData.projectNamesTotal || 0
    },statsdata));
    homedata.loadedProjectNames(projectNamesData.projectNames.length == projectNamesData.projectNamesTotal);
    ko.applyBindings(homedata);

    homedata.pagingMax(pageparams.pagingInitialMax || 15);
    homedata.pagingRepeatMax(pageparams.pagingRepeatMax || 50);
    homedata.doRefresh(pageparams.summaryRefresh != null ? pageparams.summaryRefresh : true);
    homedata.refreshDelay(pageparams.refreshDelay || 60000);
    homedata.doPaging(pageparams.doPaging != null ? pageparams.doPaging : true);
    homedata.pagingDelay(pageparams.pagingDelay || 2000);
    homedata.searchedProjects.subscribe(function(val){
        "use strict";
        //when search results change, refresh waypoints
        ko.tasks.schedule(initWaypoints.curry(homedata,true));
    });
    if(homedata.loadedProjectNames()){
        //load waypoints manually
        initWaypoints(homedata);
    }
    homedata.loaded.subscribe(Waypoint.refreshAll);
    homedata.projects.subscribe(Waypoint.refreshAll);
    homedata.init();
}
jQuery(init);
