/*
    Open Explorer, an open source file explorer & text editor
    Copyright (C) 2011 Brandon Bowles <brandroid64@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.brandroid.openmanager.util;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.box.androidlib.User;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.jcraft.jsch.UserInfo;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.FTPManager;
import org.brandroid.openmanager.data.OpenBox;
import org.brandroid.openmanager.data.OpenContent;
import org.brandroid.openmanager.data.OpenDrive;
import org.brandroid.openmanager.data.OpenDropBox;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenFileRoot;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenPath.OpenStream;
import org.brandroid.openmanager.data.OpenSCP;
import org.brandroid.openmanager.data.OpenSFTP;
import org.brandroid.openmanager.data.OpenSMB;
import org.brandroid.openmanager.data.OpenSearch;
import org.brandroid.openmanager.data.OpenSearch.SearchProgressUpdateListener;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.data.OpenZip;
import org.brandroid.openmanager.data.OpenZip.OpenZipVirtualPath;
import org.brandroid.utils.Logger;
import org.brandroid.utils.Preferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileManager {
    public static final int BUFFER = 128 * 1024;

    private boolean mShowHiddenFiles = false;
    private SortType mSorting = SortType.ALPHA;
    private long mDirSize = 0;
    private static WeakHashMap<String, OpenPath> mOpenCache = new WeakHashMap<String, OpenPath>();
    public static UserInfo DefaultUserInfo;
    private OnProgressUpdateCallback mCallback = null;

    public interface OnProgressUpdateCallback {
        public void onProgressUpdateCallback(Integer... vals);
    }

    public void setProgressListener(OnProgressUpdateCallback listener) {
        mCallback = listener;
    }

    public void updateProgress(Integer... vals) {
        if (mCallback != null)
            mCallback.onProgressUpdateCallback(vals);
    }

    /**
     * Constructs an object of the class <br>
     * this class uses a stack to handle the navigation of directories.
     */
    public FileManager() {
    }

    /**
     * @param zip
     * @param files
     */
    public void createZipFile(OpenPath zip, OpenPath[] files) {
        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
                    ((OpenFile)zip).getFile()), BUFFER));

            int total = 0;
            OpenPath relPath = files[0].getParent();
            for (OpenPath file : files) {
                if (relPath == null
                        || (file.getParent() != null && relPath.getPath().startsWith(
                                file.getParent().getPath())))
                    relPath = file.getParent();
                total += file.length();
            }
            Logger.LogDebug("Zipping " + total + " bytes!");
            for (OpenPath file : files) {
                try {
                    zipIt(file, zout, total, relPath.getPath()
                            + (relPath.getPath().endsWith("/") ? "" : "/"));
                } catch (IOException e) {
                    Logger.LogError("Error zipping file.", e);
                }
            }

            zout.close();

        } catch (FileNotFoundException e) {
            Log.e("File not found", e.getMessage());

        } catch (IOException e) {
            Log.e("IOException", e.getMessage());
        } finally {
            if (zout != null)
                try {
                    zout.close();
                } catch (IOException e) {
                    Logger.LogError("Error closing zip file", e);
                }
        }
    }

    private void zipIt(OpenPath file, ZipOutputStream zout, int totalSize, String relativePath)
            throws IOException {
        byte[] data = new byte[BUFFER];
        int read;

        if (file.isFile() && file instanceof OpenStream) {
            String name = file.getPath();
            if (relativePath != null && name.startsWith(relativePath))
                name = name.substring(relativePath.length());
            ZipEntry entry = new ZipEntry(name);
            zout.putNextEntry(entry);
            BufferedInputStream instream = new BufferedInputStream(
                    ((OpenStream)file).getInputStream());
            // Logger.LogVerbose("zip_folder file name = " + entry.getName());
            int size = (int)file.length();
            int pos = 0;
            while ((read = instream.read(data, 0, BUFFER)) != -1) {
                pos += read;
                zout.write(data, 0, read);
                updateProgress(pos, size, totalSize);
            }

            zout.closeEntry();
            instream.close();

        } else if (file.isDirectory()) {
            // Logger.LogDebug("zip_folder dir name = " + file.getPath());
            for (OpenPath kid : file.list())
                totalSize += kid.length();
            for (OpenPath kid : file.list())
                zipIt(kid, zout, totalSize, relativePath);
        }
    }

    /**
     * @param file
     * @param newName
     * @return
     */
    public static boolean renameTarget(OpenFile file, String newName) {
        try {
            file.rename(newName);
            return true;
        } catch (Exception e) {
            Logger.LogError("Unable to rename file to [" + newName + "]: " + file, e);
            return false;
        }
    }

    /**
     * The full path name of the file to delete.
     * 
     * @param target name
     * @return Number of Files deleted
     */
    public int deleteTarget(OpenPath target) {

        int ret = 0;

        if (!target.exists())
            Logger.LogWarning("Unable to delete target as it no longer exists: " + target.getPath());

        if (target.isFile()) {
            if (target.delete())
                ret++;
            else
                Logger.LogError("Unable to delete target file: " + target.getPath());
        }

        else if (target.exists() && target.isDirectory() && target.canRead()) {
            OpenPath[] file_list = null;
            try {
                file_list = target.list();
                if(file_list == null)
                	file_list = target.listFiles();
            } catch (IOException e) {
                Logger.LogError("Error listing children to delete.", e);
            }

            if (file_list != null && file_list.length > 0) {

                for (int i = 0; i < file_list.length; i++)
                    ret += deleteTarget(file_list[i]);

            }
            if (target.exists()) {
                // Directory is now empty, please delete!
                if (target.delete())
                    ret++;
                else
                    Logger.LogError("Couldn't delete target " + target.getPath());
            } else
                Logger.LogWarning("Unable to delete target as it does not exist: "
                        + target.getPath());
        }
        return ret;
    }

    /**
     * converts integer from wifi manager to an IP address.
     * 
     * @param ip
     * @return
     */
    public static String integerToIPAddress(int ip) {
        String ascii_address = "";
        int[] num = new int[4];

        num[0] = (ip & 0xff000000) >> 24;
        num[1] = (ip & 0x00ff0000) >> 16;
        num[2] = (ip & 0x0000ff00) >> 8;
        num[3] = ip & 0x000000ff;

        ascii_address = num[0] + "." + num[1] + "." + num[2] + "." + num[3];

        return ascii_address;
    }

    public static void clearOpenCache() {
        if (mOpenCache != null)
            mOpenCache.clear();
    }

    public static boolean hasOpenCache(String path) {
        return mOpenCache != null && mOpenCache.containsKey(path);
    }

    public static OpenPath removeOpenCache(String path) {
        return mOpenCache.remove(path);
    }

    public static OpenPath getOpenCache(String path) {
        return getOpenCache(path, null);
    }

    public static OpenPath getOpenCache(String path, Context c) {
        if (path == null)
            return null;
        if (mOpenCache == null)
            mOpenCache = new WeakHashMap<String, OpenPath>();
        OpenPath ret = mOpenCache.get(path);
        if (ret == null) {
            Uri uri = Uri.parse(path);
            Logger.LogDebug("getOpenCache for " + uri.getScheme() + "://" + (uri.getUserInfo() != null ?
                    uri.getUserInfo().replaceFirst(":.*", ":...") + "@" : "") + uri.getHost() + ":" + uri.getPort() +
                    uri.getPath());
            if (path.startsWith("/")) {
                ret = new OpenFile(path);
                if (path.equals("/data") || path.startsWith("/data/"))
                    ret = new OpenFileRoot(ret);
            } else if (path.startsWith("ftp:/"))
                ret = new OpenFTP(path, null, new FTPManager());
            else if (path.startsWith("sftp:/"))
                ret = new OpenSFTP(path);
            else if (path.startsWith("smb:/"))
                try {
                    ret = new OpenSMB(path);
                } catch (MalformedURLException e) {
                    Logger.LogError("FileManager.getOpenCache unable to instantiate SMB");
                }
            else if (path.equals("Videos"))
                ret = OpenExplorer.getVideoParent();
            else if (path.equals("Photos"))
                ret = OpenExplorer.getPhotoParent();
            else if (path.equals("Music"))
                ret = OpenExplorer.getMusicParent();
            else if (path.equals("Downloads"))
                ret = OpenExplorer.getDownloadParent();
            else if (path.equals("External")
                    && !checkForNoMedia(OpenFile.getExternalMemoryDrive(false)))
                ret = OpenFile.getExternalMemoryDrive(false);
            else if (path.equals("Internal") || path.equals("External"))
                ret = OpenFile.getInternalMemoryDrive();
            else if (path.startsWith("content://org.brandroid.openmanager/search/")) {
                String query = path.replace("content://org.brandroid.openmanager/search/", "");
                path = path.substring(query.indexOf("/") + 1);
                if (query.indexOf("/") > -1)
                    query = Uri.decode(query.substring(0, query.indexOf("/")));
                else
                    query = "";
                ret = new OpenSearch(query, getOpenCache(path), (SearchProgressUpdateListener) null);
            } else if (path.startsWith("content://") && c != null)
                ret = new OpenContent(uri, c);
            else
                ret = null;
        }
        return ret;
    }

    public static boolean checkForNoMedia(OpenPath defPath) {
        if (defPath == null)
            return true;
        if (defPath instanceof OpenFile) {
            StatFs sf = new StatFs(defPath.getPath());
            if (sf.getBlockCount() == 0)
                return true;
            else
                return false;
        } else {
            try {
                return defPath.list() == null || defPath.list().length == 0;
            } catch (IOException e) {
                Logger.LogError("Error Checking for Media.", e);
                return true;
            }
        }
    }

    public static OpenPath getOpenCache(String path, Boolean bGetNetworkedFiles, SortType sort)
            throws IOException // , SmbAuthException, SmbException
    {
        if (path == null)
            return null;
        if (mOpenCache == null)
            mOpenCache = new WeakHashMap<String, OpenPath>();
        OpenPath ret = mOpenCache.get(path);
        OpenServers servers = OpenServers.getDefaultServers();
        if (ret == null) {
            Uri uri = Uri.parse(path);
            Logger.LogDebug("Initializing new connection for " + uri.getScheme() + "://" + (uri.getUserInfo() != null ?
                    uri.getUserInfo().replaceFirst(":.*", ":...") + "@" : "") + uri.getHost() + ":" + uri.getPort() +
                    uri.getPath());
            if (path.startsWith("ftp:/") && servers != null) {
                FTPManager man = new FTPManager(path);
                FTPFile file = new FTPFile();
                file.setName(path.substring(path.lastIndexOf("/") + 1));
                OpenServer server = servers.findByHost("ftp", uri.getHost());
                man.setUser(server.getUser());
                man.setPassword(server.getPassword());
                ret = new OpenFTP(null, file, man);
            } else if (path.startsWith("scp:/")) {
                ret = new OpenSCP(uri.getHost(), uri.getUserInfo(), uri.getPath(), null);
            } else if (path.startsWith("sftp:/") && servers != null) {
                OpenServer server = servers.findByHost("sftp", uri.getHost());
                if (server == null)
                    Logger.LogError("Could not find OpenServer for sftp host " + uri.getHost());
                ret = new OpenSFTP(uri);
                if (server.getPrivKey() != null && server.getPubKey() != null) {
                    ((OpenSFTP) ret).addIdentity(server.getPrivKey().getBytes(), server.getPubKey().getBytes(),
                            server.getPassword().getBytes());
                    SimpleUserInfo info = new SimpleUserInfo();
                    ((OpenSFTP) ret).setUserInfo(info);
                } else {
                    SimpleUserInfo info = new SimpleUserInfo();
                    info.setPassword(server.getPassword());
                    ((OpenSFTP) ret).setUserInfo(info);
                }
            } else if (path.startsWith("smb:/") && servers != null) {
                try {
                    String user = uri.getUserInfo();
                    if (user != null && user.indexOf(":") > -1)
                        user = user.substring(0, user.indexOf(":"));
                    else
                        user = "";
                    OpenServer server = servers.findByPath("smb", uri.getHost(),
                            user, uri.getPath());
                    if (server == null)
                        server = servers.findByUser("smb", uri.getHost(), user);
                    if (server == null)
                        server = servers.findByHost("smb", uri.getHost());
                    if (server == null)
                    	server = servers.findByType("smb").size() > 0 ? servers.findByType("smb").get(0) : null;
                    if(server != null && user.equals(""))
                        user = server.getUser();
                    if (server != null && server.getPassword() != null
                            && server.getPassword() != "")
                        user += ":" + server.getPassword();
                    if (!user.equals(""))
                        user += "@";
                    ret = new OpenSMB(uri.getScheme() + "://" + user + uri.getHost()
                            + uri.getPath());
                } catch (Exception e) {
                    Logger.LogError("Couldn't get samba from cache.", e);
                }
            } else if (path.startsWith("box")) {
                try {
                    String us = uri.getUserInfo();
                    String pw = us;
                    if (pw != null && pw.indexOf(":") > -1)
                    {
                        us = us.substring(0, us.indexOf(":"));
                        pw = pw.substring(pw.indexOf(":") + 1);
                    }
                    OpenServer server = servers.findByUser("box", null, us);
                    if (server != null)
                        pw = server.getPassword();
                    User token = new User();
                    token.setAuthToken(pw);
                    ret = new OpenBox(token);
                    try {
                        if (uri.getPathSegments().size() > 0)
                            ((OpenBox)ret).setId(Long.parseLong(uri.getLastPathSegment()));
                    } catch (Exception e) {
                    }
                } catch (Exception e) {
                    Logger.LogError("Couldn't get Box.com from cache.", e);
                }
            } else if (path.startsWith("drive")) {
                try {
                    if(OpenExplorer.IS_DEBUG_BUILD)
                        Logger.LogVerbose("Drive path (from " + path + "): " + TextUtils.join(", ", uri.getPathSegments()));
                    String pw = uri.getUserInfo();
                    if (pw != null && pw.indexOf(":") > -1)
                        pw = pw.substring(pw.indexOf(":") + 1);
                    String refresh = "";
                    if (servers != null) {
                        OpenServer server = servers.findByUser("drive", null, pw);
                        if (server != null)
                        {
                            ret = (OpenDrive)server.getOpenPath();
                            if (uri.getPathSegments().size() > 0)
                                ret = ((OpenDrive)ret).setId(uri.getLastPathSegment());
                            return ret;
                        }
                    }
                    ret = new OpenDrive(pw, refresh).setId(uri.getLastPathSegment());
                } catch (Exception e) {
                    Logger.LogError("Couldn't get Drive item from cache.", e);
                }
            } else if (path.startsWith("db")) {
                try {
                    String user = uri.getUserInfo();
                    String pw = "";
                    AccessTokenPair access = null;
                    if (user == null || user.equals(""))
                    {
                        List<OpenServer> dbservers = servers.findByType("dropbox");
                        if (dbservers.size() > 0)
                        {
                            OpenServer server = dbservers.get(0);
                            if (!server.getUser().equals("") && !server.getPassword().equals(""))
                            {
                                user = server.getUser();
                                pw = server.getPassword();
                            }
                        }
                    }
                    if (user != null)
                    {
                        if (user.indexOf(":") > -1)
                        {
                            user = Uri.decode(user.substring(0, user.indexOf(":")));
                            pw = Uri.decode(pw.substring(pw.indexOf(":") + 1));
                        }

                        OpenServer server = servers.findByUser("dropbox", null, user);
                        if (server != null)
                            pw = server.getPassword();
                        access = new AccessTokenPair(user, pw);
                    }
                    AppKeyPair app = new AppKeyPair(
                            PrivatePreferences.getKey("dropbox_key"),
                            PrivatePreferences.getKey("dropbox_secret"));
                    DropboxAPI<AndroidAuthSession> api = new DropboxAPI<AndroidAuthSession>(
                            new AndroidAuthSession(app, AccessType.DROPBOX, access)
                            );
                    ret = new OpenDropBox(api);
                    ((OpenDropBox)ret).setPath(uri.getPath());
                } catch (Exception e) {
                    Logger.LogError("Couldn't get dropbox from cache.", e);
                }
            } /*
               * else if (path.startsWith("/data") || path.startsWith("/system")
               * || path.startsWith("/mnt/shell") || (path.indexOf("/emulated/")
               * > -1 && path.indexOf("/emulated/0") == -1)) ret = new
               * OpenFileRoot(new OpenFile(path));
               */
            else if (path.startsWith("/"))
                ret = new OpenFile(path);
            else if (path.startsWith("file://"))
                ret = new OpenFile(path.replace("file://", ""));
            else if (path.equals("Videos"))
                ret = OpenExplorer.getVideoParent();
            else if (path.equals("Photos"))
                ret = OpenExplorer.getPhotoParent();
            else if (path.equals("Music"))
                ret = OpenExplorer.getMusicParent();
            else if (path.equals("Downloads"))
                ret = OpenExplorer.getDownloadParent();
            if (ret == null)
                return ret;
            if (ret instanceof OpenFile && ret.isArchive() && Preferences.Pref_Zip_Internal)
                ret = new OpenZip((OpenFile)ret);
            else if (ret instanceof OpenFile && path.toLowerCase().indexOf(".zip/") > -1)
            {
            	String pp = path.substring(0, path.toLowerCase().indexOf(".zip/") + 4);
            	String cp = path.substring(pp.length() + 1);
            	OpenZip pz = new OpenZip(new OpenFile(pp));
            	pz.listFiles();
            	ret = null;
            	if(cp.endsWith("/"))
            		ret = pz.findVirtualPath(cp);
            	else if(cp.indexOf("/") > -1) {
            		OpenZip.OpenZipVirtualPath vp = (OpenZipVirtualPath) pz.findVirtualPath(cp.substring(0, cp.lastIndexOf("/")));
            		if(vp != null)
            			ret = vp.getChild(cp.substring(cp.lastIndexOf("/") + 1));
            	} else ret = pz.getChild(cp);
            	if(ret == null)
            		ret = new OpenFile(path);
            	else
            		setOpenCache(path, ret);
            }
            // if (ret instanceof OpenFile
            // && (ret.getMimeType().contains("tar")
            // || ret.getExtension().equalsIgnoreCase("tar")
            // || ret.getExtension().equalsIgnoreCase("win")))
            // ret = new OpenTar((OpenFile)ret);
            if (ret.requiresThread() && bGetNetworkedFiles) {
                if (ret.listFiles() != null)
                    setOpenCache(path, ret);
            } else if (ret instanceof OpenNetworkPath) {
                if (ret.listFromDb(sort))
                    setOpenCache(path, ret);
            }
        }
        // if(ret == null)
        // ret = setOpenCache(path, new OpenFile(path));
        // else setOpenCache(path, ret);
        return ret;
    }
    
    public static OpenPath setOpenCache(OpenPath file) {
    	mOpenCache.put(file.getAbsolutePath(), file);
    	return file;
    }

    public static OpenPath setOpenCache(String path, OpenPath file) {
        // Logger.LogDebug("FileManager.setOpenCache(" + path + ")");
        mOpenCache.put(path, file);
        return file;
    }

    public static void addCacheToDb() {
        for (OpenPath path : mOpenCache.values())
            path.addToDb();
    }

    public OpenPath[] getChildren(OpenPath directory) throws IOException {
        // mDirContent.clear();
        if (directory == null)
            return new OpenPath[0];
        if (!directory.isDirectory())
            return new OpenPath[0];
        return directory.list();
    }

}
