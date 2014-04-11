// fork from https://github.com/markeeftb/FileOpener
cordova.define("org.apache.cordova.openfilenative.OpenFileNative", function(require, exports, module) {
    module.exports = {
        open: function (url) {
            var success = function () {
                console.log("success!");
            }, failure = function (error) {
                console.log(error);
            };
            cordova.exec(success, failure, "OpenFileNative", "openFileNative", [url]);
        }
    };
});
