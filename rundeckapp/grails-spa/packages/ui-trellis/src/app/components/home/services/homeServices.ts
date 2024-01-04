import axios from 'axios'
import {getRundeckContext} from "../../../../library";


export async function getSummary(): Promise<any> {
    let ctx = getRundeckContext();
    try {
        const response = await axios
            .get(`${ctx.rdBase}api/${ctx.apiVersion}/home/summary`, {
                method: "GET",
                headers: {
                    "x-rundeck-ajax": "true",
                },
                validateStatus(status) {
                    return status <= 403;
                }
            })

        if (response.status >= 200 && response.status < 300) {
            return response.data;
        } else {
            throw {message: `Error: ${response.status}`, response: response};
        }
    } catch (e: any) {
        // e.message in this case is the error message from the server response
        throw {message: "Error: " + e.message, response: e.response};
    }
}

export async function getProjects(): Promise<any> {
    let ctx = getRundeckContext();
    try {
        const response = await axios
            .get(`${ctx.rdBase}api/${ctx.apiVersion}/projects?meta=*`, {
                method: "GET",
                headers: {
                    "x-rundeck-ajax": "true",
                },
                validateStatus(status) {
                    return status <= 403;
                }
            })

        if (response.status >= 200 && response.status < 300) {
            return response.data;
        } else {
            throw {message: `Error: ${response.status}`, response: response};
        }
    } catch (e: any) {
        // e.message in this case is the error message from the server response
        throw {message: "Error: " + e.message, response: e.response};
    }
}