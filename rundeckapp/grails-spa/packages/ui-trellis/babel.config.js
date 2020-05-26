module.exports = {
    presets: [
        ["@vue/cli-plugin-babel/preset", {
            targets: {node: "current"}
        }]
    ],
    plugins: [
        ["import", { "libraryName": "ant-design-vue", "libraryDirectory": "es", "style": "css" }]
    ]
};