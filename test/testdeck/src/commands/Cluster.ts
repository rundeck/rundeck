exports.command = 'cluster <command>'
exports.desc = 'Manage Rundeck clusters'
exports.builder = function(yargs) {
    yargs.commandDir('cluster_commands', {extensions: ['ts']}).demandCommand().scriptName('deck').help().argv
}
