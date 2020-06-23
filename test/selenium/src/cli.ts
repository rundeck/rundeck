import yargs from 'yargs'

yargs
    .strict()
    .scriptName('deck')
    .commandDir('../node_modules/@rundeck/testdeck/src/commands', {extensions: ['ts']})
    .demandCommand()
    .help()
    .argv