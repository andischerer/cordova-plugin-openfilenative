// fork from https://github.com/markeeftb/FileOpener
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