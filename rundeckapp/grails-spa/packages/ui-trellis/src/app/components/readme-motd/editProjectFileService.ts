import { api } from "../../../library/services/api";

export async function saveProjectFile(
  project: string,
  filename: string,
  fileText: string,
) {
  const resp = await api.put(`project/${project}/${filename}`, {
    contents: fileText,
  });

  if (resp.status === 200) {
    return { success: true, message: "File saved successfully." };
  } else {
    throw { success: false, message: resp.data.message };
  }
}

export async function getFileText(project: string, filename: string) {
  try {
    const response = await api.get(`project/${project}/${filename}`);

    if (response.status === 200) {
      return { success: true, contents: response.data.contents };
    } else if (response.status === 404) {
      throw { success: false, warning: true };
    } else {
      throw { success: false, message: response.data.message };
    }
  } catch (error) {
    // If it's a 404, we need to maintain the warning flag
    if (error.response && error.response.status === 404) {
      throw { success: false, warning: true };
    }
    throw error;
  }
}
