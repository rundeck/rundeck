//= require knockout.min
//= require knockout-mapping
//= require knockout-onenter
//= require ko/binding-url-path-param
function Project(data) {
}
function HomeData(data) {
    var self = this;
    self.baseUrl = data.baseUrl;
    self.summaryUrl = data.summaryUrl;
    self.loaded = ko.observable(false);
    self.jobCount = ko.observable(0);
    self.execCount = ko.observable(0);
    self.projectNames = ko.observableArray(data.projectNames || []);
    self.projects = ko.observableArray([]);

    self.recentUsers = ko.observableArray([]);
    self.recentProjects = ko.observableArray([]);
    self.frameworkNodeName = ko.observable(null);
    self.doRefresh=ko.observable(false);
    self.doPaging=ko.observable(false);
    self.pagingMax=ko.observable(20);
    self.pagingRepeatMax=ko.observable(20);
    self.pagingDelay=ko.observable(2000);
    self.pagingOffset=ko.observable(0);
    self.refreshTime=ko.observable(30000);

    self.recentUsersCount = ko.pureComputed(function () {
        return self.recentUsers().length;
    });
    self.recentProjectsCount = ko.pureComputed(function () {
        return self.recentProjects().length;
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
    self.projectForName = function (name) {
        return self.projectsByName()[name];
    };
    self.mapping = {
        'projects': {
            key: function (data) {
                return ko.utils.unwrapObservable(data.name);
            },
            update: function (options) {
                var updateArray=[];
                var found = options.parent.projectForName(options.data.name);
                if(found){
                    return ko.mapping.fromJS(options.data,{},found);
                }else{
                    return ko.mapping.fromJS(options.data,{},{});
                }
            }
        }
    };
    self.loadSummary = function () {
        jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers:{'x-rundeck-ajax':'true'},
            url: _genUrl(self.summaryUrl,{}),
            success: function (data, status, jqxhr) {

                ko.mapping.fromJS(data, self.mapping, self);
                self.loaded(true);
                if(self.doRefresh()){
                    setTimeout(self.loadSummary,self.refreshTime());
                }
            }
        });
    };
    self.loadProjects = function (projs) {
        for(var i=0;i<projs.length;i++){
            var newproj = projs[i];
            var found = self.projectForName(newproj.name);
            if(found){
                ko.mapping.fromJS(newproj, null, found);
                found.loaded(true);
            }else{
                self.projects.push(
                    ko.mapping.fromJS(
                        jQuery.extend(newproj,{loaded:true})
                    )
                );
            }
        }
    };
    self.load = function (refresh) {
        var params={};//refresh?{refresh:true}:{};
        if(self.doPaging()){
            jQuery.extend(params,{
                offset:self.pagingOffset(),
                max:self.pagingMax()
            });
            self.pagingMax(self.pagingRepeatMax());
        }
        jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers:{'x-rundeck-ajax':'true'},
            url: _genUrl(self.baseUrl,params),
            success: function (data, status, jqxhr) {
                if(data.projects){
                    self.loadProjects(data.projects);
                }
                // ko.mapping.fromJS(data, self.mapping, self);
                if(self.doPaging()&&data.nextoffset){
                    self.pagingOffset(data.nextoffset);
                    if(self.pagingOffset()===-1){
                        self.pagingOffset(0);
                    }else{
                        setTimeout(self.load.curry(true),self.pagingDelay());
                        return;
                    }
                }
            }
        });
    };

    //initial setup
    self.setup=function(){
        for(var i=0;i<self.projectNames().length;i++){
            self.projects.push(
                ko.mapping.fromJS({
                    name:self.projectNames()[i],
                    execCount:0,
                    userCount:0,
                    description:null,
                    auth:{
                        jobCreate:false,
                        admin:false
                    },
                    readme:{
                        readmeHTML:null,
                        motdHTML:null
                    },
                    loaded:false
                },null,{})
            );
        }
    };
    self.setup();
}


/**
 * START page init
 */
var homedata;
function init() {
    var pageparams=loadJsonData('homeDataPagingParams')||{};
    homedata = new HomeData({
        baseUrl: appLinks.menuHomeAjax,
        summaryUrl: appLinks.menuHomeSummaryAjax,
        projectNames: loadJsonData('projectNamesList').sort()
    });
    ko.applyBindings(homedata);
    homedata.pagingMax(pageparams.pagingInitialMax||10);
    homedata.pagingRepeatMax(pageparams.pagingRepeatMax||50);
    homedata.doRefresh(pageparams.summaryRefresh!=null?pageparams.summaryRefresh:false);
    homedata.doPaging(pageparams.doPaging!=null?pageparams.doPaging:true);
    homedata.pagingDelay(pageparams.pagingDelay||2000);
    homedata.loadSummary();
    homedata.load();
}
jQuery(init);