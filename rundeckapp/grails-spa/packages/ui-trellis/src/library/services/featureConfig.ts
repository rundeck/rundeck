import { api } from "./api";

export async function getFeatureEnabled(featureName: string) {
    const resp =  await api.get(`/feature/${featureName}`);
    return !!resp?.data.enabled;
}