
export function loadJsonData(id : string) {
  var dataElement = document.getElementById(id);
  // unescape the content of the span
  if (!dataElement) {
    return null;
  }
  var jsonText = dataElement.textContent || dataElement.innerText;
  return jsonText && jsonText != '' ? JSON.parse(jsonText) : null;
}