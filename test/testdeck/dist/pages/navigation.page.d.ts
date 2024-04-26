import { Page } from '../page';
export declare enum Elems {
    drpProjectSelect = "//*[@id=\"projectSelect\"]",
    btnSideBarExpand = "/html/body/div/div[2]/div[1]/nav/div/div[1]/button",
    lnkDashboard = "//*[@id=\"nav-dashboard-link\"]",
    lnkJobs = "//*[@id=\"nav-jobs-link\"]",
    lnkNodes = "//*[@id=\"nav-nodes-link\"]",
    lnkCommands = "//*[@id=\"nav-commands-link\"]",
    lnkActivity = "//*[@id=\"nav-activity-link\"]",
    drpProjSettings = "//*[@id=\"projectAdmin\"]"
}
export declare class NavigationPage extends Page {
    path: string;
    toggleSidebarExpand(): Promise<void>;
    gotoProject(project: string): Promise<void>;
    visitDashBoard(): Promise<void>;
    visitJobs(): Promise<void>;
    visitNodes(): Promise<void>;
    visitCommands(): Promise<void>;
    visitActivity(): Promise<void>;
    visitSystemConfiguration(): Promise<void>;
}
