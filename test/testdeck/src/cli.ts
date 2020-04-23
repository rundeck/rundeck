import yargs from 'yargs'

yargs.scriptName('deck').commandDir('commands', {extensions: ['ts']}).demandCommand().help().argv