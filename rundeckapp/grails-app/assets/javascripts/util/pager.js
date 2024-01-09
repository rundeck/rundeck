//= require vendor/knockout.min

/**
 *
 * @param data expected to included "content" (observable), "max", "offset"
 * @constructor
 */
function PagedView (data) {
    let self = this
    self.content = data.content
    self.max = ko.observable(data.max || 30)
    self.offset = ko.observable(data.offset || 0)
    self.pageCount = ko.pureComputed(function () {
        let count = self.content().length
        return Math.ceil(count / self.max())
    })
    self.hasPages = ko.pureComputed(function () {
        return self.pageCount() > 1
    })
    self.pageList = ko.pureComputed(function () {
        let pages = []
        let offset = self.offset()
        for (let i = 0; i < self.pageCount(); i++) {
            pages.push({
                page: (i + 1),
                index: i,
                current: (i * self.max()) === offset
            })
        }
        return pages
    })
    self.page = ko.pureComputed(function () {
        let paged = []
        let content = self.content()
        let count = content.length
        let start = self.offset()
        for (let i = start; i < count && i < start + self.max(); i++) {
            paged.push(content[i])
        }
        return paged
    })
    self.hasPrevLink = ko.pureComputed(function () {
        return self.offset()>0
    })
    self.hasNextLink = ko.pureComputed(function () {
        return self.offset()<((self.pageCount()-1)*self.max())
    })
    self.pageFirstIndex = ko.pureComputed(function () {
        return self.offset()+1
    })
    self.pageLastIndex = ko.pureComputed(function () {
        return Math.min(self.offset()+self.max(),self.content().length)
    })
    self.prevPage = function () {
        self.offset(self.offset() - self.max())
    }
    self.nextPage = function () {
        self.offset(self.offset() + self.max())
    }
    self.setPage = function (page) {
        self.offset(page * self.max())
    }
}
/**
 *
 * @param data expected to included "content" (observable), "filters" array of filter objects
 * @constructor
 */
function FilteredView(data){
    let self=this;
    self.content=data.content
    self.filters=ko.observableArray(data.filters||[])

    self.filteredContent=ko.pureComputed(function() {
        "use strict";
        let content = self.content();
        let filters = self.enabledFilters();
        if (filters.length < 1) {
            return content;
        }
        for(let i=0;i<filters.length;i++){
            content = filters[i].filter(content)
        }
        return content;
    });
    self.count=ko.pureComputed(function(){
        return self.filteredContent().length
    });
    self.enabledFilters=ko.pureComputed(function(){
        return ko.utils.arrayFilter(self.filters(), function (val) {
            return val.enabled()
        });
    });
    self.enabledFiltersCount=ko.pureComputed(function(){
        return self.enabledFilters().length;
    });

}

/**
 *
 * @param pager PagedView instance
 * @param name Name used to mount component to elements with [data-ko-pagination='${name}']
 * @constructor
 */
function PagerVueAdapter(pager, name) {
    let self = this;
    self.name = name;
    self.pager = pager;

    self.emitVuePagingEvent = function(pages) {
        window._rundeck.eventBus.emit('ko-pagination', {
            name: self.name,
            pager: self.pager
        })
    }

    pager.pageList.subscribe(self.emitVuePagingEvent)

    self.emitVuePagingEvent(self.pager.pageList())
}