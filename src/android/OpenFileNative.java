// fork from https://github.com/markeeftb/FileOpener
package org.apache.cordova.openfilenative;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.webkit.URLUtil;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class OpenFileNative extends CordovaPlugin {

    private Context context;
    private String progressTitle;
    private CallbackContext callback;
    private final int permissionRequestCode = 865;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context  = cordova.getActivity();
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("openFileNative")) {
            callback = callbackContext;

            if (!PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                PermissionHelper.requestPermission(this, permissionRequestCode, Manifest.permission.READ_EXTERNAL_STORAGE);
                return true;
            }

            final JSONObject params = args.getJSONObject(0);
            if (!params.has("file") || params.has("file") && params.getString("file").length() == 0) {
                callback.error("File Parameter is missing");
                return true;
            }
            final String fileToOpen = params.getString("file");
            progressTitle = (params.has("progressTitle") && params.getString("progressTitle").length() > 0) ? params.getString("progressTitle") : "Open File";

            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        URI uri = new URI(fileToOpen);
                        if (uri.isAbsolute() && uri.getScheme().equalsIgnoreCase("market")) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fileToOpen)));
                            callback.success();
                        } else {
                            new DownloadAndOpenTask().execute(uri);
                        }
                    } catch (URISyntaxException e) {
                        callback.error("Error while opening file \"" + fileToOpen + "\".");
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private class DownloadAndOpenTask extends AsyncTask<URI, Integer, Boolean> {

        // declare the dialog as a member field of your activity
        private ProgressDialog mProgressDialog;
        private File targetFile;
        private boolean canceled = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // instantiate it within the onCreate method
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage(progressTitle);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    canceled = true;
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(URI... fileUris) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection httpConnection = null;
            URI uri = fileUris[0];
            String targetFileName;
            int fileLength = 0;

            int sep = uri.toString().lastIndexOf("/");
            if (sep > 0) {
                targetFileName = uri.toString().substring(sep + 1, uri.toString().length());
            } else {
                targetFileName = uri.toString();
            }

            try {
                if (!uri.isAbsolute()){
                    // local file in assets folder
                    AssetManager am = context.getAssets();
                    input = am.open("www/" + uri.toString());

                } else if (uri.getScheme().equalsIgnoreCase("file")) {
                    // local file in phone storage
                    URLConnection urlConnection = uri.toURL().openConnection();
                    fileLength = urlConnection.getContentLength();
                    input = urlConnection.getInputStream();

                } else {
                    // Remote file
                    URL url = uri.toURL();
                    httpConnection = (HttpURLConnection) url.openConnection();
                    httpConnection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        callback.error("Server returned HTTP " + httpConnection.getResponseCode()
                                + " " + httpConnection.getResponseMessage());
                        return false;
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    fileLength = httpConnection.getContentLength();
                    input = httpConnection.getInputStream();
                }

                if (input == null) {
                    callback.error("Could not open file '" + uri.toString() + "'");
                    return false;
                }

                // download the file and save it in externalcachedir
                // so other apps can acces the file
                targetFile = new File(context.getExternalCacheDir(), targetFileName);
                output = new FileOutputStream(targetFile);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (canceled) {
                        return false;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (FileNotFoundException e) {
                callback.error("File \"" + uri.toString() + "\" does not exists, could not be opened.");
                return false;
            } catch (Exception e) {
                callback.error(e.toString());
                return false;
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (httpConnection != null)
                    httpConnection.disconnect();
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (result) {
                openFile(targetFile.getAbsolutePath());
            }
        }

        private void openFile(String sUrl) {
            File file = new File(sUrl);
            Uri uri = Uri.fromFile(file);
            String mimeType = URLConnection.guessContentTypeFromName(sUrl);
            String guessedFileName = URLUtil.guessFileName(sUrl, null, null);

            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (guessedFileName.contains(".gif")) {
                // GIF file
                mimeType = "image/gif";
            } else if (guessedFileName.contains(".jpg") || guessedFileName.contains(".jpeg")) {
                // JPG file
                mimeType = "image/jpeg";
            } else if (guessedFileName.contains(".png")) {
                // PNG file
                mimeType = "image/png";
            } else if (guessedFileName.contains(".txt")) {
                // Text file
                mimeType = "text/plain";
            } else if (
                guessedFileName.contains(".mpg") ||
                guessedFileName.contains(".mpeg") ||
                guessedFileName.contains(".mpe") ||
                guessedFileName.contains(".mp4") ||
                guessedFileName.contains(".avi") ||
                guessedFileName.contains(".3gp") ||
                guessedFileName.contains(".3gpp")  ||
                guessedFileName.contains(".3g2")
            ) {
                // Video files
                mimeType = "video/*";
            } else if (guessedFileName.contains(".doc") || guessedFileName.contains(".docx")) {
                // Word document
                mimeType = "application/msword";
            } else if (guessedFileName.contains(".pdf")) {
                // PDF file
                mimeType = "application/pdf";
            } else if (guessedFileName.contains(".ppt") || guessedFileName.contains(".pptx")) {
                // Powerpoint file
                mimeType = "application/vnd.ms-powerpoint";
            } else if (guessedFileName.contains(".xls") || guessedFileName.contains(".xlsx")) {
                // Excel file
                mimeType = "application/vnd.ms-excel";
            } else if (guessedFileName.contains(".rtf")) {
                // RTF file
                mimeType = "application/rtf";
            } else if (guessedFileName.contains(".apk")) {
                mimeType = "application/vnd.android.package-archive";
            } else if (guessedFileName.contains(".eml")) {
                mimeType = "message/rfc822";
            }

            intent.setDataAndType(uri, mimeType);

            try {
                context.startActivity(intent);
                callback.success();
            } catch (Exception e) {
                callback.error("There is no corresponding application installed for opening \"" + mimeType + "\" files.");
            }

        }

    }

}