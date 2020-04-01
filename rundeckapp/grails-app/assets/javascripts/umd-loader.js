//This is used in conjunction with the UmdModuleTagLib to auto load all umd modules defined by the UmdModule interface

function createVueComponentsFromUmdModule(moduleName) {
  for (let [k, comp] of Object.entries(window[moduleName])) { console.log('initing: ' + k); Vue.component(k,comp) }
}

function initUmdModule(moduleName, initMethodName) {
  try {
    window[moduleName][initMethodName]()
  } catch(err) {
    console.log("Failed to initialized umd module: "+moduleName)
    console.log(err)
  }
}