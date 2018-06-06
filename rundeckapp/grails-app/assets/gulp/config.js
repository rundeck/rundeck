var path = require('path');
var production = (process.env.NODE_ENV === 'production');

module.exports = {
	bower: 'bower_components',
	//dist: production ? 'dist' : '.tmp',
	dist: 'build',
	src: 'src',
	//livereloadPort: 35729,
	//port: 9000,
	//root: path.resolve('./')
	port: 9006
};
