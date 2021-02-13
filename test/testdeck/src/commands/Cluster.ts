exports.command = 'cluster <command>'
exports.desc = 'Manage Rundeck clusters'
exports.builder = function(yargs: any) {
    yargs.commandDir('cluster_commands', {extensions: ['ts']}).demandCommand().scriptName('deck').help()
}
