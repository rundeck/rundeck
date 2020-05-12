import yargs from 'yargs'

yargs.scriptName('deck').commandDir('../node_modules/@rundeck/testdeck/src/commands', {extensions: ['ts']}).demandCommand().help().argv