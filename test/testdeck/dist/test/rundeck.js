"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.envOpts = void 0;
const parseBool_1 = require("../util/parseBool");
exports.envOpts = {
    TESTDECK_RUNDECK_URL: process.env.TESTDECK_RUNDECK_URL || 'http://127.0.0.1:4440',
    TESTDECK_RUNDECK_TOKEN: process.env.TESTDECK_RUNDECK_TOKEN,
    CI: (0, parseBool_1.ParseBool)(process.env.CI),
    TESTDECK_HEADLESS: (0, parseBool_1.ParseBool)(process.env.TESTDECK_HEADLESS) || (0, parseBool_1.ParseBool)(process.env.CI),
    TESTDECK_S3_UPLOAD: (0, parseBool_1.ParseBool)(process.env.TESTDECK_S3_UPLOAD),
    TESTDECK_S3_BASE: process.env.TESTDECK_S3_BASE,
    TESTDECK_VISUAL_REGRESSION: (0, parseBool_1.ParseBool)(process.env.TESTDECK_VISUAL_REGRESSION),
    TESTDECK_CLUSTER_CONFIG: process.env.TESTDECK_CLUSTER_CONFIG,
    TESTDECK_BASE_IMAGE: process.env.TESTDECK_BASE_IMAGE
};
//# sourceMappingURL=rundeck.js.map