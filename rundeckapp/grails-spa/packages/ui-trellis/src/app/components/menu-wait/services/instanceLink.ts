/**
 * Build a link to a project on another Rundeck instance.
 *
 * The instance value comes from request parameters, so only http(s) URLs are
 * accepted to prevent executable schemes such as `javascript:`.
 *
 * @param instance base URL of the other instance
 * @param iproject target project name on the other instance
 * @returns the href, or null when the instance is not a valid http(s) URL
 */
export function buildInstanceLink(
  instance: string | null | undefined,
  iproject: string | null | undefined,
): string | null {
  if (!instance) {
    return null;
  }
  let parsed: URL;
  try {
    parsed = new URL(instance);
  } catch (e) {
    return null;
  }
  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    return null;
  }
  return `${instance}/?project=${encodeURIComponent(iproject || "")}`;
}
