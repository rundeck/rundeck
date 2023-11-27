import {getRundeckContext} from "../../../library";
import {Notification} from "uiv";

const rundeckClient = getRundeckContext().rundeckClient
function notifySuccess(title: string, message: string) {
    Notification.notify({
        type: "success",
        title: title,
        content: message,
        duration: 5000
    });
}

function notifyError(message: string) {
    Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: message,
        duration: 0
    });
}

function notifyWarning(message: string) {
    Notification.notify({
        type: "info",
        title: "Warning",
        content: message,
        duration: 0
    });
}
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
        notifySuccess("Success", "Saved Project File " + filename);
    } else {
        notifyError(resp.parsedBody.message);
    }
}
export async function getFileText(project: string, filename: string) {
    try {
        const response = await rundeckClient.sendRequest({
            baseUrl: `${getRundeckContext().rdBase}api/${getRundeckContext().apiVersion}`,
            pathTemplate: "/project/" + project + "/" + filename,
            headers: {
                'Accept': 'application/json'
            },
            method: 'GET'
        });

        if (response.status === 200) {
            return response.parsedBody.contents;
        } else if (response.status === 404) {
            notifyWarning("The file " + filename + " does not exist in project " + project + " yet. Please save to create it.");
        } else {
            notifyError(response.parsedBody.message);
        }
    } catch (error) {
        notifyError("An error occurred while fetching the file contents.");
        throw error;
    }
}