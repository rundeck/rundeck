

interface AuthzMeta {
    name: string;
    data: {
        project: {
            [key: string]: boolean;
        };
        types: {
            job: {
                [key: string]: boolean;
            }
        };
    };
}

interface ConfigMeta {
    name: string;
    data: {
        executionsEnabled: boolean;
        groupExpandLevel: number;
        scheduleEnabled: boolean;
    };
}

interface SysModeMeta {
    name: string;
    data: {
        active: boolean;
    };
}

interface SysModeMeta {
    name: string;
    data: {
        active: boolean;
    };
}

interface ScmExportMeta {
    name: string;
    data: object;
}

interface ScmImportMeta {
    name: string;
    data: object;
}

type MetaType = AuthzMeta | ConfigMeta | SysModeMeta | ScmExportMeta | ScmImportMeta;

interface Project {
    name: string;
    url: string;
    description: string;
    label?: string;
    config?: object;
    meta: MetaType[];
    extra?: any;
}

interface ProjectActionsItemDropdown {
    show: boolean;
    link: string;
    text: string;
}

export {Project, ConfigMeta, AuthzMeta, SysModeMeta, ScmExportMeta, ScmImportMeta, ProjectActionsItemDropdown}