/**
 * Brace uses it's own require implementation, acequire, that seems to conflict
 * with webpack and ES6 imports. This swaps acequire for the working require in
 * an import hoisting friendly module.
 * 
 * This must occur before brace is imported or the export value of "undefined"
 * will be cached.
 */
// @ts-ignore
window.ace.acequire = window.ace.require

export default ''