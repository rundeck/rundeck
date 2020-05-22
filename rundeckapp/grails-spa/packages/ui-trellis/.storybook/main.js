module.exports = {
    addons: [
        {
            name: '@storybook/preset-typescript',
            options: {
                framework: 'vue'
            }
        }
    ],
    stories: ['../src/**/*.stories.[tj]s']
}