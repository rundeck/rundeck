type UrlParams = string | Record<string, unknown>;

export function _genUrlQuery(params: UrlParams) {
  let urlparams = [];
  if (typeof params === "string") {
    urlparams = [params];
  } else if (typeof params === "object") {
    for (const e in params) {
      urlparams.push(
        encodeURIComponent(e) +
          "=" +
          encodeURIComponent(params[e] as string | number | boolean),
      );
    }
  }
  return urlparams.join("&");
}

/**
 * Generate a URL
 * @param url
 * @param params
 * @returns {string}
 * @private
 */
export function _genUrl(url: string, params: UrlParams) {
  const paramString = _genUrlQuery(params);
  return (
    url +
    (paramString.length ? (url.indexOf("?") > 0 ? "&" : "?") + paramString : "")
  );
}
