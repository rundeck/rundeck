module.exports = {
  presets: [
    ['@vue/cli-plugin-babel/preset', { "polyfills": ["es7.object.entries", "es6.promise"], "useBuiltIns": "entry" }]
  ],
  plugins: [
    ["import", { "libraryName": "ant-design-vue", "libraryDirectory": "es", "style": false }]
  ]
}
