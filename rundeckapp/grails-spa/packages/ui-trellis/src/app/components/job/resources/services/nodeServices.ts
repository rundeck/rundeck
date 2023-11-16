import axios from 'axios'
import {_genUrl} from "../../../../utilities/genUrl";
import {getAppLinks, getRundeckContext} from "../../../../../library";


export async function getExecutionMode(): Promise<any> {
    let ctx = getRundeckContext();
    const response = await axios.request({
        method: "GET",
        headers: {
            "x-rundeck-ajax": "true",
        },
        url: `${ctx.rdBase}api/${ctx.apiVersion}/system/executions/status`,
    });
    if (response.status >= 200 && response.status < 300) {
        return response.data;
    } else {
        throw {message: `Error fetching execution mode: ${response.status}`, response: response};
    }
}
export async function getNodeSummary(): Promise<any> {
    const response = await axios.get(
        _genUrl(getAppLinks().frameworkNodeSummaryAjax, {})
    );
    if (response.status < 200 && response.status >= 300) {
        throw {message: "Error: " + response.status, response: response};
    }
    return response.data;
}
export async function getNodes(params: any, url: string): Promise<any> {
    const response = await axios
        .request({
            method: "GET",
            headers: {
                "x-rundeck-ajax": "true",
            },
            url: _genUrl(url, params),
        })

    if (response.status === 403) {
        throw {message: "Not authorized", response: response};
    } else if (response.status >= 300) {
        if (response.data.message) {
            throw {message: response.data.message, response: response};
        } else {
            throw {message: "Error: " + response.status, response: response};
        }
    }


    return response.data
}