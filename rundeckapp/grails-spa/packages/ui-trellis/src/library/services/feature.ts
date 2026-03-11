import { api } from "./api";

export async function getFeatureEnabled(featureName: string): Promise<boolean> {
  try {
    const resp = await api.get(`/feature/${featureName}`);
    return !!resp?.data.enabled;
  } catch (error) {
    console.warn(`Failed to fetch feature flag: ${featureName}`, error);
    return false;
  }
}
