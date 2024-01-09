export default {
    en_US: {
        edit: {
            readme: {
                label: 'Edit Readme',
            },
            motd:{
                label: 'Edit Message of the Day',
            },
            file: {
                project: 'Edit {0} for {1}'
            }
        },
        file: {
            readme: {
                help: {
                    markdown: `Edit the Readme for the project. This will be displayed on the home page. `,
                    html: `You can use
                    <a href="http://en.wikipedia.org/wiki/Markdown" target="_blank">
                    Markdown
                    </a>.\ `
                }
            },
            warning:{
                not: {
                    displayed: {
                        admin: {
                            message: 'Warning: This file will not be shown anywhere, you can enable it in the:'
                        },
                        nonadmin:{
                            message: 'Warning: This file will not be shown anywhere, you should ask an admin to update your Project Configuration to enable it.'
                        }
                    }
                }
            }
        },
        project: {
            configuration: {
                label: 'Project Configuration',
            }
        }
    }
};