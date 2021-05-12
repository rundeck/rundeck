//= require vendor/knockout.min

function Loadable (dataLoader,loaded) {
    let self = this
    self.loaded = ko.observable(loaded?true:false)
    self.loading = ko.observable(false)
    self.error = ko.observable(false)
    self.errorMessage = ko.observable()
    self.onError=function(err){
        self.loading(false)
        self.error(true)
        self.errorMessage(err)
    }
    self.onData = function (input) {
        try {
            dataLoader(input)
            self.loaded(true)
        } catch (e) {
            self.onError(e)
        }
        self.loading(false)
    }
    self.begin = function () {
        self.loading(true)
    }
    self.finished = function () {
        self.loaded(true)
        self.loading(false)
    }
}
