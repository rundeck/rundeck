import yargs from 'yargs'

yargs.commandDir('../node_modules/@rundeck/testdeck/src/commands', {extensions: ['ts']}).demandCommand().help().argv