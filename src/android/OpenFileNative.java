// fork from https://github.com/markeeftb/FileOpener
package org.apache.cordova.openfilenative;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class OpenFileNative extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        try {
            if (action.equals("openFileNative")) {
                new DownloadTask(cordova.getActivity()).execute(args.getString(0));
                callbackContext.success();
                return true;
            }
        } catch (RuntimeException e) {  // KLUDGE for Activity Not Found
            e.printStackTrace();
            callbackContext.error(e.getMessage());
        }
        return false;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        // declare the dialog as a member field of your activity
        private ProgressDialog mProgressDialog;
        private String downloadFileName;
        private File downloadFile;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // instantiate it within the onCreate method
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage("Öffne Datei");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                downloadFileName = URLUtil.guessFileName(sUrl[0], null, null);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                downloadFile = new File(context.getExternalCacheDir(), downloadFileName);
                output = new FileOutputStream(downloadFile);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled())
                        return null;
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            } else {
                Uri uri = Uri.fromFile(downloadFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                String mimeType = URLConnection.guessContentTypeFromName(downloadFileName);


                if (downloadFileName.contains(".gif")) {
                    // GIF file
                    mimeType = "image/gif";
                } else if (downloadFileName.contains(".jpg") || downloadFileName.contains(".jpeg")) {
                    // JPG file
                    mimeType = "image/jpeg";
                } else if (downloadFileName.contains(".txt")) {
                    // Text file
                    mimeType = "text/plain";
                } else if (downloadFileName.contains(".mpg") || downloadFileName.contains(".mpeg") || downloadFileName.contains(".mpe") || downloadFileName.contains(".mp4") || downloadFileName.contains(".avi")) {
                    // Video files
                    mimeType = "video/*";
                } else if (downloadFileName.contains(".doc") || downloadFileName.contains(".docx")) {
                    // Word document
                    mimeType = "application/msword";
                } else if (downloadFileName.contains(".pdf")) {
                    // PDF file
                    mimeType = "application/pdf";
                } else if (downloadFileName.contains(".ppt") || downloadFileName.contains(".pptx")) {
                    // Powerpoint file
                    mimeType = "application/vnd.ms-powerpoint";
                } else if (downloadFileName.contains(".xls") || downloadFileName.contains(".xlsx")) {
                    // Excel file
                    mimeType = "application/vnd.ms-excel";
                } else if (downloadFileName.contains(".rtf")) {
                    // RTF file
                    mimeType = "application/rtf";
                }

                intent.setDataAndType(uri, mimeType);

                try {
                    context.startActivity(intent); // TODO handle ActivityNotFoundException
                } catch (Exception e) {
                    Toast.makeText(context, "Es ist keine entsprechende Anwendung installiert um den Dateityp \"" + mimeType + "\" zu öffnen.", Toast.LENGTH_LONG).show();
                }

            }
        }

    }

}
