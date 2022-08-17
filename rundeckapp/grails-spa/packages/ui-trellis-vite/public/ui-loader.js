console.log(' UI loader is loading scripts ...');

  var go = document.createElement('script');
  go.type = 'module';
  go.crossorigin = true;
  go.src = '/plugin/file/UI/ProViteUiComponentsUiPlugin/vite-pro-ui-components/index.js';
  document.getElementsByTagName('head')[0].appendChild(go);

  /*
    var po = document.createElement('script');
    po.type = 'module';
    po.crossorigin = true;
    po.src = '/plugin/file/UI/ProViteUiComponentsUiPlugin/vite-pro-ui-components/rundeckpro-runner.js';
    document.getElementsByTagName('head')[0].appendChild(po);
  */

  var jo = document.createElement('link');
  jo.rel = 'modulepreload';
  jo.href = '/plugin/file/UI/ProViteUiComponentsUiPlugin/vite-pro-ui-components/vendor.js';
  document.getElementsByTagName('head')[0].appendChild(jo);

  