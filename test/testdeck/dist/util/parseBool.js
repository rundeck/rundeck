"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ParseBool = void 0;
const falsy = /^(?:f(?:alse)?|no?|0+)$/i;
function ParseBool(val) {
    return !falsy.test(val) && !!val;
}
exports.ParseBool = ParseBool;
;
//# sourceMappingURL=parseBool.js.map