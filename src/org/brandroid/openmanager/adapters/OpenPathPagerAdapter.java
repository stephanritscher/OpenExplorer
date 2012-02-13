package org.brandroid.openmanager.adapters;

import java.util.ArrayList;
import java.util.List;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.utils.Logger;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

public class OpenPathPagerAdapter extends FragmentStatePagerAdapter
{
	private OpenPath mPath = new OpenFile("/mnt/sdcard");
	private Fragment mFirst = null;
	//private List<OpenPath> mChildren = new ArrayList<OpenPath>();

	public OpenPathPagerAdapter(FragmentManager fm) {
		super(fm);
	}
	
	public OpenPathPagerAdapter setPath(OpenPath path) {
		mPath = path;
		notifyDataSetChanged();
		return this;
	}
	
	public OpenPathPagerAdapter setFirstFragment(Fragment f)
	{
		mFirst = f;
		return this;
	}
	
	public Fragment getLastItem() { return getItem(getCount() - 1); }
	
	@Override
	public CharSequence getPageTitle(int position) {
		if(position == getCount() - 1)
			return mPath.getName();
		if(mFirst != null)
		{
			if(position == 0)
				return mFirst.getText(R.string.s_bookmarks);
			position++;
		}
		OpenPath tmp = mPath;
		for(int i=position; i<getCount(); i++)
		{
			if(tmp.getParent() != null)
				tmp = tmp.getParent();
			else break;
		}
		return tmp.getName();
	}

	@Override
	public Fragment getItem(int pos) {
		if(pos == getCount() - 1 && mPath.isTextFile())
		{
			Logger.LogVerbose("Getting TextEdtor Fragment.");
			return new TextEditorFragment(mPath);
		} else {
			if(mFirst != null)
			{
				if(pos == 0)
				{
					Logger.LogVerbose("Getting First");
					return mFirst;
				}
				pos++;
			}
			OpenPath tmp = mPath;
			for(int i=pos; i<getCount(); i++)
			{
				if(tmp.getParent() != null)
					tmp = tmp.getParent();
				else break;
			}
			Logger.LogVerbose("Getting Fragment for #" + pos + " - " + tmp.getPath());
			//return 
			return ContentFragment.getInstance(tmp);
		}
	}
	
	@Override
	public int getCount() {
		return mPath.getDepth() + (mFirst != null ? 1 : 0);
	}

}
