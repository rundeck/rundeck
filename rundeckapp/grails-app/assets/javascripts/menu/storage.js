//= require momentutil
//= require vendor/knockout.min
//= require vendor/knockout-mapping
//= require knockout-onenter
//= require storageBrowseKO

let storageBrowse

function init () {
    const data = loadJsonData('storageData')
    let rootPath = 'keys'
    storageBrowse = new StorageBrowser(appLinks.storageKeysApi, rootPath)
    storageBrowse.staticRoot(true)
    storageBrowse.browseMode('browse')
    storageBrowse.allowUpload(true)
    if (data.project) {
        storageBrowse.basePath('project/' + data.project)
    }
    if (!data.project) {
        //load project names
        jQuery.ajax({
            type: 'GET',
            contentType: 'json',
            headers: {'x-rundeck-ajax': 'true'},
            url: appLinks.menuProjectNamesAjax
        }).then(function (data) {
                storageBrowse.linksTitle('Projects')
                storageBrowse.jumpLinks(data.projectNames.map(function (v) {
                    return {name: v, path: "keys/project/" + v}
                }))
            }
        )

    }

    storageBrowse.allowNotFound(true)
    ko.applyBindings(storageBrowse)
    jQuery('#storageupload').find('.obs-storageupload-select').on('click', function (evt) {
        var file = jQuery('#storageuploadfile').val()
        jQuery('#uploadForm')[0].submit()
    })
    storageBrowse.browse(null, null, data.resourcePath ? data.resourcePath : null)
}

jQuery(init)
