
package org.brandroid.openmanager.data;

import android.content.Context;
import android.os.AsyncTask;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.OpenPath.NeedsTempFile;
import org.brandroid.openmanager.data.OpenPath.OpenStream;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class OpenNetworkPath extends OpenPath implements NeedsTempFile, OpenStream {
    /**
	 * 
	 */
    private static final long serialVersionUID = -3829590216951441869L;
    protected UserInfo mUserInfo;
    protected byte[] mPubKey, mPrivKey, mPrivPwd;
    public static final JSch DefaultJSch = new JSch();
    public static int Timeout = 20000;
    protected String mName = null;
    protected int mPort = -1;
    private final boolean DEBUG = OpenExplorer.IS_DEBUG_BUILD && false;
    private OpenServer mServer;

    public interface NetworkListener {
        public static final NetworkListener DefaultListener = new NetworkListener() {

            @Override
            public void OnNetworkFailure(OpenNetworkPath np, OpenFile dest, Exception e) {
                Logger.LogWarning("Network Failure for " + np);
            }

            @Override
            public void OnNetworkCopyUpdate(Integer... progress) {

            }

            @Override
            public void OnNetworkCopyFinished(OpenNetworkPath np, OpenFile dest) {
                Logger.LogDebug("Network Copy Finished for " + np + " \u2661 " + dest);
            }
        };

        public void OnNetworkCopyFinished(OpenNetworkPath np, OpenFile dest);

        public void OnNetworkCopyUpdate(Integer... progress);

        public void OnNetworkFailure(OpenNetworkPath np, OpenFile dest, Exception e);
    }

    public interface OpenAuthCallback {
        public void OnAuthenticate(String url);

        public void OnAuthenticated(OpenPath path);
    }
    
    public final void setServer(OpenServer server)
    {
        mServer = server;
    }
    
    public final OpenServer getServer()
    {
        return mServer;
    }
    
    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof OpenNetworkPath)) return false;
        return super.equals(o);
    }

    @Override
    public Boolean canWrite() {
        return false; // disable network writing until we can get it fixed
    }

    @Override
    public final Boolean requiresThread() {
        return true;
    }

    public String getTempFileName() {
        return getUri().getScheme() + "-" + getName() + "-"
                + Utils.md5(getPath()).replaceAll("[^A-Za-z0-9\\.]", "-");
    }

    public OpenFile getTempFile() {
        OpenFile root = OpenFile.getTempFileRoot();
        if (root != null)
            return root.getChild(getTempFileName());
        return null;
    }

    public OpenFile tempDownload(AsyncTask task) throws IOException {
        if(DEBUG)
            Logger.LogDebug("tempDownload() on " + getPath());
        OpenFile tmp = getTempFile();
        if (tmp == null)
            throw new IOException("Unable to download Temp file");
        if (!tmp.exists())
            tmp.create();
        else if (tmp.length() > 0 && lastModified() != null && tmp.lastModified() != null
                && lastModified() < tmp.lastModified()) {
            Logger.LogWarning("Remote file is older than local temp file.");
            return tmp;
        }
        copyTo(tmp, task);
        return tmp;
    }

    public void tempUpload(AsyncTask task) throws IOException {
        if(DEBUG)
            Logger.LogDebug("tempUpload() on " + getPath());
        OpenFile tmp = getTempFile();
        if (tmp == null)
            throw new IOException("Unable to download Temp file");
        if (!tmp.exists())
            tmp.create();
        else if (lastModified() != null && tmp.lastModified() != null
                && lastModified() >= tmp.lastModified()) {
            Logger.LogWarning("Remote file is newer than local temp file.");
            return;
        }
        copyFrom(tmp, task);
    }
    
    public static boolean isEnabled(Context context)
    {
        return true;
    }
    
    @Override
    public boolean hasThumbnail() {
        return false;
    }

    /**
     * Upload file (used during tempUpload).
     * 
     * @param f Local file to upload.
     * @param l Network listener for logging (since exceptions are caught).
     * @return True if transfer was successful, false otherwise.
     */
    public abstract boolean syncUpload(OpenFile f, NetworkListener l);

    /**
     * Download file (used during tempDownload).
     * 
     * @param f Local file to download to.
     * @param l Network listener for logging (since exceptions are caught).
     * @return True if transfer was successful, false otherwise.
     */
    public abstract boolean syncDownload(OpenFile f, NetworkListener l);

    public boolean copyFrom(OpenFile f, final AsyncTask task) {
        if (task == null)
            return syncUpload(f, OpenNetworkPath.NetworkListener.DefaultListener);
        return syncUpload(f, new NetworkListener() {
            public void OnNetworkFailure(OpenNetworkPath np, OpenFile dest, Exception e) {
                Logger.LogError("copyFrom: Network failure for " + np, e);
            }

            public void OnNetworkCopyUpdate(Integer... progress) {
                if (task instanceof TextEditorFragment.FileLoadTask)
                    ((TextEditorFragment.FileLoadTask)task).showProgress(progress);
                else if (task instanceof TextEditorFragment.FileSaveTask)
                    ((TextEditorFragment.FileSaveTask)task).showProgress(progress);
            }

            public void OnNetworkCopyFinished(OpenNetworkPath np, OpenFile dest) {
                if (task instanceof TextEditorFragment.FileLoadTask)
                    ((TextEditorFragment.FileLoadTask)task).showProgress();
                else if (task instanceof TextEditorFragment.FileSaveTask)
                    ((TextEditorFragment.FileSaveTask)task).showProgress();
            }
        });
    }

    public boolean copyTo(OpenFile f, final AsyncTask task) {
        return syncDownload(f, new NetworkListener() {
            public void OnNetworkFailure(OpenNetworkPath np, OpenFile dest, Exception e) {
                Logger.LogError("copyTo: Network failure for " + np, e);
            }

            public void OnNetworkCopyUpdate(Integer... progress) {
                if (task instanceof TextEditorFragment.FileLoadTask)
                    ((TextEditorFragment.FileLoadTask)task).showProgress(progress);
                else if (task instanceof TextEditorFragment.FileSaveTask)
                    ((TextEditorFragment.FileSaveTask)task).showProgress(progress);
            }

            public void OnNetworkCopyFinished(OpenNetworkPath np, OpenFile dest) {
                if (task instanceof TextEditorFragment.FileLoadTask)
                    ((TextEditorFragment.FileLoadTask)task).showProgress();
                else if (task instanceof TextEditorFragment.FileSaveTask)
                    ((TextEditorFragment.FileSaveTask)task).showProgress();
            }
        });
    }
    
    public interface PipeNeeded {
        public boolean isConnected() throws IOException;
        public void connect() throws IOException, JSchException;
        public void disconnect();
    }

    public interface CloudCompletionListener extends OpenPath.ExceptionListener {
        public void onCloudComplete(String status);
    }
    public interface CloudProgressListener extends CloudCompletionListener {
        public void onProgress(long bytes);
    }
    
    public interface CloudDeleteListener extends OpenPath.ExceptionListener {
        public void onDeleteComplete(String status);
    }
    
    public interface Cancellable {
        public boolean cancel();
    }
    
    public interface CloudOpsHandler extends ListHandler {
        public boolean copyTo(OpenNetworkPath folder, CloudCompletionListener callback);
        public boolean delete(CloudDeleteListener callback);
        public Cancellable uploadToCloud(OpenFile file, CloudProgressListener callback);
        public Cancellable downloadFromCloud(OpenFile file, CloudProgressListener callback);
        public boolean touch(CloudCompletionListener callback);
    }
    
    public static Cancellable runCloud(final Runnable r, final CloudCompletionListener listener)
    {
        return getThreadCancellor(thread(new Runnable() {
            public void run() {
                try {
                    r.run();
                    post(new Runnable() {
                        public void run() {
                            listener.onCloudComplete("Complete");
                        }
                    });
                } catch(Exception e) {
                    postException(e, listener);
                }
            }
        }));
    }
    
    public static Cancellable getThreadCancellor(final Thread thread)
    {
        return new Cancellable() {
            public boolean cancel() {
                if(thread.isAlive())
                {
                    thread.interrupt();
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * This does not change the actual path of the underlying object, just what
     * is displayed to the user.
     * 
     * @param name New title for OpenPath object
     */
    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public String getName(String defaultName) {
        return mName != null ? mName : defaultName;
    }
    
    public final String getRemotePath() {
        String name = getName();
        if (!name.endsWith("/") && !name.startsWith("/"))
            name = "/" + name;
        return getUri().getPath().replace(name, "");
    }

    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public UserInfo setUserInfo(UserInfo info) {
        Logger.LogDebug("setUserInfo: " + info);
        mUserInfo = info;
        return mUserInfo;
    }

    public void addIdentity(byte[] privKey, byte[] pubKey, byte[] privPwd) {
        try {
            Logger.LogDebug("addIdentity");
            DefaultJSch.addIdentity("id", privKey, pubKey, privPwd);
        } catch (JSchException e) {
            Logger.LogError("addIdentity", e);
        }
    }

    public int getServerIndex() {
        if(mServer != null)
            return mServer.getServerIndex();
        return -1;
    }

    public Thread list(final ListListener listener) {
        return thread(new Runnable() {
            public void run() {
                try {
                    listFiles();
                    postListReceived(getChildren(), listener);
                    getParent(); // just make sure we have parents
                } catch(final Exception e) {
                    postException(e, listener);
                }
            }
        });
    }

    public abstract OpenNetworkPath[] getChildren();

    @Override
    public String toString() {
        return getName(super.toString());
    }

    public void setPort(int port) {
        mPort = port;
    }

    public int getPort() {
        return mPort;
    }

    @Override
    public String getDetails(boolean countHiddenChildren) {
        String deets = "";

        if (!isDirectory())
            deets += OpenPath.formatSize(length());

        return deets;
    }

    public InputStream getInputStream() throws IOException {
        return tempDownload(null).getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return tempDownload(null).getOutputStream();
    }
}
