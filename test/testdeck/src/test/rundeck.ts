import { ParseBool } from "../util/parseBool";

export const envOpts = {
    TESTDECK_RUNDECK_URL: process.env.TESTDECK_RUNDECK_URL || 'http://127.0.0.1:4440',
    TESTDECK_RUNDECK_TOKEN: process.env.TESTDECK_RUNDECK_TOKEN,
    CI: ParseBool(process.env.CI),
    TESTDECK_HEADLESS: ParseBool(process.env.TESTDECK_HEADLESS) || ParseBool(process.env.CI),
    TESTDECK_S3_UPLOAD: ParseBool(process.env.TESTDECK_S3_UPLOAD),
    TESTDECK_S3_BASE: process.env.TESTDECK_S3_BASE,
    TESTDECK_VISUAL_REGRESSION: ParseBool(process.env.TESTDECK_VISUAL_REGRESSION),
    TESTDECK_CLUSTER_CONFIG: process.env.TESTDECK_CLUSTER_CONFIG,
    TESTDECK_BASE_IMAGE: process.env.TESTDECK_BASE_IMAGE
}