import { NativeModules } from 'react-native';

const GJC = NativeModules.GJCamera;

class GJ {
  OpenMyCamera = async() => {
    const result = await GJC.openCamera();
    return result;
  }

  OpenMyCameraWithParams = async(params) => {
    const stringParams = JSON.stringify(params);

    console.log('params = ' + stringParams);

    const result = await GJC.openCameraWithParams(params);
    return result;
  }

  getFpsRanges = async() => {
    const result = await GJC.getFpsRanges();
    return result;
  }

  getExposureRanges = async() => {
    const result = await GJC.getExposureRanges();
    return result;
  }

  getIsoRanges = async() => {
    const result = await GJC.getIsoRanges();
    return result;
  }

  getAvailableResolutions = async() => {
    const result = await GJC.getAvailableResolutions();
    return result;
  }

  isManualFocusSupported = async() => {
    const result = await GJC.isManualFocusSupported();
    return result;
  }

  getMinimumFocusDistance = async() => {
    const result = await GJC.getMinimumFocusDistance();
    return result;
  }
}

export default new GJ()