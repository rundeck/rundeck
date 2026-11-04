import axios from "axios";
import _ from "lodash";
import Trellis from "../../../library";
import TourConstants from "./constants";
import type { Tour } from "./interfaces/Tours";

export interface TourManifestEntry {
  key: string;
  name: string;
  author: string;
  provider: string;
}

export interface TourLoader {
  tours: TourManifestEntry[];
  provider: string;
  loader: string;
}

export interface LoadedTour extends Tour {
  project?: string;
}

export const getTours = () => {
  const tours: TourLoader[] = [];
  return new Promise<TourLoader[]>((resolve, reject) => {
    axios
      .get(TourConstants.tourManifestUrl, getHeaderObject())
      .then((response) => {
        if (response && response.data && response.data.length) {
          _.each(response.data, (tourLoader) => {
            tours.push(tourLoader);
          });
          resolve(tours);
        }
      })
      .catch(function (error) {
        reject(new Error(error));
        console.log("Tour manifest not found");
      });
  });
};

export const getTour = (tourLoader: string, tourKey: string) => {
  return new Promise<LoadedTour>((resolve, reject) => {
    axios
      .get(
        `${TourConstants.tourUrl}${tourLoader}/${tourKey}.json`,
        getHeaderObject(),
      )
      .then((response) => {
        if (response && response.data) {
          resolve(response.data);
        }
      })
      .catch(function (error) {
        console.log(error);
        reject(error);
      });
  });
};

export const unsetTour = () => {
  return new Promise<void>((resolve) => {
    Trellis.FilterPrefs.unsetFilterPref("activeTour").then(() => {
      Trellis.FilterPrefs.unsetFilterPref("activeTourStep").then(() => {
        resolve();
      });
    });
  });
};

function getHeaderObject() {
  return window._rundeck.projectName
    ? { headers: { "X-Tour-Project": window._rundeck.projectName } }
    : {};
}

export default {
  getTours,
  getTour,
  unsetTour,
};
