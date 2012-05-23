package org.brandroid.openmanager.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.SortType;
import org.brandroid.utils.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

import android.database.Cursor;
import android.net.Uri;

public class OpenSFTP extends OpenNetworkPath
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3263112609308933024L;
	private long filesize = 0l;
	private Session mSession = null;
	private ChannelSftp mChannel = null;
	private InputStream in = null;
	private OutputStream out = null;
	private final String mHost, mUser, mRemotePath;
	private SftpATTRS mAttrs = null;
	private String mName = null;
	protected OpenSFTP mParent = null;
	private OpenSFTP[] mChildren = null;
	private Long mSize = null;
	private Long mModified = null;
	
	public OpenSFTP(String fullPath)
	{
		//this.jsch = jsch;
		Uri uri = Uri.parse(fullPath);
		mHost = uri.getHost();
		mUser = uri.getUserInfo();
		mRemotePath = uri.getPath();
		if(uri.getPort() > 0)
			setPort(uri.getPort());
		mUserInfo = FileManager.DefaultUserInfo;
	}
	public OpenSFTP(Uri uri)
	{
		//this.jsch = jsch;
		mHost = uri.getHost();
		mUser = uri.getUserInfo();
		mRemotePath = uri.getPath();
		if(uri.getPort() > 0)
			setPort(uri.getPort());
	}
	public OpenSFTP(String host, String user, String path)
	{
		//this.jsch = jsch;
		mHost = host;
		mUser = user;
		mRemotePath = path;
	}
	public OpenSFTP(String host, String user, String path, UserInfo info)
	{
		//this.jsch = jsch;
		mHost = host;
		mUser = user;
		mRemotePath = path;
		mUserInfo = info;
	}
	public OpenSFTP(OpenSFTP parent, LsEntry child)
	{
		//this.jsch = jsch;
		mUserInfo = parent.getUserInfo();
		mHost = parent.getHost();
		mUser = parent.getUser();
		mParent = parent;
		mAttrs = child.getAttrs();
		mSize = mAttrs.getSize();
		mModified = (long) mAttrs.getMTime();
		Uri pUri = mParent.getUri();
		String name = child.getFilename();
		name = name.substring(name.lastIndexOf("/") + 1);
		Uri myUri = Uri.withAppendedPath(pUri, name);
		//Logger.LogDebug("Name: [" + name + "] Parent Uri: [" + pUri.toString() + "] Child Uri: [" + myUri.toString() + "] Path: [" + myUri.getPath() + "]");
		mRemotePath = myUri.getPath();
		mSession = parent.mSession;
		mChannel = parent.mChannel;
		//Logger.LogDebug("Created OpenSFTP @ " + mRemotePath);
	}
	
	public OpenSFTP(String path, int size, int modified) {
		this(Uri.parse(path));
		mSize = (long)size;
		mModified = (long)modified;
	}
	public String getHost() { return mHost; }
	public String getUser() { return mUser; }

	@Override
	public String getName() {
		if(mName != null)
			return mName;
		if(mRemotePath.equals("") || mRemotePath.equals("/"))
			return mHost;
		String ret = mRemotePath.substring(mRemotePath.lastIndexOf("/", mRemotePath.length() - 1) + 1);
		if(ret.equals(""))
			ret = mRemotePath;
		return ret;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int getPort() {
		int port = super.getPort();
		if(port > 0) return port;
		return 22;
	}

	@Override
	public String getPath() {
		return "sftp://" + mUser + "@" + mHost + ":" + getPort() + (mRemotePath.startsWith("/") ? "" : "/") + mRemotePath;
	}

	@Override
	public String getAbsolutePath() {
		return getPath();
	}
	
	@Override
	public void setName(String name) {
		mName = name;
	}

	@Override
	public void setPath(String path) {
		//throw new T("Can't setPath on Networked Paths");
	}

	@Override
	public long length() {
		if(mSize != null)
			return mSize;
		if(mAttrs != null)
			return mAttrs.getMTime();
		return 0;
	}

	@Override
	public OpenPath getParent() {
		if(mParent != null)
			return mParent;
		try {
			if(getUri().getPath().length() > 2)
			{
				String path = getAbsolutePath().replace("/" + getName(), "");
				if(path.length() > 8)
					return new OpenSFTP(path);
			}
		} catch(Exception e) { Logger.LogError("Unable to get OpenSFTP.getParent(" + getPath() + ")", e); }
		return null;
	}
	
	@Override
	public OpenPath getChild(String name) {
		return null;
	}
	
	@Override
	public int getChildCount(boolean countHidden) throws IOException {
		if(mChildren == null) return 0;
		return super.getChildCount(countHidden);
	}

	@Override
	public OpenPath[] list() throws IOException {
		if(mChildren != null) return mChildren;
		return listFiles();
	}

	@Override
	public OpenPath[] listFiles() throws IOException {
		try {
			connect();
			String lsPath = mRemotePath;
			if(lsPath.equals(""))
				lsPath = ".";
			Logger.LogVerbose("ls " + lsPath);
			Vector<LsEntry> vv = mChannel.ls(lsPath);
			mChildren = new OpenSFTP[vv.size()];
			int i = 0;
			for(LsEntry item : vv)
			{
				String name = item.getFilename();
				name = name.substring(name.lastIndexOf("/") + 1);
				if(name.equals(".")) continue;
				if(name.equals("..")) continue;
				mChildren[i++] = new OpenSFTP(this, item);
			}
		} catch (SftpException e) {
			Logger.LogError("SftpException during listFiles", e);
			throw new IOException("SftpException during listFiles");
		} catch (JSchException e) {
			Logger.LogError("JSchException during listFiles", e);
			throw new IOException("JSchException during listFiles");
		}
		FileManager.setOpenCache(getAbsolutePath(), this);
		return mChildren;
	}
	
	@Override
	public OpenNetworkPath[] getChildren() {
		return mChildren;
	}

	@Override
	public Boolean isDirectory() {
		return mAttrs != null ? mAttrs.isDir() : true;
	}

	@Override
	public Boolean isFile() {
		return true;
	}

	@Override
	public Boolean isHidden() {
		return getName().startsWith(".");
	}

	@Override
	public Uri getUri() {
		return Uri.parse("sftp://" + mHost + ":" + getPort() + (!mRemotePath.startsWith("/") ? "/" : "") + mRemotePath);
	}

	@Override
	public Long lastModified() {
		if(mModified != null)
			return mModified;
		return mAttrs != null ? (long)mAttrs.getMTime() : null;
	}

	@Override
	public Boolean canRead() {
		return mAttrs != null && (mAttrs.getPermissions() & SftpATTRS.S_IRUSR) != 0;
	}

	@Override
	public Boolean canWrite() {
		return mAttrs != null && (mAttrs.getPermissions() & SftpATTRS.S_IWUSR) != 0;
	}

	@Override
	public Boolean canExecute() {
		return mAttrs != null && (mAttrs.getPermissions() & SftpATTRS.S_IXUSR) != 0;
	}

	@Override
	public Boolean exists() {
		return true;
	}

	@Override
	public Boolean delete() {
		return false;
	}

	@Override
	public Boolean mkdir() {
		return false;
	}
	

	@Override
	public boolean listFromDb(SortType sort)
	{
		if(!AllowDBCache) return false;
		String parent = getPath(); //.replace("/" + getName(), "");
		if(!parent.endsWith("/"))
			parent += "/";
		Logger.LogDebug("Fetching from folder: " + parent);
		Cursor c = mDb.fetchItemsFromFolder(parent, sort);
		if(c == null) {
			Logger.LogWarning("DB Fetch returned null?");
			return false;
		}
		ArrayList<OpenSFTP> arr = new ArrayList<OpenSFTP>(); 
		c.moveToFirst();
		while(!c.isAfterLast())
		{
			String folder = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_FOLDER));
			String name = c.getString(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_NAME));
			int size = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_SIZE));
			int modified = c.getInt(OpenPathDbAdapter.getKeyIndex(OpenPathDbAdapter.KEY_MTIME));
			arr.add(new OpenSFTP(folder + name, size, modified));
			c.moveToNext();
		}
		Logger.LogDebug("listFromDb returning " + arr.size() + " children");
		c.close();
		mChildren = arr.toArray(new OpenSFTP[0]);
		return true;
	}
	
	@Override
	public void disconnect()
	{
		if(mChannel == null && mSession == null) return;
		super.disconnect();
		if(mChannel != null)
			mChannel.disconnect();
		if(mSession != null)
			mSession.disconnect();
		try {
			if(in != null)
				in.close();
		} catch (IOException e) { }
		try {
			if(out != null)
				out.close();
		} catch (IOException e) { }
	}
	
	@Override
	public void connect() throws IOException
	{
		if(mSession != null && mSession.isConnected() && mChannel != null && mChannel.isConnected())
		{
			//Logger.LogInfo("No need for new OpenSFTP connection @ " + getName());
			return;
		}
		super.connect();
		//Logger.LogDebug("Attempting to connect to OpenSFTP " + getName());
		//disconnect();
		//Logger.LogDebug("Ready for new connection");
		//JSch jsch = new JSch();
		//try {
		if(mSession == null || !mSession.isConnected())
		{
			mSession = DefaultJSch.getSession(mUser, mHost, getPort());
			if(mUserInfo != null)
				mSession.setUserInfo(mUserInfo);
			else
				Logger.LogWarning("No User Info!");
			mSession.setTimeout(Timeout);
			Logger.LogDebug("Connecting session...");
			mSession.connect();
		}
		Logger.LogDebug("Session achieved. Opening Channel...");
		//String command = "scp -f " + mRemotePath;
		mChannel = (ChannelSftp)mSession.openChannel("sftp");
		//((ChannelSftp)mChannel);
		
		mChannel.connect();
		
		Logger.LogDebug("Channel open! Ready for action!");
		//} catch (JSchException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
	}
	
	@Override
	public boolean isConnected() throws IOException {
		if(mSession == null) return false;
		return mSession.isConnected();
	}

	private InputStream getMyInputStream() throws IOException {
		if(in != null)
			return in;

		try {
			connect();
			
			out = mChannel.getOutputStream();
			in = mChannel.getInputStream();
			
			byte[] buf = new byte[1024];
	
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
		      
			while(true) {
				int c = checkAck(in);
				if(c != 'C') break;
			}
			
			// read '0644 '
			in.read(buf, 0, 5);
			
			filesize = 0l;
			while(true)
			{
				if(in.read(buf,0,1) < 0) break;
				if(buf[0] == ' ') break;
				filesize = filesize * 10l + (long)(buf[0]-'0');
			}
			
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
			
		} catch (JSchException e) {
			throw new IOException("JSchException while trying to get SCP file (" + mRemotePath + ")");
		}
		return in;
	}

	private OutputStream getMyOutputStream() throws IOException {
		if(out != null)
			return out;

		try {
			connect();
			
			out = mChannel.getOutputStream();
			in = mChannel.getInputStream();
			
			if(checkAck(in) != 0) throw new IOException("No ack on getOutputStream");
			
		} catch (JSchException e) {
			throw new IOException("JSchException while trying to get SCP file (" + mRemotePath + ")");
		}
		return out;
	}
	
	static int checkAck(InputStream in) throws IOException
	{
		int b=in.read();
		if(b==0) return b;
		if(b==-1) return b;
		
		if(b==1 || b==2){
			StringBuffer sb=new StringBuffer();
			int c;
			do {c=in.read();sb.append((char)c);}while(c!='\n');
			if(b==1){System.out.print(sb.toString());}
			if(b==2){System.out.print(sb.toString());}
		}
		return b;
	}
	@Override
	public void clearChildren() {
		mChildren = null;
	}
	@Override
	public boolean copyFrom(OpenFile f, NetworkListener l) {
		try {
			mChannel.put(f.getPath(), getUri().getPath());
			l.OnNetworkCopyFinished(this, f);
			return true;
		} catch(Exception e){
			l.OnNetworkFailure(this, f, e);
		}
		return false;
	}
	@Override
	public boolean copyTo(OpenFile f, NetworkListener l) {
		try {
			mChannel.get(getUri().getPath(), f.getPath());
			l.OnNetworkCopyFinished(this, f);
			return true;
		} catch (SftpException e) {
			l.OnNetworkFailure(this, f, e);
		}
		return false;
	}
}
