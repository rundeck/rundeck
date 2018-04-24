var fs = require('fs');
var tasks = fs.readdirSync('./gulp/tasks/')

//var onlyScripts = require('./util/scriptFilter');
// var tasks = fs.readdirSync('./gulp/tasks/').filter(onlyScripts);

tasks.forEach(function(task) {
	require('./tasks/' + task);
});
