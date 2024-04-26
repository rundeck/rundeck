import { Page } from '../page';
export declare enum Elems {
    drpProjectSelect = "//*[@id=\"projectSelect\"]",
    projectsCountCss = "#layoutBody span.h3.text-primary > span"
}
export declare class ProjectListPage extends Page {
    path: string;
    getProjectsCount(): Promise<any>;
}
