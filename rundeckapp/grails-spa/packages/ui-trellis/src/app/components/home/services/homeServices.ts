import axios from 'axios'
import {getRundeckContext} from "../../../../library";


export async function getSummary(): Promise<any> {
    let ctx = getRundeckContext();
    const response = await axios
        .request({
            method: "GET",
            headers: {
                "x-rundeck-ajax": "true",
            },
            url: `${ctx.rdBase}api/${ctx.apiVersion}/home/summary`
        })

    if (response.status >= 200 && response.status < 300) {
        return response.data;
    } else {
        throw {message: `Error: ${response.status}`, response: response};
    }
}