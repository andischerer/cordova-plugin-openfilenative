// fork from https://github.com/markeeftb/FileOpener
module.exports = {
  open: function (params, successCallback, errorCallback) {
    successCallback = successCallback || function() {};
    errorCallback = errorCallback || function() {};
    if (typeof params === 'string') {
      params = {
        file: params,
        progressTitle: 'Open File'
      };
    }
    cordova.exec(successCallback, errorCallback, 'OpenFileNative', 'openFileNative', [params]);
  }
};
