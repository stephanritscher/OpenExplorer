
package org.brandroid.openmanager.data;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;

public class OpenPathMerged extends OpenPath {
    private ArrayList<OpenPath> mParents = new ArrayList<OpenPath>();
    private ArrayList<OpenPath> mKids = new ArrayList<OpenPath>();
    private boolean mDirty = false;
    private String mName = null;

    public OpenPathMerged(String name) {
        mName = name;
    }

    public void addParent(OpenPath parent) {
        mParents.add(parent);
        mDirty = true;
    }

    public void refreshKids() throws IOException {
        for (OpenPath parent : mParents)
            for (OpenPath kid : parent.list())
                if (!mKids.contains(kid))
                    mKids.add(kid);
        mDirty = false;
    }

    @Override
    public boolean isLoaded() {
        return !mDirty;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getPath() {
        return mName;
    }

    @Override
    public String getAbsolutePath() {
        return mName;
    }

    @Override
    public long length() {
        return mKids.size();
    }

    @Override
    public int getListLength() {
        return (int)length();
    }

    @Override
    public OpenPath getParent() {
        return null;
    }

    @Override
    public OpenPath getChild(String name) {
        for (OpenPath kid : mKids)
            if (kid.getName().equals(name))
                return kid;
        return null;
    }

    @Override
    public OpenPath[] list() throws IOException {
        if (mDirty)
            return listFiles();
        return mKids.toArray(new OpenPath[mKids.size()]);
    }

    @Override
    public OpenPath[] listFiles() throws IOException {
        refreshKids();
        return list();
    }

    @Override
    public Boolean isDirectory() {
        return true;
    }

    @Override
    public Boolean isFile() {
        return false;
    }

    @Override
    public Boolean isHidden() {
        return false;
    }

    @Override
    public Uri getUri() {
        String s = "content://org.brandroid.openmanager/merge/";
        for (OpenPath p : mParents)
            s += Uri.encode(p.getPath()) + ":";
        s = s.substring(0, s.length() - 1);
        Uri ret = null;
        ret = Uri.parse(s);
        return ret;
    }

    @Override
    public Long lastModified() {
        long last = 0;
        for (OpenPath kid : mKids)
            last = Math.max(kid.lastModified(), last);
        return null;
    }

    @Override
    public Boolean canRead() {
        return true;
    }

    @Override
    public Boolean canWrite() {
        return mParents.get(0).canWrite();
    }

    @Override
    public Boolean canExecute() {
        return false;
    }

    @Override
    public Boolean exists() {
        return true;
    }

    @Override
    public Boolean requiresThread() {
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

    public void setName(String string) {
        mName = string;
    }

}
