const Path = require("path");
const webpack = require("webpack");

const BUILD_COPYRIGHT = `© ${new Date().getFullYear()} PagerDuty, Inc. All Rights Reserved.`;

process.env.VUE_APP_BUILD_COPYRIGHT = BUILD_COPYRIGHT;

module.exports = {
  pages: {
    "components/central": { entry: "./src/app/components/central/main.ts" },
    "components/community-news-notification": {
      entry: "./src/app/components/community-news-notification/main.js",
    },
    "components/copybox": { entry: "./src/app/components/copybox/main.ts" },
    "components/uisockets": { entry: "./src/app/components/ui/main.ts" },
    "components/first-run": { entry: "./src/app/components/first-run/main.ts" },
    "components/ko-paginator": {
      entry: "./src/app/components/ko-paginator/main.ts",
    },
    "components/motd": { entry: "./src/app/components/motd/main.js" },
    "components/navbar": { entry: "./src/app/components/navbar/main.ts" },
    "components/project-picker": {
      entry: "./src/app/components/project-picker/main.ts",
    },
    "components/theme": { entry: "./src/app/components/theme/main.ts" },
    "components/tour": { entry: "./src/app/components/tour/main.js" },
    "components/version": { entry: "./src/app/components/version/main.js" },
    "components/server-identity": {
      entry: "./src/app/components/server-identity/serverIdentity.ts",
    },
    "components/readme-motd": {
      entry: "./src/app/components/readme-motd/main.ts",
    },
    "pages/storage": { entry: "./src/app/pages/storage/main.ts" },
    "pages/login": { entry: "./src/app/pages/login/main.ts" },
    "pages/project-dashboard": {
      entry: "./src/app/pages/project-dashboard/main.js",
    },
    "pages/project-activity": {
      entry: "./src/app/pages/project-activity/main.js",
    },
    "pages/repository": { entry: "./src/app/pages/repository/main.js" },
    "pages/command": { entry: "./src/app/pages/command/main.ts" },
    "pages/community-news": { entry: "./src/app/pages/community-news/main.js" },
    "pages/project-nodes-config": {
      entry: "./src/app/pages/project-nodes-config/main.js",
    },
    "pages/project-nodes-editor": {
      entry: "./src/app/pages/project-nodes-editor/main.ts",
    },
    "pages/project-config": { entry: "./src/app/pages/project-config/main.js" },
    "pages/execution-show": { entry: "./src/app/pages/execution-show/main.js" },
    "pages/webhooks": { entry: "./src/app/pages/webhooks/main.js" },
    "pages/user-summary": { entry: "./src/app/pages/menu/main.js" },
    "pages/dynamic-form": { entry: "./src/app/pages/dynamic-form/main.js" },
    "pages/job/editor": { entry: "./src/app/pages/job/editor/main.js" },
    "pages/nodes": { entry: "./src/app/pages/nodes/main.ts" },
    "pages/job/browse": { entry: "./src/app/pages/job/browse/main.ts" },
    "pages/home": { entry: "./src/app/pages/home/main.ts" },
    "pages/job/head/scm-action-buttons": {
      entry: "./src/app/pages/job/head/scm/scm-action-buttons.ts",
    },
    "pages/job/head/scm-status-badge": {
      entry: "./src/app/pages/job/head/scm/scm-status-badge.ts",
    },
  },
  publicPath: "/assets/static/",
  outputDir: process.env.VUE_APP_OUTPUT_DIR,
  filenameHashing: false,
  parallel: true,
  css: {
    sourceMap: true,
    /** Workaround for Vue CLI accounting for nested page paths
     * https://github.com/vuejs/vue-cli/issues/4378
     */
    extract:
      process.env.VUE_APP_CSS_EXTRACT === "true"
        ? {
            filename: "./css/[name].css",
            chunkFilename: "./css/[name].css",
          }
        : false,
  },
  chainWebpack: (config) => {
    /** Do not create index pages for entry points */
    config.entryPoints.store.forEach((_, entry) => {
      config.plugins.delete(`html-${entry}`);
      config.plugins.delete(`preload-${entry}`);
      config.plugins.delete(`prefetch-${entry}`);
    });

    /** Process source maps from deps */
    config.module
      .rule("source-map-loader")
      .test(/\.js$/)
      .include.add(/ui-trellis\/lib/)
      .end()
      .enforce("pre")
      .use("source-map-loader")
      .loader("source-map-loader")
      .end();

    /** adjust mini css extract loader to use a relative publicPath **/
    config.module
      .rule("css")
      .oneOf("vue-modules")
      .use("extract-css-loader")
      .set("options", {
        publicPath: "../../",
      })
      .end()
      .oneOf("vue")
      .use("extract-css-loader")
      .set("options", {
        publicPath: "../../",
      })
      .end()
      .oneOf("normal-modules")
      .use("extract-css-loader")
      .set("options", {
        publicPath: "../../",
      })
      .end()
      .oneOf("normal")
      .use("extract-css-loader")
      .set("options", {
        publicPath: "../../",
      });

    config.module
      .rule("scss")
      .oneOf("vue")
      .use("extract-css-loader")
      .set("options", {
        publicPath: "../../",
      });

    config.module
      .rule("scss")
      .oneOf("normal")
      .use("extract-css-loader")
      .set("options", {
        publicPath: "../../",
      });
  },
  configureWebpack: {
    devtool: process.env.VUE_APP_DEVTOOL,
    resolve: {
      alias: {
        "@": Path.resolve(__dirname, "./src"),
      },
    },
    output: {
      filename: (asset) => {
        if (asset.chunk.name.includes("chunk")) {
          return "js/[name].js";
        }
        return "[name].js";
      },
      library: "rundeckCore",
    },
    devServer: {
      hot: true,
      watchOptions: {
        followSymlinks: true,
      },
      proxy: {
        ".": {
          target: "http://localhost:4440",
        },
      },
    },
    externals: { vue: "Vue" },
    plugins: [
      /** Generate source maps for CSS as it does not support eval-source-map */
      new webpack.SourceMapDevToolPlugin({
        filename: "[file].map",
        include: [/\.css$/],
      }),
    ],
  },
  transpileDependencies: ["uiv"],
};
