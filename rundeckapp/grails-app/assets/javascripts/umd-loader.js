function loadUmdModule(moduleName,pluginUrl,cssUrl) {
  return new Promise((resolve, reject) => {
    let script = document.createElement("script")
    script.onload = () => {
      resolve(moduleName)
    }
    script.onerror = () => {
      reject("module " + moduleName+" failed to load")
    }
    script.async = true
    script.src = pluginUrl
    document.head.appendChild(script)
    if(cssUrl) {
      let css = document.createElement("link")
      css.rel = "stylesheet"
      css.href = cssUrl
      document.head.appendChild(css)
    }

  })
}
var umdModulesToLoad = [] //this is set by the umd modules tag lib
var loadedUmdModules = []
function allUmdModulesLoaded() {
  jQuery(document).trigger(jQuery.Event('umd.loaded'))
}
function reportModuleLoaded(module) {
  loadedUmdModules.push(module)
  if(umdModulesToLoad.length === loadedUmdModules.length) {
    if(umdModulesToLoad.every(mod => loadedUmdModules.indexOf(mod) !== -1)) {
      allUmdModulesLoaded()
    }
  }
}
//This is used in conjunction with the UmdModuleTagLib to auto load all umd modules defined by the UmdModule interface