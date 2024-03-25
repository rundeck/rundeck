import type { StorybookConfig } from "@storybook/vue3-webpack5";

const config: StorybookConfig = {
  stories: [
    "../src/library/components/primeVue/**/*.stories.@(js|jsx|mjs|ts|tsx)",
  ],
  addons: [
    "@storybook/addon-webpack5-compiler-swc",
    "@storybook/addon-links",
    "@storybook/addon-essentials",
    "@chromatic-com/storybook",
    "@storybook/addon-interactions",
    "storybook-dark-mode",
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
  ],
  framework: {
    name: "@storybook/vue3-webpack5",
    options: {},
  },
  docs: {
    autodocs: "tag",
  },
};
export default config;
