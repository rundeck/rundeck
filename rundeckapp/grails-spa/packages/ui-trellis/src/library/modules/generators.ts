type UrlParams = string | Record<string, unknown>;

export const generateUrl = (url: string, params: UrlParams) => {
  return new Promise<string>((resolve) => {
    let urlparams = [];
    if (typeof params === "string") {
      urlparams = [params];
    } else if (typeof params === "object") {
      for (const e in params) {
        urlparams.push(
          `${encodeURIComponent(e)}=${encodeURIComponent(
            params[e] as string | number | boolean,
          )}`,
        );
      }
    }
    resolve(
      url +
        (urlparams.length
          ? (url.indexOf("?") > 0 ? "&" : "?") + urlparams.join("&")
          : ""),
    );
  });
};

export default {
  generateUrl,
};
