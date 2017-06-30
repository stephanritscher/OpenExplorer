
package org.brandroid.openmanager.data;

import android.database.Cursor;
import android.net.Uri;

import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.SortType;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import jcifs.smb.AllocInfo;
import jcifs.smb.Handler;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class OpenSMB extends OpenNetworkPath implements OpenNetworkPath.PipeNeeded, OpenPath.SpaceHandler {
    private SmbFile mFile;
    private OpenSMB mParent;
    private OpenSMB[] mChildren = null;
    private Long mSize = null;
    private Long mModified = null;
    private Long mDiskSpace = 0l;
    private Long mDiskFreeSpace = 0l;
    private Integer mAttributes = null;
    private static final int MAX_BUFFER = 128 * 1024;

    public OpenSMB(String urlString) throws MalformedURLException {
        URL url = new URL(null, urlString, Handler.SMB_HANDLER);
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(url.getUserInfo());
        if (auth.getPassword() == null || "".equals(auth.getPassword())) {
            OpenServers servers = OpenServers.getDefaultServers();
            OpenServer s = servers.findByUser("smb", url.getHost(), auth.getUsername());
            if (s != null) {
                auth.setUsername(s.getUser());
                auth.setPassword(s.getPassword());
            }
        }
        mFile = new SmbFile(url, auth);
        mParent = null;
        mSize = mModified = null;
    }

    public OpenSMB(SmbFile file) {
        mFile = file;
        mParent = null;
        mSize = mModified = null;
    }

    public OpenSMB(OpenSMB parent, SmbFile kid) {
        mParent = parent;
        mFile = kid;
        try {
            if (kid.isConnected()) {
                mAttributes = kid.getAttributes();
                mSize = kid.length();
                mModified = kid.lastModified();
            }
        } catch (SmbException e) {
            Logger.LogError("Error creating SMB", e);
        }
    }

    public OpenSMB(String urlString, long size, long modified) throws MalformedURLException {
        URL url = new URL(null, urlString, Handler.SMB_HANDLER);
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(url.getUserInfo());
        if (auth.getUsername() == null || "".equals(auth.getUsername()) || auth.getPassword() == null
                || "".equals(auth.getPassword())) {
            OpenServers servers = OpenServers.getDefaultServers();
            OpenServer s = servers.findByUser("smb", url.getHost(), auth.getUsername());
            if (s == null)
                s = servers.findByHost("smb", url.getHost());
            if (s != null) {
                auth.setUsername(s.getUser());
                auth.setPassword(s.getPassword());
            }
        }
        mFile = new SmbFile(url, auth);
        mParent = null;
        mSize = size;
        mModified = modified;
    }
    
    public String getHost()
    {
    	return mFile.getURL().getHost();
    }

    @Override
    public void disconnect() {
        mFile.disconnect();
    }

    @Override
    public boolean isConnected() throws IOException {
        if (mFile == null)
            return false;
        return mFile.isConnected();
    }

    @Override
    public String getName() {
        return Utils.ifNull(mName, mFile.getName());
    }

    @Override
    public String getPath() {
        URL url = mFile.getURL();
        String user = url.getUserInfo();
        if (user != null) {
            if (user.indexOf(":") > -1)
                user = user.substring(0, user.indexOf(":"));
            if (!user.equals(""))
                user += "@";
        } else
            user = "";
        return url.getProtocol() + "://" + user + url.getHost() + url.getPath();
    }

    @Override
    public long length() {
        if (mSize != null)
            return mSize;
        if (isDirectory())
            return 0l;
        try {
            mSize = mFile.length();
            return mSize;
        } catch (Exception e) {
            Logger.LogError("Couldn't get SMB length", e);
            return 0l;
        }
    }

    @Override
    public int getAttributes() {
        if (mAttributes != null)
            return mAttributes;
        try {
            if (!Thread.currentThread().equals(OpenExplorer.UiThread))
                mAttributes = mFile.getAttributes();
            else
                return 0;
        } catch (SmbException e) {
        }
        return mAttributes;
    }

    @Override
    public String getAbsolutePath() {
        String ret = "smb://";
        if(getServer() != null)
            ret = getServer().getAbsolutePath();
        if(!ret.endsWith("/") && !mFile.getURL().getPath().startsWith("/"))
            ret += "/";
        ret += mFile.getURL().getPath();
        return ret;
    }

    @Override
    public void setName(String name) {
        mName = name;
    }

    @Override
    public OpenSMB getParent() {
        return mParent;
    }

    @Override
    public OpenPath getChild(String name) {
        try {
            for (OpenSMB kid : listFiles()) {
                if (kid.getName().equalsIgnoreCase(name))
                    return kid;
            }
        } catch (IOException e) {
        }
        return null;
    }

    @Override
    public OpenSMB[] list() throws IOException {
        if (mChildren != null)
            return mChildren;
        return listFiles();
    }
    
    @Override
    public Thread list(final ListListener listener) {
        return thread(new Runnable() {
            public void run() {
                try {
                    listFiles();
                    postListReceived(getChildren(), listener);
                    getParent(); // just make sure we have parents
                    mDiskSpace = mFile.getDiskSpace();
                    mDiskFreeSpace = mFile.getDiskFreeSpace();
                    AllocInfo disk = mFile.getDiskInfo();
                    if (disk != null) {
                        mDiskSpace = disk.getCapacity();
                        mDiskFreeSpace = disk.getFree();
                    }
                } catch(final Exception e) {
                    postException(e, listener);
                }
            }
        });
    }

    @Override
    public OpenSMB[] listFiles() throws IOException {
        if (Thread.currentThread().equals(OpenExplorer.UiThread))
            return getChildren();
        SmbFile[] kids = null;
        try {
            getAttributes();
            int opts = SmbFile.ATTR_DIRECTORY | SmbFile.ATTR_SYSTEM;
            if (!ShowHiddenFiles)
                opts |= SmbFile.ATTR_HIDDEN;
            kids = mFile.listFiles("*", opts, null, null);
        } catch (SmbAuthException e) {
            Logger.LogWarning("Unable to authenticate. Trying to get password from Servers.");
            mFile.disconnect();
            String path = getServerPath(mFile.getPath());
            URL url = new URL(null, path, Handler.SMB_HANDLER);
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(url.getUserInfo());
            mFile = new SmbFile(mFile.getPath(), auth);
            kids = mFile.listFiles();
        }
        if (kids != null) {
            mChildren = new OpenSMB[kids.length];
            for (int i = 0; i < kids.length; i++) {
                kids[i].setAuth(mFile.getAuth());
                // Logger.LogDebug(kids[i].getPath() + " = " +
                // kids[i].length());
                OpenSMB smb = new OpenSMB(this, kids[i]);
                mChildren[i] = smb;
                FileManager.setOpenCache(smb.getPath(), smb);
            }
        }
        return mChildren;
    }

    @Override
    public OpenSMB[] getChildren() {
        return mChildren;
    }

    @Override
    public Boolean isDirectory() {
        if (mFile.getURL().getPath().endsWith("/"))
            return true;
        if (mAttributes != null)
            return (mAttributes & SmbFile.ATTR_DIRECTORY) == SmbFile.ATTR_DIRECTORY;
        if (!Thread.currentThread().equals(OpenExplorer.UiThread))
            try {
                return mFile.isDirectory();
            } catch (SmbException e) {
            }
        return getPath().endsWith("/");
    }

    @Override
    public Boolean isFile() {
        return !isDirectory();
    }

    @Override
    public Boolean isHidden() {
        try {
            if (mAttributes != null)
                return (mAttributes & SmbFile.ATTR_HIDDEN) == SmbFile.ATTR_HIDDEN;
            else if (!Thread.currentThread().equals(OpenExplorer.UiThread))
                return mFile.isHidden();
            else
                return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public Uri getUri() {
        return Uri.parse(mFile.getPath());
    }

    @Override
    public Long lastModified() {
        if (mModified != null || Thread.currentThread().equals(OpenExplorer.UiThread))
            return mModified != null ? mModified : 0;
        else
            return mFile.getLastModified();
    }

    @Override
    public Boolean canRead() {
        return true;
        /*
         * try { if(Thread.currentThread().equals(OpenExplorer.UiThread)) return
         * true; else return mFile.canRead(); } catch (SmbException e) { return
         * false; }
         */
    }

    @Override
    public Boolean canWrite() {
        if (!super.canWrite())
            return false;
        try {
            if (mAttributes != null)
                return (mAttributes & SmbFile.ATTR_READONLY) == 0;
            if (Thread.currentThread().equals(OpenExplorer.UiThread))
                return true;
            else {
                mAttributes = mFile.getAttributes();
                return (mAttributes & SmbFile.ATTR_READONLY) == 0;
            }
        } catch (SmbException e) {
            return false;
        }
    }

    @Override
    public Boolean canExecute() {
        return false;
    }

    @Override
    public Boolean exists() {
        try {
            if (Thread.currentThread().equals(OpenExplorer.UiThread))
                return true;
            else
                return mFile.exists();
        } catch (SmbException e) {
            return false;
        }
    }

    @Override
    public Boolean delete() {
        try {
            mFile.delete();
            return true;
        } catch (SmbException e) {
            return false;
        }
    }

    @Override
    public Boolean mkdir() {
        try {
            mFile.mkdir();
            return true;
        } catch (SmbException e) {
            return false;
        }
    }

    /*
     * @Override public InputStream getInputStream() throws IOException { return
     * mFile.getInputStream(); }
     * @Override public OutputStream getOutputStream() throws IOException {
     * return mFile.getOutputStream(); }
     */

    @Override
    public boolean syncUpload(OpenFile f, NetworkListener l) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = mFile.getInputStream();
            os = new BufferedOutputStream(getFile().getOutputStream());
            while (is.available() > 0) {
                byte[] buffer = new byte[Math.min(MAX_BUFFER, is.available())];
                if (is.read(buffer) > 0)
                    os.write(buffer);
            }
            l.OnNetworkCopyFinished(this, f);
            return true;
        } catch (IOException e) {
            l.OnNetworkFailure(this, f, e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                }
        }
        return false;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return mFile.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return mFile.getOutputStream();
    }

    @Override
    public boolean syncDownload(OpenFile f, NetworkListener l) {
        try {
            copyTo(f, null);
            l.OnNetworkCopyFinished(this, f);
            return true;
        } catch (Exception e) {
            l.OnNetworkFailure(this, f, e);
        }
        return false;
    }

    public void copyTo(OpenFile dest, BackgroundWork task) throws SmbException {
        try {
            /*
             * if(dest.isDirectory()) { dest = dest.getChild(getName());
             * if(!dest.exists()) if(!dest.create()) throw new
             * IOException("Unable to create destination file."); }
             */
            Logger.LogDebug("OpenSMB.copyTo - starting " + dest);
            mFile.copyTo(dest, task);
            if (dest.length() <= 0 || dest.length() < length())
                throw new IOException("Copied file size is too small.");
            Logger.LogVerbose("OpenSMB.copyTo - Copied " + dest.length() + " bytes - " + dest);
        } catch (Exception e) {
            Logger.LogError("OpenSMB couldn't copy using Channels. Falling back to Streams.", e);
            OutputStream os = null;
            InputStream is = null;
            try {
                is = new BufferedInputStream(mFile.getInputStream());
                os = dest.getOutputStream();
                int pos = 0;
                int len = (int)length();
                while (is.available() > 0) {
                    byte[] buffer = new byte[Math.min(MAX_BUFFER, len - pos)];
                    int read = is.read(buffer);
                    if (read <= 0)
                        break;
                    os.write(buffer);
                    pos += read;
                    task.publishMyProgress(pos, pos, len);
                }
            } catch (Exception e2) {
                throw new SmbException("Unable to fallback", e);
            } finally {
                if (os != null)
                    try {
                        os.close();
                    } catch (IOException e1) {
                    }
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e1) {
                    }
            }
        }
    }

    public boolean copyTo(OpenSMB dest) {
        try {
            mFile.copyTo(dest.getFile());
            return true;
        } catch (SmbException sex) {
            Logger.LogError("Error copying SMB -> SMB.", sex);
            return false;
        }
    }

    public SmbFile getFile() {
        return mFile;
    }

    private String getServerPath(String path) {
        OpenServer server = null;
        if (getServerIndex() >= 0)
            server = OpenServers.getDefaultServers().get(getServerIndex());
        else {
            Uri uri = Uri.parse(path);
            String user = uri.getUserInfo();
            if (user.indexOf(":") > -1)
                user = user.substring(0, user.indexOf(":"));
            server = OpenServers.getDefaultServers().findByPath("smb", uri.getHost(), user,
                    uri.getPath());
            if (server == null)
                server = OpenServers.getDefaultServers().findByUser("smb", uri.getHost(), user);
            if (server == null)
                server = OpenServers.getDefaultServers().findByHost("smb", uri.getHost());

            Logger.LogVerbose("User: " + user + " :: " + server.getUser() + ":"
                    + server.getPassword().substring(0, 1)
                    + server.getPassword().substring(1).replaceAll(".", "*"));
        }
        if (server == null)
            Logger.LogWarning("Couldn't find server for Server Path");
        else if ((server.getPassword() == null || server.getPassword().equals(""))
                && mUserInfo != null) {
            if ((mUserInfo.getPassword() != null && !mUserInfo.getPassword().equals(""))
                    || mUserInfo.promptPassword("Password for " + server.getHost()))
                path = "smb://" + server.getUser() + ":" + mUserInfo.getPassword() + "@"
                        + server.getHost() + Uri.parse(path).getPath();
        }
        if (server != null)
            path = "smb://" + server.getUser() + ":" + server.getPassword() + "@"
                    + server.getHost() + Uri.parse(path).getPath();
        else
            Logger.LogWarning("Couldn't find server for Server Path");
        return path;
    }

    @Override
    public boolean listFromDb(SortType sort) {
        if (!AllowDBCache)
            return false;
        String parent = getPath(); // .replace("/" + getName(), "");
        if (!parent.endsWith("/"))
            parent += "/";
        Logger.LogDebug("Fetching from folder: " + parent);
        Cursor c = mDb.fetchItemsFromFolder(parent, sort);
        if (c == null) {
            Logger.LogWarning("DB Fetch returned null?");
            return false;
        }
        ArrayList<OpenSMB> arr = new ArrayList<OpenSMB>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            String folder = c
                    .getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
            String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
            int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
            int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
            OpenSMB child;
            try {
                String path = folder + name;
                /* path = getServerPath(path); */
                child = new OpenSMB(path, size, modified);
                arr.add(child);
            } catch (MalformedURLException e) {
            }
            c.moveToNext();
        }
        Logger.LogDebug("listFromDb returning " + arr.size() + " children");
        c.close();
        mChildren = arr.toArray(new OpenSMB[0]);
        return true;
    }

    public long getDiskSpace() {
        if(mDiskSpace != null)
            return mDiskSpace;
        if (!Thread.currentThread().equals(OpenExplorer.UiThread))
            try {
                mDiskSpace = mFile.getDiskSpace();
            } catch (SmbException e) {
            }
        return mDiskSpace;
    }

    public long getDiskFreeSpace() {
        if(mDiskFreeSpace != null)
            return mDiskFreeSpace;
        if (!Thread.currentThread().equals(OpenExplorer.UiThread))
            try {
                mDiskFreeSpace = mFile.getDiskFreeSpace();
            } catch (SmbException e) {
            }
        return mDiskFreeSpace;
    }

    @Override
    public void clearChildren() {
        mChildren = null;
    }

    public long getTotalSpace() {
        return getDiskSpace();
    }

    public long getUsedSpace() {
        return getDiskSpace() - getDiskFreeSpace();
    }
    
    public long getFreeSpace() {
        return getDiskFreeSpace();
    }
    
    @Override
    public void getSpace(final SpaceListener callback) {
        if(mDiskSpace != null)
        {
            callback.onSpaceReturned(mDiskSpace, getUsedSpace(), 0);
            return;
        }
        thread(new Runnable() {
            public void run() {
                final long t = getTotalSpace();
                final long u = getUsedSpace();
                post(new Runnable() {
                    public void run() {
                        callback.onSpaceReturned(t, u, 0);
                    }
                });
            }
        });
    }

    @Override
    public void connect() throws IOException {
        mFile.connect();
    }
}
