import {getRundeckContext} from "../../../library";

const rundeckClient = getRundeckContext().rundeckClient
export async function saveProjectFile(project: string, filename: string, fileText: string) {
    const resp = await rundeckClient.sendRequest({
        baseUrl: `${getRundeckContext().rdBase}api/${getRundeckContext().apiVersion}`,
        pathTemplate: "/project/" + project + "/" + filename,
        method: "PUT",
        body: {
            contents: fileText
        }
    });

    if (resp.status === 200) {
        return {success: true, message: "File saved successfully."};
    } else {
        throw {success: false, message: resp.parsedBody.message};
    }
}
export async function getFileText(project: string, filename: string) {
        const response = await rundeckClient.sendRequest({
            baseUrl: `${getRundeckContext().rdBase}api/${getRundeckContext().apiVersion}`,
            pathTemplate: "/project/" + project + "/" + filename,
            headers: {
                'Accept': 'application/json'
            },
            method: 'GET'
        });

        if (response.status === 200) {
            return {success: true, contents: response.parsedBody.contents};
        } else if (response.status === 404) {
            throw {success: false, warning: true}
        } else {
            throw {success: false, message: response.parsedBody.message}
        }
}