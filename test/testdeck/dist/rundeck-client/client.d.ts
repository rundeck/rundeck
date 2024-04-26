import { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import * as Dto from './dto';
interface IClientOpts {
    apiUrl: string;
    username?: string;
    password?: string;
    token?: string;
}
export declare class Client {
    readonly opts: IClientOpts;
    c: AxiosInstance;
    private loginProm?;
    constructor(opts: IClientOpts);
    login(): Promise<void>;
    listUsers(): Promise<AxiosResponse<Dto.IUser[]>>;
    systemInfo(): Promise<AxiosResponse<Dto.ISystemInfoResponse>>;
    get<T>(url: string, config?: AxiosRequestConfig): Promise<any>;
    private _doLogin;
}
export {};
