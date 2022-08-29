console.log(' Vite UI loader is loading scripts ...');

  var go = document.createElement('script');
  go.type = 'module';
  go.crossorigin = true;
  go.src = '/assets/static/ui-trellis-vite/index.js';
  document.getElementsByTagName('head')[0].appendChild(go);
