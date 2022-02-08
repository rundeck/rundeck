function createVueComponentsFromUmdModule(moduleName) {
  // console.log("attempting to init vue components for module: " + moduleName)
  for (let [k, comp] of Object.entries(window[moduleName])) {
    //console.log('initing: ' + k); //uncomment for debug message
    Vue.component(k,comp)
  }
}
