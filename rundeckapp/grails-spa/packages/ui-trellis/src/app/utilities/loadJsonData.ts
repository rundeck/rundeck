export function loadJsonData(id: string) {
  const dataElement = document.getElementById(id);
  // unescape the content of the span
  if (!dataElement) {
    return null;
  }
  const jsonText = dataElement.textContent || dataElement.innerText;
  return jsonText && jsonText != "" ? JSON.parse(jsonText) : null;
}
