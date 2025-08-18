import type { StorybookConfig } from "@storybook/vue3-webpack5";
import remarkGfm from "remark-gfm";

const config: StorybookConfig = {
  stories: [
    "../src/library/components/primeVue/**/*.mdx",
    "../src/library/components/primeVue/**/*.stories.@(js|jsx|mjs|ts|tsx)",
  ],
  staticDirs: [],
  addons: [
    "@storybook/addon-webpack5-compiler-swc",
    "storybook-dark-mode",
    {
      name: "@storybook/addon-essentials",
      options: {
        backgrounds: false,
      },
    },
    {
      name: "@storybook/addon-styling-webpack",
      options: {
        rules: [
          {
            test: /\.css$/,
            sideEffects: true,
            use: [
              require.resolve("style-loader"),
              {
                loader: require.resolve("css-loader"),
                options: {},
              },
            ],
          },
          {
            test: /\.s[ac]ss$/,
            sideEffects: true,
            use: [
              require.resolve("style-loader"),
              {
                loader: require.resolve("css-loader"),
                options: {
                  importLoaders: 2,
                },
              },
              require.resolve("resolve-url-loader"),
              {
                loader: require.resolve("sass-loader"),
                options: {
                  // Want to add more Sass options? Read more here: https://webpack.js.org/loaders/sass-loader/#options
                  implementation: require("sass"),
                  sourceMap: true,
                  sassOptions: {},
                },
              },
            ],
          },
        ],
      },
    },
    "@storybook/addon-a11y",
    {
      name: "@storybook/addon-docs",
      options: {
        mdxPluginOptions: {
          mdxCompileOptions: {
            remarkPlugins: [remarkGfm],
          },
        },
      },
    },
  ],
  framework: {
    name: "@storybook/vue3-webpack5",
    options: {},
  },
  features: {
    backgroundsStoryGlobals: true,
  },
};
export default config;
