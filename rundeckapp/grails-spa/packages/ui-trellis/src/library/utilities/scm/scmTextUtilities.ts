export class ScmTextUtilities {
    constructor(private readonly translateFunction: (key: string) => string) {
    }

    importDisplayText(importSynchState: string) {
        switch (importSynchState) {
            case "IMPORT_NEEDED":
                return this.translateFunction(
                    "scm.import.status.IMPORT_NEEDED.display.text"
                );
            case "REFRESH_NEEDED":
                return this.translateFunction(
                    "scm.import.status.REFRESH_NEEDED.display.text"
                );
            case "UNKNOWN":
                return this.translateFunction("scm.import.status.UNKNOWN.display.text");
            case "CLEAN":
                return this.translateFunction("scm.import.status.CLEAN.display.text");
            case "LOADING":
                return this.translateFunction("scm.import.status.LOADING.display.text");
        }
        return importSynchState;
    }

    exportDisplayText(exportSynchState: string) {
        switch (exportSynchState) {
            case "EXPORT_NEEDED":
                return this.translateFunction(
                    "scm.export.status.EXPORT_NEEDED.display.text"
                );
            case "CREATE_NEEDED":
                return this.translateFunction(
                    "scm.export.status.CREATE_NEEDED.display.text"
                );
            case "REFRESH_NEEDED":
                return this.translateFunction(
                    "scm.export.status.REFRESH_NEEDED.display.text"
                );
            case "DELETED":
                return this.translateFunction("scm.export.status.DELETED.display.text");
            case "CLEAN":
                return this.translateFunction("scm.export.status.CLEAN.display.text");
            case "LOADING":
                return this.translateFunction("scm.export.status.LOADING.display.text");
        }
        return exportSynchState;
    }

    jobScmDescription(exportSynchState: string, importSynchState: string) {
        let exportStatus = null;
        let importStatus = null;
        let text = null;
        if (exportSynchState) {
            exportStatus = exportSynchState;
            switch (exportStatus) {
                case "EXPORT_NEEDED":
                    text = this.translateFunction(
                        "scm.export.status.EXPORT_NEEDED.description"
                    );
                    break;
                case "CREATE_NEEDED":
                    text = this.translateFunction(
                        "scm.export.status.CREATE_NEEDED.description"
                    );
                    break;
                case "CLEAN":
                    text = this.translateFunction("scm.export.status.CLEAN.description");
                    break;
                case "LOADING":
                    text = this.translateFunction("scm.export.status.LOADING.description");
                    break;
                default:
                    text = exportStatus;
            }
        }
        if (importSynchState) {
            if (text) {
                text += ', ';
            } else {
                text = '';
            }
            importStatus = importSynchState;
            switch (importStatus) {
                case "IMPORT_NEEDED":
                    text += this.translateFunction('scm.import.status.IMPORT_NEEDED.description');
                    break;
                case "DELETE_NEEDED":
                    text += this.translateFunction('scm.import.status.DELETE_NEEDED.description');
                    break;
                case "CLEAN":
                    text += this.translateFunction('scm.import.status.CLEAN.description');
                    break;
                case "REFRESH_NEEDED":
                    text += this.translateFunction('scm.import.status.REFRESH_NEEDED.description');
                    break;
                case "UNKNOWN":
                    text += this.translateFunction('scm.import.status.UNKNOWN.description');
                    break;
                case "LOADING":
                    text = this.translateFunction('scm.import.status.LOADING.description');
                    break;
                default:
                    text += importStatus;
            }
        }
        return text;
    }

    jobScmStatusIconClass(jobSynchState: string) {
        switch (jobSynchState) {
            case "EXPORT_NEEDED":
                return "text-info";
            case "CREATE_NEEDED":
                return "text-success";
            case "UNKNOWN":
                return "text-primary";
            case "IMPORT_NEEDED":
            case "REFRESH_NEEDED":
            case "LOADING":
                return "text-warning";
            case "DELETED":
                return "text-danger";
            case "CLEAN":
                return "text-primary";
        }
        return "text-primary";
    }

    jobScmStatusIcon(jobSynchState: string) {
        switch (jobSynchState) {
            case "EXPORT_NEEDED":
            case "CREATE_NEEDED":
                return "glyphicon-exclamation-sign";
            case "UNKNOWN":
                return "glyphicon-question-sign";
            case "IMPORT_NEEDED":
            case "REFRESH_NEEDED":
                return "glyphicon-exclamation-sign";
            case "DELETED":
                return "glyphicon-minus-sign";
            case "CLEAN":
                return "glyphicon-ok";
            case "LOADING":
                return "glyphicon-refresh";
        }
        return "glyphicon-plus";
    }

}
