/*
	Open Explorer, an open source file explorer & text editor
	Copyright (C) 2013 Brandon Bowles <brandroid64@gmail.com>

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

package org.brandroid.openmanager.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.Time;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.Gravity;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.ImageCacheService;
import com.android.gallery3d.util.ThreadPool;
import com.jcraft.jsch.JSchException;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;
import com.viewpagerindicator.TabPageIndicator;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter.OnPageTitleClickListener;
import org.brandroid.openmanager.adapters.IconContextMenu;
import org.brandroid.openmanager.adapters.IconContextMenu.IconContextItemSelectedListener;
import org.brandroid.openmanager.adapters.IconContextMenuAdapter;
import org.brandroid.openmanager.adapters.OpenBookmarks;
import org.brandroid.openmanager.adapters.OpenBookmarks.OnBookmarkSelectListener;
import org.brandroid.openmanager.adapters.OpenClipboard;
import org.brandroid.openmanager.adapters.OpenClipboard.OnClipboardUpdateListener;
import org.brandroid.openmanager.adapters.OpenPathDbAdapter;
import org.brandroid.openmanager.data.OpenCursor;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenMediaStore;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenNetworkPath.PipeNeeded;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenPathArray;
import org.brandroid.openmanager.data.OpenPathMerged;
import org.brandroid.openmanager.data.OpenSFTP;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.data.OpenSmartFolder;
import org.brandroid.openmanager.data.OpenSmartFolder.SmartSearch;
import org.brandroid.openmanager.data.OpenURL;
import org.brandroid.openmanager.fragments.ContentFragment;
import org.brandroid.openmanager.fragments.DialogHandler;
import org.brandroid.openmanager.fragments.LogViewerFragment;
import org.brandroid.openmanager.fragments.OpenFragment;
import org.brandroid.openmanager.fragments.OpenFragment.OnFragmentDPADListener;
import org.brandroid.openmanager.fragments.OpenFragment.OnFragmentTitleLongClickListener;
import org.brandroid.openmanager.fragments.OpenFragment.Poppable;
import org.brandroid.openmanager.fragments.OpenPathFragmentInterface;
import org.brandroid.openmanager.fragments.OperationsFragment;
import org.brandroid.openmanager.fragments.SearchResultsFragment;
import org.brandroid.openmanager.fragments.TextEditorFragment;
import org.brandroid.openmanager.interfaces.OpenApp;
import org.brandroid.openmanager.util.BetterPopupWindow;
import org.brandroid.openmanager.util.EventHandler;
import org.brandroid.openmanager.util.EventHandler.BackgroundWork;
import org.brandroid.openmanager.util.EventHandler.EventType;
import org.brandroid.openmanager.util.EventHandler.OnWorkerUpdateListener;
import org.brandroid.openmanager.util.FileManager;
import org.brandroid.openmanager.util.IntentManager;
import org.brandroid.openmanager.util.MimeTypeParser;
import org.brandroid.openmanager.util.MimeTypes;
import org.brandroid.openmanager.util.PrivatePreferences;
import org.brandroid.openmanager.util.RootManager;
import org.brandroid.openmanager.util.ShellSession;
import org.brandroid.openmanager.util.SimpleHostKeyRepo;
import org.brandroid.openmanager.util.SimpleUserInfo;
import org.brandroid.openmanager.util.SimpleUserInfo.UserInfoInteractionCallback;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.util.ThumbnailCreator.OnUpdateImageListener;
import org.brandroid.openmanager.views.OpenPathList;
import org.brandroid.openmanager.views.OpenViewPager;
import org.brandroid.utils.CustomExceptionHandler;
import org.brandroid.utils.DiskLruCache;
import org.brandroid.utils.ImageUtils;
import org.brandroid.utils.Logger;
import org.brandroid.utils.LruCache;
import org.brandroid.utils.MenuUtils;
import org.brandroid.utils.Preferences;
import org.brandroid.utils.SubmitStatsTask;
import org.brandroid.utils.Utils;
import org.brandroid.utils.ViewUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

@SuppressLint({
        "NewApi", "DefaultLocale"
})
public class OpenExplorer extends OpenFragmentActivity implements OnBackStackChangedListener,
        OnClipboardUpdateListener, OnWorkerUpdateListener, OnPageTitleClickListener,
        LoaderCallbacks<Cursor>, OnPageChangeListener, OpenApp, IconContextItemSelectedListener,
        OnKeyListener, OnFragmentDPADListener, OnFocusChangeListener, OnBookmarkSelectListener {

    private MenuItem mMenuPaste;
    public static final int REQ_PREFERENCES = 6;
    public static final int REQ_SPLASH = 7;
    public static final int REQ_INTENT = 8;
    public static final int REQ_SAVE_FILE = 9;
    public static final int REQ_PICK_FOLDER = 10;
    public static final int REQUEST_VIEW = 11;
    public static final int REQ_SERVER_NEW = 12;
    public static final int REQ_SERVER_MODIFY = 13;
    public static final int RESULT_RESTART_NEEDED = 14;
    public static final int REQ_AUTHENTICATE_BOX = 15;
    public static final int REQ_AUTHENTICATE_DROPBOX = 16;
    public static final int REQ_AUTHENTICATE_DRIVE = 17;
    public static final int REQ_EVENT_CANCEL = 18;
    public static final int REQ_EVENT_VIEW = 19;
    public static final int VIEW_LIST = 0;
    public static final int VIEW_GRID = 1;
    public static final int VIEW_CAROUSEL = 2;
    
    public static final String INTENT_BROADCAST_ACTION = "INTENT_BROADCAST";

    public static final boolean BEFORE_HONEYCOMB = Build.VERSION.SDK_INT < 11;
    public static final boolean SDK_JELLYBEAN = Build.VERSION.SDK_INT > 15;
    public static boolean CAN_DO_CAROUSEL = false;
    public static boolean USE_ACTION_BAR = false;
    public static boolean USE_SPLIT_ACTION_BAR = true;
    public static boolean IS_DEBUG_BUILD = true;
    public static boolean LOW_MEMORY = false;
    public static final boolean SHOW_FILE_DETAILS = false;
    public static boolean USE_PRETTY_CONTEXT_MENUS = true;
    public static boolean IS_FULL_SCREEN = false;
    public static final boolean IS_KEYBOARD_AVAILABLE = false;

    // private final static boolean DEBUG = IS_DEBUG_BUILD && true;

    public static int SCREEN_WIDTH = -1;
    public static int SCREEN_HEIGHT = -1;
    public static int SCREEN_DPI = -1;
    public static int VERSION = 160;
    public static int COLUMN_WIDTH_GRID = 128;
    public static int COLUMN_WIDTH_LIST = 300;
    public static int IMAGE_SIZE_GRID = 36;
    public static int IMAGE_SIZE_LIST = 128;
    public static int TEXT_EDITOR_MAX_SIZE = 500000;
    public static float DP_RATIO = 1;

    public static SparseArray<MenuItem> mMenuShortcuts;

    private static MimeTypes mMimeTypes;
    private ActionMode mActionMode;
    private int mLastBackIndex = -1;
    private static long lastSubmit = 0l;
    private OpenPath mLastPath = null;
    private BroadcastReceiver storageReceiver = null;
    private static final Handler mHandler = new Handler(); // handler for the
                                                           // main thread
    // private int mViewMode = VIEW_LIST;
    // private static long mLastCursorEnsure = 0;
    private static boolean mRunningCursorEnsure = false;
    private Boolean mSinglePane = false;
    private Boolean mStateReady = true;
    private Boolean mTwoRowTitle = false;
    private String mLastMenuClass = "";
    private int mLastClipSize = -1;
    private boolean mLastClipState = false;
    public static boolean DEBUG_TOGGLE = false;
    // private ActionBarHelper mActionBarHelper = null;

    private static LogViewerFragment mLogFragment = null;
    private static OperationsFragment mOpsFragment = null;
    private static boolean mLogViewEnabled = true;
    private OpenViewPager mViewPager;
    private static ArrayPagerAdapter mViewPagerAdapter;

    private static final boolean mViewPagerEnabled = true;
    private View mBookmarksView;
    private OpenBookmarks mBookmarks;
    private BetterPopupWindow mBookmarksPopup;
    private static OpenApp.OnBookMarkChangeListener mBookmarkListener;
    private ViewGroup mToolbarButtons = null;
    private ViewGroup mStaticButtons = null;
    private static ActionBar mBar = null;
    private OpenClipboard mClipboard;

    private static boolean bRetrieveDimensionsForPhotos = Build.VERSION.SDK_INT >= 10;
    private static boolean bRetrieveExtraVideoDetails = Build.VERSION.SDK_INT > 8;
    private static boolean bRetrieveCursorFiles = Build.VERSION.SDK_INT > 10;

    private static final FileManager mFileManager = new FileManager();
    private static final EventHandler mEvHandler = new EventHandler(mFileManager);

    public FragmentManager fragmentManager;

    private final static OpenCursor mPhotoParent = new OpenCursor("Photos",
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    private final static OpenCursor mVideoParent = new OpenCursor("Videos",
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    private final static OpenCursor mMusicParent = new OpenCursor("Music",
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
    private final static OpenCursor mApkParent = new OpenCursor("Apps",
            BEFORE_HONEYCOMB ? Uri.fromFile(OpenFile.getExternalMemoryDrive(true).getFile())
                    : MediaStore.Files.getContentUri("/mnt"));
    private final static OpenSmartFolder mVideoSearchParent = new OpenSmartFolder("Videos"),
            mPhotoSearchParent = new OpenSmartFolder("Photos"),
            mDownloadParent = new OpenSmartFolder("Downloads");
    private final static OpenPathMerged mVideosMerged = new OpenPathMerged("Videos"),
            mPhotosMerged = new OpenPathMerged("Photos");

    public boolean isViewPagerEnabled() {
        return mViewPagerEnabled;
    }

    private void loadPreferences() {
        Preferences prefs = getPreferences();

        // mViewPagerEnabled = prefs.getBoolean("global", "pref_pagers", true);
        // USE_ACTIONMODE = getPreferences().getBoolean("global",
        // "pref_actionmode", false);

        Preferences.Pref_Intents_Internal = prefs
                .getBoolean("global", "pref_intent_internal", true);
        Preferences.Pref_Text_Internal = prefs.getBoolean("global", "pref_text_internal", true);
        Preferences.Pref_Zip_Internal = prefs.getBoolean("global", "pref_zip_internal", true);
        Preferences.Pref_ShowUp = prefs.getBoolean("global", "pref_showup", false);
        Preferences.Pref_ShowThumbs = prefs.getBoolean("global", "pref_thumbs", true);
        Preferences.Pref_CacheThumbs = prefs.getBoolean("global", "pref_thumbs_cache", false);
        Preferences.Pref_Language = prefs.getString("global", "pref_language", "");
        Preferences.Pref_Analytics = prefs.getBoolean("global", "pref_stats", false);
        Preferences.Pref_Text_Max_Size = prefs.getInt("global", "text_max", 500000);
        Preferences.Pref_Root = prefs.getBoolean("global", "pref_root", Preferences.Pref_Root);
        ThumbnailCreator.showCenteredCroppedPreviews = prefs.getBoolean("global",
                "prefs_thumbs_crop", false);
        Preferences.Run_Count = prefs.getInt("stats", "runs", Preferences.Run_Count) + 1;
        prefs.setSetting("stats", "runs", Preferences.Run_Count);
        Preferences.UID = prefs.getString("stats", "uid", Preferences.UID);
        for(String cloud : new String[]{"box","dropbox","drive"})
        {
            if(!prefs.getSetting("global", "pref_cloud_" + cloud + "_enabled", true)) continue;
            String key = prefs.getSetting("global", "pref_cloud_" + cloud + "_key", (String)null);
            String secret = prefs.getSetting("global", "pref_cloud_" + cloud + "_secret", (String)null);
            if(key != null && !key.equals("") && secret != null && !secret.equals(""))
            {
                PrivatePreferences.putKey(cloud + "_key", key);
                PrivatePreferences.putKey(cloud + "_secret", secret);
            }
        }
        if (Preferences.UID == null) {
            Preferences.UID = UUID.randomUUID().toString();
            prefs.setSetting("stats", "uid", Preferences.UID);
        }
        lastSubmit = new Date().getTime();

        PackageInfo pi = null;
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
        }

        VERSION = pi.versionCode;

        if (!Preferences.Pref_Language.equals(""))
            setLanguage(getContext(), Preferences.Pref_Language);

        // USE_PRETTY_MENUS = prefs.getBoolean("global", "pref_fancy_menus",
        // Build.VERSION.SDK_INT < 14);
        USE_PRETTY_CONTEXT_MENUS = prefs.getBoolean("global", "pref_fancy_context", true);

        String s = prefs.getString("global", "pref_location_ext", null);
        if (s != null && new OpenFile(s).exists())
            OpenFile.setExternalMemoryDrive(new OpenFile(s));
        else
            prefs.setSetting("global", "pref_location_ext", OpenFile.getExternalMemoryDrive(true)
                    .getPath());

        s = prefs.getString("global", "pref_location_int", null);
        if (s != null && new OpenFile(s).exists())
            OpenFile.setInternalMemoryDrive(new OpenFile(s));
        else
            prefs.setSetting("global", "pref_location_int", OpenFile.getInternalMemoryDrive()
                    .getPath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("OpenExplorer.onResume");
        handleIntent(getIntent());
        onClipboardUpdate();
    }

    public void onCreate(Bundle savedInstanceState) {
        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("OpenExplorer.onCreate");

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

        if (getPreferences().getBoolean("global", "pref_fullscreen", false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            IS_FULL_SCREEN = true;
        } // else getWindow().addFlags(WindowManager.LayoutParams.FLAG
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            IS_FULL_SCREEN = false;
        }

        // IS_KEYBOARD_AVAILABLE =
        // getContext().getResources().getConfiguration().keyboard ==
        // Configuration.KEYBOARD_QWERTY;

        loadPreferences();
        checkRoot();

        int theme = getThemeId();
        boolean themeDark = R.style.AppTheme_Dark == theme ||
                R.style.AppTheme_LightAndDark == theme;

        getApplicationContext().setTheme(theme);
        setTheme(theme);
        getOpenApplication().loadThemedAssets(this);

        Resources res = getResources();
        if (res != null) {
            COLUMN_WIDTH_GRID = res.getDimensionPixelSize(R.dimen.grid_width);
            COLUMN_WIDTH_LIST = res.getDimensionPixelSize(R.dimen.list_width);
            DP_RATIO = res.getDimension(R.dimen.one_dp);
            IMAGE_SIZE_GRID = res.getInteger(R.integer.content_grid_image_size);
            IMAGE_SIZE_LIST = res.getInteger(R.integer.content_list_image_size);
            TEXT_EDITOR_MAX_SIZE = res.getInteger(R.integer.max_text_editor_size);
        }

        if (getPreferences().getBoolean("global", "pref_hardware_accel", false)
                && !BEFORE_HONEYCOMB)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        USE_ACTION_BAR = true;

        mBar = getSupportActionBar();

        if (findViewById(R.id.content_pager_indicator_frame) != null) {
            mTwoRowTitle = true;
            setTitle(R.string.app_name);
        }

        if (mBar != null) {
            if (Build.VERSION.SDK_INT >= 14)
                mBar.setHomeButtonEnabled(true);
            mBar.setDisplayUseLogoEnabled(true);
            // if(!mTwoRowTitle)
            try {
                // mBar.setDisplayOptions(ActionBar.NAVIGATION_MODE_TABS);
                mBar.setCustomView(R.layout.title_bar);
                mBar.setDisplayShowCustomEnabled(true);
                mBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                // ViewGroup cv = (ViewGroup)ab.getCustomView();
                // if(cv.findViewById(R.id.title_paste) != null)
                // cv.removeView(cv.findViewById(R.id.title_paste));
                // ab.getCustomView().findViewById(R.id.title_icon).setVisibility(View.GONE);
            } catch (InflateException e) {
                Logger.LogWarning("Couldn't set up ActionBar custom view", e);
            }
            if (mTwoRowTitle)
                setTitle(R.string.app_name);
        } else
            USE_ACTION_BAR = false;

        OpenFile.setTempFileRoot(new OpenFile(getFilesDir()).getChild("temp"));
        setupLogviewHandlers();
        //handleExceptionHandler();
        getMimeTypes();
        setupFilesDb();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_fragments);
        handleNfc();
        if (Build.VERSION.SDK_INT < 11)
            getWindow().setBackgroundDrawableResource(
                    themeDark ? R.drawable.background_holo_dark : R.drawable.background_holo_light);

        try {
            upgradeViewSettings();
        } catch (Exception e) {
        }
        // try {
        showWarnings();
        // } catch(Exception e) { }

        mEvHandler.setUpdateListener(this);

        getClipboard().setClipboardUpdateListener(this);

        try {
            /*
             * Signature[] sigs =
             * getPackageManager().getPackageInfo(getPackageName(),
             * PackageManager.GET_SIGNATURES).signatures; for(Signature sig :
             * sigs) if(sig.toCharsString().indexOf("4465627567") > -1) // check
             * for "Debug" in signature IS_DEBUG_BUILD = true;
             */
            if (IS_DEBUG_BUILD)
                IS_DEBUG_BUILD = (getPackageManager().getActivityInfo(getComponentName(),
                        PackageManager.GET_META_DATA).applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE;
            if (isBlackBerry())
                IS_DEBUG_BUILD = false;
            if(RootTools.debugMode)
                RootTools.debugMode = IS_DEBUG_BUILD;
        } catch (NameNotFoundException e1) {
        }

        // handleNetworking();
        refreshCursors();

        checkWelcome();

        setViewVisibility(false, false, R.id.title_paste, R.id.title_ops, R.id.title_log);
        setOnClicks(R.id.title_ops, // R.id.menu_global_ops_icon,
                                    // R.id.menu_global_ops_text,
                R.id.title_log, R.id.title_icon_holder, R.id.title_paste_icon
        // ,R.id.title_sort, R.id.title_view, R.id.title_up
        );
        checkTitleSeparator();
        // IconContextMenu.clearInstances();

        if (findViewById(R.id.list_frag) == null)
            mSinglePane = true;

        setViewVisibility(
                getSetting(null, "pref_show_bookmarks", getResources().getBoolean(R.bool.large)),
                false, R.id.list_frag);

        Logger.LogDebug("Looking for path");
        OpenPath path = mLastPath;
        if (savedInstanceState == null || path == null) {
            String start = getPreferences().getString("global", "pref_start", "External");

            if (savedInstanceState != null && savedInstanceState.containsKey("last")
                    && !savedInstanceState.getString("last").equals(""))
                start = savedInstanceState.getString("last");

            path = FileManager.getOpenCache(start, this);
        }

        if (path == null)
            path = OpenFile.getExternalMemoryDrive(true);

        if (FileManager.checkForNoMedia(path))
            showToast(R.string.s_error_no_media, Toast.LENGTH_LONG);

        mLastPath = path;

        boolean bAddToStack = true;

        if (findViewById(R.id.content_pager_frame_stub) != null)
            ((ViewStub)findViewById(R.id.content_pager_frame_stub)).inflate();

        Logger.LogDebug("Pager inflated");

        if (fragmentManager == null) {
            fragmentManager = getSupportFragmentManager();
            fragmentManager.addOnBackStackChangedListener(this);
        }

        if(mLogFragment == null)
        	mLogFragment = new LogViewerFragment();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        Logger.LogDebug("Creating with " + path.getPath());
        if (path instanceof OpenFile)
            EventHandler.execute(new PeekAtGrandKidsTask(), path);

        initPager();

        if (mViewPager != null && mViewPagerAdapter != null && path != null) {
            // mViewPagerAdapter.add(mContentFragment);
            mLastPath = null;
            changePath(path, bAddToStack, true);
            setCurrentItem(mViewPagerAdapter.getCount() - 1, false);
            restoreOpenedEditors();
        } else
            Logger.LogWarning("Nothing to show?!");

        ft.commit();

        // invalidateOptionsMenu();
        initBookmarkDropdown();

        // handleMediaReceiver();

        // if(!getPreferences().getBoolean("global", "pref_splash", false))
        // showSplashIntent(this, getPreferences().getString("global",
        // "pref_start", "Internal"));
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getText(titleId));
    }

    @Override
    public void setTitle(CharSequence title) {
        TextView tb = (TextView)findViewById(R.id.title_title);
        if (tb == null && mBar != null && mBar.getCustomView() != null)
            tb = (TextView)mBar.getCustomView().findViewById(R.id.title_title);
        if (tb != null) {
            if (!tb.isShown())
                tb.setVisibility(View.VISIBLE);
            tb.setText(title);
        } else if (mBar != null)
            mBar.setTitle(title);
        else
            super.setTitle(title);
    }

    private void showDonateDialog(int resMessage, String sTitle, final String pref) {
        if (getPreferences().getBoolean("warn", pref, false))
            return;
        int[] opts = new int[] {
                R.string.s_menu_donate, R.string.s_no, R.string.s_menu_rate
        };
        if (Build.VERSION.SDK_INT > 13)
            opts = new int[] {
                    R.string.s_menu_rate, R.string.s_no, R.string.s_menu_donate
            };
        DialogHandler.showMultiButtonDialog(this, getString(resMessage), sTitle,
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case R.string.s_no:
                                getPreferences().setSetting("warn", pref, true);
                                break;
                            case R.string.s_cancel:
                                break;
                            case R.string.s_menu_rate:
                                getPreferences().setSetting("warn", pref, true);
                                launchReviews();
                                break;
                            default:
                                getPreferences().setSetting("warn", pref, true);
                                launchUri(OpenExplorer.this,
                                        Uri.parse("http://brandroid.org/donate.php?ref=app"));
                                break;
                        }
                        if (dialog != null)
                            dialog.dismiss();
                    }
                }, opts);
    }

    public void launchReviews() {
        if (isNook()) {
            Intent iRate = new Intent("com.bn.sdk.shop.details");
            iRate.putExtra("product_details_ean", "2940043894236");
            if (IntentManager.getResolveInfo(iRate, this) != null)
                try {
                    startActivity(iRate);
                    return;
                } catch (Exception e) {
                    Logger.LogWarning("Unable to launch Nook reviews!");
                }
            launchUri(
                    this,
                    Uri.parse("http://www.barnesandnoble.com/reviews/OpenExplorer%2Fbrandroidorg/1111331126"));
            return;
        }
        else if (isBlackBerry())
            launchUri(this,
                    Uri.parse("http://appworld.blackberry.com/webstore/content/85146/"));
        else
            launchUri(
                    this,
                    Uri.parse("https://play.google.com/store/apps/details?id=org.brandroid.openmanager&reviewId=0"));
    }

    private void checkWelcome() {
        if (Preferences.Run_Count > 5) {
            if (isNook())
                showDonateDialog(R.string.msg_nook_donate2, "Hello Nook User!", "nook_donate2");
            else if (isBlackBerry())
                showDonateDialog(R.string.msg_bb_donate, "Hello Blackberry User!", "rim_donate");
            else
                showDonateDialog(R.string.msg_donate, null, "donate");
        } else if (Preferences.Run_Count < 5)
            checkGTV();
    }

    private void checkGTV() {
        try {
            if (isGTV()) {
                // IS_DEBUG_BUILD = false;
                // CAN_DO_CAROUSEL = true;
                if (!getPreferences().getBoolean("global", "welcome", false)) {
                    showToast("Welcome, GoogleTV user!");
                    getPreferences().setSetting("global", "welcome", true);
                }
            }
        } catch (Exception e) {
            Logger.LogWarning("Couldn't check for GTV", e);
        }
    }

    private void checkRoot() {
        try {
            if (Preferences.Pref_Root) {
                // && (RootManager.Default == null ||
                // !RootManager.Default.isRoot()))
                requestRoot();
            } else { // if(RootManager.Default != null)
                exitRoot();
            }
        } catch (Exception e) {
            Logger.LogWarning("Couldn't get root.", e);
        }
    }

    private void requestRoot() {
        if(!Preferences.Pref_Root) return;
        showToast("Root preference selected, requesting now.");
        new Thread(new Runnable() {
            public void run() {
                Preferences.Pref_Root = false;
                if (RootTools.isAccessGiven())
                    try {
                        RootTools.getShell(true);
                        Preferences.Pref_Root = true;
                        checkBusybox();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (RootDeniedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }
        }).start();
    }
    
    private boolean checkBusybox()
    {
    	if(RootTools.checkUtil("busybox")) return true;
    	post(new Runnable() {
			public void run() {
		    	DialogHandler.showConfirmationDialog(OpenExplorer.this,
		    			"It appears that Busybox is not installed on your system. Would you like to install it?",
		    			"Busybox Check", getPreferences(), "pref_busybox", false,
		    			new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							    if(which != DialogInterface.BUTTON_POSITIVE)
							        return;
							    if(dialog != null)
							        dialog.dismiss();
								new Thread(new Runnable() {
									public void run() {
										installBusybox();
									}
								}).start();
							}
						});
			}
		});
    	return false;
    }
    
    private void installBusybox()
    {
    	try {
    		String cpuinfo = new OpenFile("/proc/cpuinfo").readAscii();
    		for(String arc : new String[] {"v7l", "v6l", "v5l", "v4tl", "v4l", "i686", "i586", "i486", "mips", "x86_64", "powerpc"})
    			if(cpuinfo.indexOf(arc) > -1)
    				if(installBusybox((arc.startsWith("v") ? "arm" : "") + arc))
    					return;
    		RootTools.getShell(false, 500).add(new Command(0, 500, "uname -m") {
				public void output(int id, String arch) {
					if(arch == null || arch.equals("")) return;
					if(arch.indexOf(" ") > -1) return;
					installBusybox(arch);
				}
			});
    	} catch(IOException e) {
    		e.printStackTrace();
    	} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RootDeniedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private boolean installBusybox(String arch)
    {
    	final String mBusyboxUrl = "http://busybox.net/downloads/binaries/latest/";
    	final String mBusyboxFile = "busybox-" + arch;
    	final String url = mBusyboxUrl + "busybox-" + arch;
    	if(IS_DEBUG_BUILD)
    	    Logger.LogVerbose("Checking for busybox for " + arch + ": " + url);
		OpenFile dl = new OpenFile("/mnt/sdcard");
		if(!dl.exists() || !dl.canWrite())
		    dl = OpenFile.getExternalMemoryDrive(true);
		if(dl != null && dl.getChild("Download") != null && dl.getChild("Download").canWrite())
			dl = dl.getChild("Download");
//		dl = dl.getChild(".busybox");
//		if(dl.exists())
//		    dl.delete();
//		try {
//			dl.create(); // Make sure we can write to it
//			if(!dl.exists()) throw new IOException("WTF");
//			dl.delete();
//		} catch (IOException e) {
//			Logger.LogError("Unable to create temporary busybox", e);
//			return false;
//		}
		final OpenFile dlp = dl;
		final OpenURL u = new OpenURL(url);
        if(!u.exists())
        {
        	showToast("Unable to download busybox for [" + arch + "]");
        	return false;
        } else
        	post(new Runnable() {
				public void run() {
					EventHandler eh = new EventHandler(getFileManager());
					BackgroundWork bw = eh.getWorker(EventType.COPY, OpenExplorer.this, dlp);
					eh.setUpdateListener(new OnWorkerUpdateListener() {
						
						@Override
						public void onWorkerThreadFailure(EventType type, OpenPath... files) {
							Logger.LogError("Busybox installation failed!");
						}
						
						@Override
						public void onWorkerThreadComplete(EventType type, String... results) {
							for(String s : results)
								Logger.LogDebug("BusyBox.onWorkerThreadComplete(" + s + ")");
							if(installBusybox(dlp.getChild(mBusyboxFile)))
								showToast("Busybox installed successfully!");
							else
							    showToast("Unable to install Busybox");
						}
						
						@Override
						public void onWorkerProgressUpdate(int pos, int total) {
							// TODO Auto-generated method stub
							
						}
					});
					bw.execute(u);
				}
        	});
        return true;
    }
    
    private boolean installBusybox(OpenFile tmp)
    {
    	boolean success = false;
    	if(!RootManager.isSystemMounted())
    	    RootTools.remount("/system", "rw");
    	try {
            Command cmd = new CommandCapture(0,
                    "cp " + tmp.getAbsolutePath() + " /system/xbin/busybox",
                    "chmod 755 /system/xbin/busybox"
                    );
    	    Shell.runRootCommand(cmd);
    	    cmd.waitForFinish();
    	    final List<String> missings = new ArrayList<String>();
    	    cmd = new Command(1, "busybox --list") {
                public void output(int id, String util) {
                    if(new File("/system/xbin/" + util).exists() ||
                       new File("/system/bin/" + util).exists())
                        Logger.LogWarning(util + " already exists!");
                    else
                    {
                        missings.add("ln -s /system/xbin/busybox /system/xbin/" + util);
                        Logger.LogVerbose("Need to link to busybox: " + util);
                    }
                }
            };
            Shell.runCommand(cmd);
            cmd.waitForFinish();
            if(missings.size() > 0)
            {
                for(int i = 0; i < missings.size(); i++)
                {
                    cmd = new CommandCapture(2 + i, missings.get(i));
                    Shell.runRootCommand(cmd);
                    cmd.waitForFinish();
                }
            }
    	    tmp.delete();
            success = RootTools.checkUtil("busybox");
        } catch (Exception e) {
            Logger.LogError("Couldn't finish Busybox install", e);
        }
    	RootTools.remount("/system", "ro");
    	return success;
    }

    private void exitRoot() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    RootTools.closeAllShells();
                } catch (IOException e) {
                    Logger.LogWarning("An exception occurred while closing root shells.", e);
                }
            }
        }).start();
    }

    private void handleNetworking() {
        FileManager.DefaultUserInfo = new SimpleUserInfo();
        final Context c = this;
        Preferences.Warn_Networking = getPreferences().getSetting("warn", "networking", false);
        SimpleUserInfo.setInteractionCallback(new UserInfoInteractionCallback() {

            public boolean promptPassword(String message) {
                View view = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.prompt_password, null);
                TextView tv = (TextView)view.findViewById(android.R.id.message);
                tv.setText(message);
                final EditText text1 = ((EditText)view.findViewById(android.R.id.text1));
                final CheckBox checkpw = (CheckBox)view.findViewById(android.R.id.checkbox);
                checkpw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            text1.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            text1.setTransformationMethod(new SingleLineTransformationMethod());
                        } else {
                            text1.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            text1.setTransformationMethod(new PasswordTransformationMethod());
                        }
                    }
                });

                final AlertDialog.Builder dlg = new AlertDialog.Builder(c)
                        .setTitle(R.string.s_prompt_password)
                        .setView(view)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String mPassword = text1.getText().toString();
                                        onPasswordEntered(mPassword);
                                        onYesNoAnswered(true);
                                    }
                                }).setNegativeButton(android.R.string.no, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onYesNoAnswered(false);
                            }
                        });

                OpenExplorer.getHandler().post(new Runnable() {
                    public void run() {
                        dlg.create().show();
                    }
                });
                return true;
            }

            @Override
            public void onYesNoAnswered(boolean yes) {
            }

            @Override
            public void onPasswordEntered(String password) {
                try {
                    OpenPath path = getDirContentFragment(false).getPath();
                    if (path instanceof OpenNetworkPath)
                    {
                        ((OpenNetworkPath)path).getServer().setPassword(password);
                        ServerSetupActivity.SaveToDefaultServers(OpenServers.getDefaultServers(), c);
                    }
                } catch (Exception e) {
                }
                getDirContentFragment(true).refreshData();
            }

            @Override
            public boolean promptYesNo(final String message) {
                OpenExplorer.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog dlg = new AlertDialog.Builder(c).setMessage(message)
                                .setPositiveButton(android.R.string.yes, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onYesNoAnswered(true);
                                    }
                                }).setNegativeButton(android.R.string.no, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onYesNoAnswered(false);
                                    }
                                }).create();
                        dlg.show();
                    }
                });
                return true;
            }
        });
        OpenNetworkPath.Timeout = getPreferences().getSetting("global", "server_timeout", 20) * 1000;
        try {
            OpenSFTP.DefaultJSch.setHostKeyRepository(new SimpleHostKeyRepo(OpenSFTP.DefaultJSch,
                    FileManager.DefaultUserInfo, Preferences.getPreferences(
                            getApplicationContext(), "hosts")));
        } catch (JSchException e) {
            Logger.LogWarning("Couldn't set Preference-backed Host Key Repository", e);
        }
    }

    private MimeTypes getMimeTypes() {
        return getMimeTypes(this);
    }

    public static MimeTypes getMimeTypes(Context c) {
        if (mMimeTypes != null)
            return mMimeTypes;
        if (MimeTypes.Default != null)
            return MimeTypes.Default;
        MimeTypeParser mtp = null;
        try {
            mtp = new MimeTypeParser(c, c.getPackageName());
        } catch (NameNotFoundException e) {
            // Should never happen
        }

        XmlResourceParser in = c.getResources().getXml(R.xml.mimetypes);

        try {
            mMimeTypes = mtp.fromXmlResource(in);
        } catch (XmlPullParserException e) {
            Logger.LogError("Couldn't parse mimeTypes.", e);
            throw new RuntimeException("PreselectedChannelsActivity: XmlPullParserException");
        } catch (IOException e) {
            Logger.LogError("PreselectedChannelsActivity: IOException", e);
            throw new RuntimeException("PreselectedChannelsActivity: IOException");
        }
        MimeTypes.Default = mMimeTypes;
        return mMimeTypes;
    }

    private void setCurrentItem(final int page, final boolean smooth) {
        try {
            // if(!Thread.currentThread().equals(UiThread))
            mViewPager.post(new Runnable() {
                public void run() {
                    try {
                        if (mViewPager.getCurrentItem() != page)
                            mViewPager.setCurrentItem(page, smooth);
                        else
                            Logger.LogDebug("Current page already set to " + page);
                    } catch (Exception e) {
                        Logger.LogError("Unable to set page to " + page, e);
                    }
                }
            });
            // else if(mViewPager.getCurrentItem() != page)
            // mViewPager.setCurrentItem(page, smooth);
        } catch (Exception e) {
            Logger.LogError("Couldn't set ViewPager page to " + page, e);
        }
    }

    private void handleExceptionHandler() {
        if (Logger.checkWTF()) {
            OpenFile crashFile = Logger.getCrashFile();
            if (crashFile.exists()) {
                try {
                    Logger.LogWTF(crashFile.readAscii(), new Exception());
                    crashFile.delete();
                } catch (Exception e) {
                }
            }
            // if(!getSetting(null, "pref_autowtf", false))
            // showWTFIntent();
            // else if(isNetworkConnected())
            // new SubmitStatsTask(this).execute(
            // Logger.getCrashReport(true));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Logger.LogDebug("New Intent! " + intent.toString());
        setIntent(intent);
        handleIntent(intent); // Unneeded, onResume will handle
    }

    public boolean showMenu(int menuId, final View from, final boolean fromTouch) {
        if (!BEFORE_HONEYCOMB && MenuUtils.getMenuLookupSub(menuId) > -1)
            return false;
        // if(mMenuPopup == null)

        if (from != null
                && !(from instanceof CheckedTextView)
                && showIContextMenu(menuId, from instanceof CheckedTextView ? null : from,
                        fromTouch) != null)
            return true;
        if (from.showContextMenu())
            return true;
        // if(IS_DEBUG_BUILD && BEFORE_HONEYCOMB)
        // showToast("Invalid option (0x" + Integer.toHexString(menuId) + ")" +
        // (from != null ? " under " + from.toString() + " (" + from.getLeft() +
        // "," + from.getTop() + ")" : ""));
        return false;
    }

    /*
     * This should only be used with the "main" menu
     */
    public boolean showMenu(final Menu menu, final View from, final boolean fromTouch) {
        if (from != null) {
            if (showIContextMenu(menu, from, fromTouch) == null) {
                from.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu cmenu, View v,
                            ContextMenuInfo menuInfo) {
                        // MenuUtils.transferMenu(menu, cmenu, false);
                    }
                });
                return from.showContextMenu();
            } else
                return true;
        } else
            openOptionsMenu();
        return false;
    }

    public IconContextMenu showIContextMenu(int menuId, final View from, final boolean fromTouch) {
        // if(menuId != R.menu.context_file && !OpenExplorer.USE_PRETTY_MENUS)
        // return null;
        if (menuId == R.menu.context_file && !OpenExplorer.USE_PRETTY_CONTEXT_MENUS)
            return null;
        try {
            if (Utils.getArrayIndex(MenuUtils.MENU_LOOKUP_IDS, menuId) > -1)
                menuId = MenuUtils.getMenuLookupSub(menuId);
            Logger.LogDebug("Trying to show context menu 0x"
                    + Integer.toHexString(menuId)
                    + (from != null ? " under " + from.toString() + " ("
                            + ViewUtils.getAbsoluteLeft(from) + ","
                            + ViewUtils.getAbsoluteTop(from) + ")" : "") + ".");
            if (Utils.getArrayIndex(MenuUtils.MENU_LOOKUP_SUBS, menuId) > -1) {
                // IconContextMenu icm1 = new
                // IconContextMenu(getApplicationContext(), menu, from, null,
                // null);
                // MenuBuilder menu = IconContextMenu.newMenu(this, menuId);
                IconContextMenu mOpenMenu = IconContextMenu.getInstance(this, menuId, from);
                onPrepareOptionsMenu(mOpenMenu.getMenu());
                mOpenMenu.setAnchor(from);
                if (menuId == R.menu.context_file) {
                    mOpenMenu.setNumColumns(2);
                    // icm.setPopupWidth(getResources().getDimensionPixelSize(R.dimen.popup_width)
                    // / 2);
                    mOpenMenu.setTextLayout(R.layout.context_item);
                } else
                    mOpenMenu.setNumColumns(1);
                mOpenMenu.setOnIconContextItemSelectedListener(this);
                /*
                 * if(menuId == R.menu.menu_sort || menuId ==
                 * R.menu.content_sort)
                 * mOpenMenu.setTitle(getString(R.string.s_menu_sort) + " (" +
                 * getDirContentFragment(false).getPath().getPath() + ")"); else
                 * if(menuId == R.menu.menu_view || menuId ==
                 * R.menu.content_view)
                 * mOpenMenu.setTitle(getString(R.string.s_view) + " (" +
                 * getDirContentFragment(false).getPath().getPath() + ")");
                 */
                mOpenMenu.show();
                return mOpenMenu;
            }
        } catch (Exception e) {
            Logger.LogWarning(
                    "Couldn't show icon context menu"
                            + (from != null ? " under " + from.toString() + " (" + from.getLeft()
                                    + "," + from.getTop() + ")" : "") + ".", e);
            if (from != null)
                return showIContextMenu(menuId, null, fromTouch);
        }
        Logger.LogWarning("Not sure what happend with "
                + menuId
                + (from != null ? " under " + from.toString() + " (" + from.getLeft() + ","
                        + from.getTop() + ")" : "") + ".");
        return null;
    }

    public IconContextMenu showIContextMenu(Menu menu, final View from, final boolean fromTouch) {
        boolean isContext = false;
        if (menu != null && menu.findItem(R.id.menu_context_zip) != null
                && menu.findItem(R.id.menu_context_zip).isVisible())
            isContext = true;
        return showIContextMenu(menu, from, fromTouch, isContext ? 2 : 1);
    }

    public IconContextMenu showIContextMenu(Menu menu, final View from, final boolean fromTouch,
            int cols) {
        if (menu != null && menu.findItem(R.id.menu_context_paste) != null
                && !OpenExplorer.USE_PRETTY_CONTEXT_MENUS)
            return null;
        // else if(!OpenExplorer.USE_PRETTY_MENUS) return null;

        if (menu == null)
            menu = new MenuBuilder(this);

        Logger.LogDebug("Trying to show context menu "
                + menu.toString()
                + (from != null ? " under " + from.toString() + " (" + from.getLeft() + ","
                        + from.getTop() + ")" : "") + ".");
        try {
            /*
             * if(mToolbarButtons != null) for(int i = menu.size() - 1; i >= 0;
             * i--) { MenuItem item = menu.getItem(i);
             * if(mToolbarButtons.findViewById(item.getItemId()) != null)
             * menu.removeItemAt(i); }
             */
            onPrepareOptionsMenu(menu);
            IconContextMenu mOpenMenu = new IconContextMenu(this, menu, from);
            if (cols > 1) {
                mOpenMenu.setTextLayout(R.layout.context_item);
                mOpenMenu.setNumColumns(cols);
            }
            mOpenMenu.setOnIconContextItemSelectedListener(this);
            mOpenMenu.show();
            return mOpenMenu;
        } catch (Exception e) {
            Logger.LogWarning("Couldn't show icon context menu.", e);
        }
        return null;
    }

    /**
     * Returns true if the Intent was "Handled"
     * 
     * @param intent Input Intent
     */
    public boolean handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            OpenPath searchIn = new OpenFile("/");
            Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
            if (bundle != null && bundle.containsKey("path"))
                try {
                    searchIn = FileManager.getOpenCache(bundle.getString("path"), false, null);
                } catch (IOException e) {
                    searchIn = new OpenFile(bundle.getString("path"));
                }
            String query = intent.getStringExtra(SearchManager.QUERY);
            Logger.LogDebug("ACTION_SEARCH for \"" + query + "\" in " + searchIn);
            SearchResultsFragment srf = SearchResultsFragment.getInstance(searchIn, query);
            if (mViewPagerEnabled && mViewPagerAdapter != null) {
                mViewPagerAdapter.add(srf);
                setViewPageAdapter(mViewPagerAdapter, false);
                setCurrentItem(mViewPagerAdapter.getCount() - 1, true);
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frag, srf)
                        .commit();
            }
        } else if ((Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_EDIT
                .equals(intent.getAction())) && intent.getData() != null) {
            Uri uri = intent.getData();
            OpenPath path = FileManager.getOpenCache(uri.toString(), this);
            if (path == null && uri.getScheme().equals("file"))
                path = new OpenFile(uri.toString().replace("file:///", "/").replace("file://", "/")
                        .replace("file:/", "/"));
            if (path == null)
                return false;
            if (path.isArchive()) {
                changePath(path);
                return true;
            }
            if (editFile(path))
                return true;
            else
                changePath(path, true, true);
        } else if (intent.hasExtra("state")) {
            Bundle state = intent.getBundleExtra("state");
            onRestoreInstanceState(state);
        } else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            Logger.LogInfo("BEAM Received: " + rawMsgs.length);
            for (Parcelable p : rawMsgs) {
                NdefMessage msg = (NdefMessage)p;
                NdefRecord[] recs = msg.getRecords();
                OpenFile file = (OpenFile)getDownloadParent().getChild(
                        "nfc_" + new Time().toString());
                int dataPos = 1;
                if (recs.length > 2)
                    file = (OpenFile)getDownloadParent().getChild(
                            new String(recs[dataPos++].getPayload()));
                file.writeBytes(recs[dataPos].getPayload());
            }
        } else if (intent.hasExtra("TaskId"))
        {
            int taskId = intent.getIntExtra("TaskId", 0);
            int reqId = intent.getIntExtra("RequestId", 0);
            NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if(reqId == REQ_EVENT_CANCEL)
            {
                EventHandler.cancelRunningTasks();
                nm.cancel(taskId);
            } else if (reqId == REQ_EVENT_VIEW)
            {
                refreshOperations();
                mOpsFragment = null;
                initOpsPopup();
                BetterPopupWindow pw = mOpsFragment.getPopup();
                if(pw != null)
                    pw.showLikePopDownMenu();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState == null)
            return;
        super.onSaveInstanceState(outState);
        if (mViewPagerAdapter != null)
        {
            Parcelable p = mViewPagerAdapter.saveState();
            outState.putParcelable("oe_fragments", p);
            outState.putInt("oe_frag_index", mViewPager.getCurrentItem());
        }
        /*
         * mStateReady = false; if(mLogFragment != null) try {
         * //fragmentManager.
         * beginTransaction().remove(mLogFragment).disallowAddToBackStack
         * ().commitAllowingStateLoss(); } catch(Exception e) { } try { Fragment
         * f = fragmentManager.findFragmentByTag("ops"); //if(f != null) //
         * fragmentManager
         * .beginTransaction().remove(f).disallowAddToBackStack().
         * commitAllowingStateLoss(); } catch(Exception e) { }
         * super.onSaveInstanceState(outState); if(mViewPagerAdapter != null) {
         * Parcelable p = mViewPagerAdapter.saveState(); if(p != null) {
         * Logger.LogDebug("<-- Saving Fragments: " + p.toString());
         * outState.putParcelable("oe_fragments", p);
         * outState.putInt("oe_frag_index", mViewPager.getCurrentItem()); } }
         */
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        if (state != null) {
            Logger.LogDebug("Restoring State: " + state);
            super.onRestoreInstanceState(state);
        }
        mStateReady = true;
        if (state != null &&
                state.containsKey("oe_fragments")) {
            mViewPagerAdapter.restoreState(state, getClassLoader());
            setViewPageAdapter(mViewPagerAdapter, true);
            if (state.containsKey("oe_frag_index"))
                setCurrentItem(state.getInt("oe_frag_index"), false);
        }

    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        ContentFragment cf = getDirContentFragment(false);
        if (cf == null)
            return false;
        OpenPath path = cf.getPath();
        appData.putString("path", path.getPath());
        startSearch(null, false, appData, false);
        return true;
    }

    private void upgradeViewSettings() {
        if (getSetting(null, "pref_up_views", false))
            return;
        setSetting("pref_up_views", true);
        getPreferences().upgradeViewSettings();
    }

    private static ScrollView wrapLayoutWithScroller(View child)
    {
        ScrollView ret = new ScrollView(child.getContext());
        ret.addView(child);
        return ret;
    }

    private void initBookmarkDropdown() {
        if (mBookmarksView == null)
            mBookmarksView = new LinearLayout(this);
        if (findViewById(R.id.list_frag) != null) {
            ViewGroup leftNav = ((ViewGroup)findViewById(R.id.list_frag));
            leftNav.removeAllViews();
            leftNav.addView(wrapLayoutWithScroller(mBookmarksView));
        } else {
            View anchor = null;
            if (anchor == null)
                anchor = findViewById(R.id.title_icon_holder);
            if (anchor == null)
                anchor = findViewById(android.R.id.home);
            if (anchor == null && USE_ACTION_BAR && mBar != null && mBar.getCustomView() != null)
                anchor = mBar.getCustomView();
            if (anchor == null)
                anchor = findViewById(R.id.title_bar);
            mBookmarksPopup = new BetterPopupWindow(this, anchor);
            mBookmarksPopup.setLayout(R.layout.contextmenu_simple);
            mBookmarksPopup.setAnimation(R.style.Animations_SlideFromLeft);
            mBookmarksPopup.setPopupHeight(LayoutParams.MATCH_PARENT);
            mBookmarksPopup.setContentView(wrapLayoutWithScroller(mBookmarksView));
        }
        mBookmarks = new OpenBookmarks(this, mBookmarksView);
        mBookmarks.setOnBookmarkSelectListener(this);
        if (mBookmarksView instanceof ExpandableListView)
        {
            ExpandableListView lv = (ExpandableListView)mBookmarksView;
            for (int i = 0; i < lv.getCount(); i++)
                lv.expandGroup(i);
        }
    }

    private void initLogPopup() {
        if (mLogFragment == null)
            mLogFragment = new LogViewerFragment();
        /*
         * if(findViewById(R.id.frag_log) != null) return;
         */
        View anchor = ViewUtils.getFirstView(this, R.id.title_log, R.id.title_bar);
        mLogFragment.setupPopup(this, anchor);
    }

    private void initOpsPopup() {
        if (mOpsFragment == null) {
            mOpsFragment = new OperationsFragment();
            View anchor = ViewUtils.getFirstView(this, R.id.title_ops, R.id.title_bar);
            mOpsFragment.setupPopup(this, anchor);
        }
    }

    private void initPager() {
        mViewPager = ((OpenViewPager)findViewById(R.id.content_pager));
        if (mViewPagerEnabled && mViewPager != null) {
            setViewVisibility(false, false, R.id.content_frag, R.id.title_path);
            setViewVisibility(mTwoRowTitle, false, R.id.title_text);
            setViewVisibility(true, false, R.id.content_pager, R.id.content_pager_indicator);
            mViewPager.setOnPageChangeListener(this);
            TabPageIndicator indicator = (TabPageIndicator)findViewById(R.id.content_pager_indicator);
            if (indicator != null)
                mViewPager.setIndicator(indicator);
            else
                Logger.LogError("Couldn't find indicator!");
        } else {
            // mViewPagerEnabled = false;
            mViewPager = null; // (ViewPager)findViewById(R.id.content_pager);
            setViewVisibility(false, false, R.id.content_pager, R.id.content_pager_indicator);
            setViewVisibility(true, false, R.id.content_frag, R.id.title_text, R.id.title_path);
        }

        if (mViewPager != null && mViewPagerEnabled) {
            if (IS_DEBUG_BUILD)
                Logger.LogDebug("Setting up ViewPager");
            mViewPagerAdapter = // new PagerTabsAdapter(this, mViewPager,
                                // indicator);
            new ArrayPagerAdapter(this, mViewPager);

            mViewPagerAdapter.setOnPageTitleClickListener(this);
            setViewPageAdapter(mViewPagerAdapter, true);
        }

    }

    public void setViewVisibility(final boolean visible, final boolean allowAnimation, int... ids) {
        for (int id : ids) {
            final View v = findViewById(id);
            if (v != null && visible != (v.getVisibility() == View.VISIBLE)) {
                if (allowAnimation) {
                    Animation anim;
                    if (visible)
                        anim = AnimationUtils.makeInAnimation(getApplicationContext(), true);
                    else
                        anim = AnimationUtils.makeOutAnimation(getApplicationContext(), false);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            if (visible)
                                v.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (!visible)
                                v.setVisibility(View.GONE);
                            else
                                v.setVisibility(View.VISIBLE);
                        }
                    });
                    v.startAnimation(anim);
                } else
                    v.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    private boolean setViewPageAdapter(PagerAdapter adapter, boolean reload) {
        if (adapter == null)
            adapter = mViewPager.getAdapter();
        if (mViewPager != null && getResources() != null) {
            try {
                if (!adapter.equals(mViewPager.getAdapter()) || reload)
                    mViewPager.setAdapter(adapter);
                else
                    mViewPager.notifyDataSetChanged();
                return true;
            } catch (IndexOutOfBoundsException e) {
                Logger.LogError("Why is this happening?", e);
                return false;
            } catch (IllegalStateException e) {
                Logger.LogError("Error trying to set ViewPageAdapter", e);
                return false;
            } catch (Exception e) {
                Logger.LogError("Please stop!", e);
                return false;
            }
        }
        return false;
    }

    public static void launchUri(Activity a, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        a.startActivity(intent);
    }

    @SuppressWarnings("deprecation")
    public static void launchTranslator(Activity a) {
        new Preferences(a).setSetting("warn", "translate", true);
        String lang = Utils.getLangCode();
        Uri uri = Uri.parse("http://brandroid.org/translation_helper.php?lang=" + lang + "&full="
                + Locale.getDefault().getDisplayLanguage() + "&wid="
                + a.getWindowManager().getDefaultDisplay().getWidth());
        // Intent intent = new Intent(a, WebViewActivity.class);
        // intent.setData();
        // a.startActivity(intent);
        WebViewFragment web = new WebViewFragment().setUri(uri);
        web.setShowsDialog(true);
        if (Build.VERSION.SDK_INT < 100) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            a.startActivity(intent);
        } else if (a.findViewById(R.id.frag_log) != null) {
            ((FragmentActivity)a).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frag_log, web).addToBackStack("trans").commit();
            a.findViewById(R.id.frag_log).setVisibility(View.VISIBLE);
            LayoutParams lp = a.findViewById(R.id.frag_log).getLayoutParams();
            lp.width = a.getResources().getDimensionPixelSize(R.dimen.bookmarks_width) * 2;
            a.findViewById(R.id.frag_log).setLayoutParams(lp);
        } else {
            web.show(((FragmentActivity)a).getSupportFragmentManager(), "trans");
            if (web.getDialog() != null)
                web.getDialog().setTitle(R.string.button_translate);
            web.setUri(uri);
        }
    }

    private void showWarnings() {
        // Non-viewpager disabled

        if (checkLanguage() > 0 && !getPreferences().getBoolean("warn", "translate", false)) {
            // runOnUiThread(new Runnable(){public void run() {
            int msg = checkLanguage() == 1 ? R.string.alert_translate_google
                    : R.string.alert_translate_none;
            new AlertDialog.Builder(OpenExplorer.this)
                    .setCancelable(true)
                    .setMessage(getString(msg, Locale.getDefault().getDisplayLanguage()))
                    .setNeutralButton("Use English", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            setLanguage(getContext(), "en");
                            goHome();
                        }
                    })
                    .setPositiveButton(R.string.button_translate,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    launchTranslator(OpenExplorer.this);
                                }
                            })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getPreferences().setSetting("warn", "translate", true);
                        }
                    }).create().show();
            // }});
        } else {
            /*
             * if (!getPreferences().getBoolean("warn", "skip_help", false)) {
             * //getPreferences().setSetting("warn", "help_pager", true);
             * DialogHandler.showHelpDialog(this, "operations"); }
             */
        }
    }

    private int checkLanguage() {
        String lang = Utils.getLangCode();
        if (lang.equals("EN"))
            return 0;
        return ",AR,EL,PL,ES,FR,KO,HE,DE,RU,".indexOf("," + Utils.getLangCode() + ",") == -1 ? 2
                : 1;
    }

    public static void showSplashIntent(Context context, String start) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra("start", start);
        if (context instanceof OpenExplorer)
            ((OpenExplorer)context).startActivityForResult(intent, REQ_SPLASH);
        else if (context instanceof Activity)
            ((Activity)context).startActivityForResult(intent, REQ_SPLASH);
        else if (context instanceof FragmentActivity)
            ((FragmentActivity)context).startActivityForResult(intent, REQ_SPLASH);
        else
            context.startActivity(intent);
    }

    @SuppressWarnings("unused")
    public void updatePagerTitle(int page) {
        TextView tvLeft = null; // (TextView)findViewById(R.id.title_left);
        TextView tvRight = null; // (TextView)findViewById(R.id.title_right);
        String left = "";
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (int i = 0; i < page; i++) {
            OpenFragment f = mViewPagerAdapter.getItem(i);
            if (f instanceof ContentFragment) {
                OpenPath p = ((ContentFragment)f).getPath();
                left += p.getName();
                if (p.isDirectory() && !left.endsWith("/"))
                    left += "/";
            }
        }
        if(!left.equals(""))
        {
            SpannableString srLeft = new SpannableString(left);
            if(left.length() > 0)
                srLeft.setSpan(new ForegroundColorSpan(Color.GRAY), 0, left.length(),
                    Spanned.SPAN_COMPOSING);
            ssb.append(srLeft);
        }
        // ssb.setSpan(new ForegroundColorSpan(Color.GRAY), 0, left.length(),
        // Spanned.SPAN_COMPOSING);
        OpenFragment curr = mViewPagerAdapter.getItem(page);
        if (curr instanceof ContentFragment) {
            OpenPath pCurr = ((ContentFragment)curr).getPath();
            ssb.append(pCurr.getName());
            if (pCurr.isDirectory())
                ssb.append("/");
        }
        String right = "";
        for (int i = page + 1; i < mViewPagerAdapter.getCount(); i++) {
            OpenFragment f = mViewPagerAdapter.getItem(i);
            if (f instanceof ContentFragment) {
                OpenPath p = ((ContentFragment)f).getPath();
                right += p.getName();
                if (p.isDirectory() && !right.endsWith("/"))
                    right += "/";
            }
        }
        if(!right.equals(""))
        {
            SpannableString srRight = new SpannableString(right);
            srRight.setSpan(new ForegroundColorSpan(Color.GRAY), 0, right.length(),
                    Spanned.SPAN_COMPOSING);
            ssb.append(srRight);
        }
        updateTitle(ssb);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOW_MEMORY = true;
        showToast(R.string.s_msg_low_memory);
        ThumbnailCreator.flushCache(this, false);
        FileManager.clearOpenCache();
        EventHandler.cancelRunningTasks();
    }

    @SuppressWarnings("unused")
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("OpenExplorer.onAttachedToWindow");
        handleNetworking();
        handleMediaReceiver();
        if (getWindowManager() != null) {
            Display d = getWindowManager().getDefaultDisplay();
            if (d != null) {
                DisplayMetrics dm = new DisplayMetrics();
                d.getMetrics(dm);
                if (dm != null) {
                    SCREEN_DPI = dm.densityDpi;
                    SCREEN_WIDTH = dm.widthPixels;
                    SCREEN_HEIGHT = dm.heightPixels;
                } else {
                    SCREEN_WIDTH = d.getWidth();
                    SCREEN_HEIGHT = d.getHeight();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("OpenExplorer.onStart");
        if (findViewById(R.id.frag_log) != null) {
            fragmentManager.beginTransaction().replace(R.id.frag_log, mLogFragment, "log").commit();
            findViewById(R.id.frag_log).setVisibility(View.GONE);
        } else {
            initLogPopup();
        }
        // submitStats();
        // new Thread(new Runnable(){public void run()
        // {refreshCursors();}}).start();;
        // refreshCursors();
        // mBookmarks.scanBookmarks();
        mStateReady = true;
    }

    /*
     * @Override protected void onSaveInstanceState(Bundle outState) { try {
     * super.onSaveInstanceState(outState); List<Fragment> frags =
     * mViewPagerAdapter.getFragments(); String[] paths = new
     * String[frags.size()]; for(int i = 0; i < frags.size(); i++) { Fragment f
     * = frags.get(i); if(f instanceof OpenPathFragmentInterface) paths[i] =
     * ((OpenPathFragmentInterface)f).getPath().toString(); }
     * outState.putStringArray("paths", paths);
     * Logger.LogDebug("-->Saving fragments: " + paths.length); }
     * catch(Exception e) { Logger.LogError("Couldn't save main state", e); } }
     * @Override protected void onRestoreInstanceState(Bundle
     * savedInstanceState) { super.onRestoreInstanceState(savedInstanceState);
     * if(savedInstanceState != null && savedInstanceState.containsKey("paths"))
     * { String[] paths = savedInstanceState.getStringArray("paths");
     * Logger.LogDebug("<--Restoring fragments: " + paths.length); for(int i =
     * 0; i < paths.length; i++) { String path = paths[i]; if(path == null)
     * continue; try { OpenPath file = FileManager.getOpenCache(path, false,
     * null); if(file == null) file = new OpenFile(path); if(file.isTextFile())
     * mViewPagerAdapter.add(new TextEditorFragment(file)); } catch (IOException
     * e) { Logger.LogError("Couldn't get Path while restoring state", e); } }
     * mViewPagerAdapter.notifyDataSetChanged(); } }
     */

    private void checkTitleSeparator() {
        if (mStaticButtons == null) {
            View tsb = findViewById(R.id.title_static_buttons);
            if (tsb != null && tsb instanceof ViewGroup)
                mStaticButtons = (ViewGroup)tsb;
        }
        if (mStaticButtons == null && USE_ACTION_BAR && mBar != null
                && mBar.getCustomView() != null)
            Logger.LogDebug(String.valueOf(mBar.getCustomView().findViewById(R.id.title_static_buttons)));
            Logger.LogDebug(String.valueOf(mBar.getCustomView().findViewById(R.id.title_static_buttons).getClass()));
            mStaticButtons = (ViewGroup)mBar.getCustomView()
                    .findViewById(R.id.title_static_buttons);
        if (mStaticButtons == null) {
            Logger.LogWarning("Unable to find Title Separator");
            return;
        }

        boolean visible = false;
        for (int id : new int[] {
                R.id.title_paste, R.id.title_log, R.id.title_ops
        })
            if (mStaticButtons.findViewById(id) != null
                    && mStaticButtons.findViewById(id).getVisibility() == View.VISIBLE)
                visible = true;

        ViewUtils.setViewsVisible(mStaticButtons, visible, R.id.title_divider);
    }

    public void sendToLogView(final String txt, final int color) {
        try {

            if (txt == null)
                return;
            if (mLogFragment == null)
                mLogFragment = new LogViewerFragment();
            mLogFragment.print(txt, color);
            ViewUtils.setViewsVisible(this, true, R.id.title_log);
            if (mLogFragment.getAdded())
                return;
            checkTitleSeparator();
            if (!mLogFragment.getAdded() && !mLogFragment.isVisible()) {
                mLogFragment.setAdded(true);
                final View logview = findViewById(R.id.frag_log);
                if (logview != null && !mLogFragment.getAdded()) {
                    Fragment fl = fragmentManager.findFragmentById(R.id.frag_log);
                    if (!(fl instanceof LogViewerFragment))
                        fragmentManager.beginTransaction().replace(R.id.frag_log, mLogFragment)
                                .disallowAddToBackStack().commitAllowingStateLoss();
                    logview.post(new Runnable() {
                        public void run() {
                            logview.setVisibility(View.VISIBLE);
                        }
                    });
                } // else mLogFragment.show(fragmentManager, "log");
            }
        } catch (Exception e) {
            Logger.LogError("Couldn't send to Log Viewer", e);
        }
    }

    private void setupFilesDb() {
        OpenPath.setDb(new OpenPathDbAdapter(getApplicationContext()));
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveOpenedEditors();
        submitStats();
        if (Logger.isLoggingEnabled() && Logger.hasDb())
            Logger.closeDb();
        OpenPath.closeDb();
    }

    @SuppressWarnings("deprecation")
    public boolean isNetworkConnected() {
        ConnectivityManager conman = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!conman.getBackgroundDataSetting())
            return false;
        NetworkInfo ni = conman.getActiveNetworkInfo();
        if (ni == null)
            return false;
        if (!ni.isAvailable() || !ni.isConnected())
            return false;
        if (ni.getState() == State.CONNECTING)
            return false;
        return true;
    }

    private void submitStats() // submit anonymous error log
    {
        if (!Logger.isLoggingEnabled())
            return; // Disable by default
            // if (OpenExplorer.IS_DEBUG_BUILD)
            // return;
        if (new Date().getTime() - lastSubmit < 6000)
        {
            Logger.LogVerbose("Skipping stats. Not enough time has passed ("
                    + (new Date().getTime() - lastSubmit) + ")");
            return;
        }
        lastSubmit = new Date().getTime();
        if (!isNetworkConnected())
            return;

        String logs = Logger.getDbLogs(false);
        if (logs == null || "".equals(logs))
            logs = "[]";
        // if(logs != null && logs != "") {
        Logger.LogDebug("Found " + logs.length() + " bytes of logs.");
        EventHandler.execute(new SubmitStatsTask(this), logs);
        // } else Logger.LogWarning("Logs not found.");
    }

    public void handleRefreshMedia(final String path, boolean keepChecking, final int retries) {
        if (!keepChecking || retries <= 0) {
            refreshBookmarks();
            return;
        }
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Logger.LogDebug("Check " + retries + " for " + path);
                try {
                    StatFs sf = new StatFs(path);
                    if (sf.getBlockCount() == 0)
                        throw new Exception("No blocks");
                    showToast(getResources().getString(R.string.s_alert_new_media)
                            + " "
                            + getVolumeName(path)
                            + " @ "
                            + OpenPath.formatSize((long)sf.getBlockSize()
                                    * (long)sf.getAvailableBlocks()));
                    refreshBookmarks();
                    if (mLastPath.getPath().equals(path))
                        goHome();
                } catch (Throwable e) {
                    Logger.LogWarning("Couldn't read " + path);
                    handleRefreshMedia(path, true, retries - 1); // retry again
                                                                 // in 1/2
                                                                 // second
                }
            }
        }, 1000);
    }

    private void handleNfc() {
        if (Build.VERSION.SDK_INT < 14)
            return;
        // Initialize nfc adapter
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null)
            return;
        if (Build.VERSION.SDK_INT >= 16) {
            mNfcAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public Uri[] createBeamUris(NfcEvent event) {
                    List<OpenPath> selectedFiles = getClipboard();
                    if (selectedFiles.size() > 0) {
                        List<Uri> fileUri = new ArrayList<Uri>();
                        for (OpenPath f : selectedFiles) {
                            // Beam ignores folders and system files
                            if (!f.isDirectory() && f.canWrite()) {
                                fileUri.add(f.getUri());
                            }
                        }
                        if (fileUri.size() > 0) {
                            Logger.LogInfo("BEAM: " + fileUri.size() + " items");
                            return fileUri.toArray(new Uri[fileUri.size()]);
                        }
                    }
                    return null;
                }
            }, this);
        } else if (Build.VERSION.SDK_INT >= 14) {
            mNfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {
                public NdefMessage createNdefMessage(NfcEvent event) {
                    Logger.LogVerbose("Beam me up, scotty!");
                    List<OpenPath> selectedFiles = getClipboard();
                    if (selectedFiles.size() > 0) {
                        List<NdefRecord> recs = new ArrayList<NdefRecord>();
                        for (OpenPath f : selectedFiles) {
                            if (!(f instanceof OpenFile) || f.isDirectory() || !f.canWrite())
                                continue;
                            OpenFile of = (OpenFile)f;
                            NdefRecord rec = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                                    "application/vnd.org.brandroid.beam".getBytes(),
                                    f.getName().getBytes(),
                                    of.readBytes());
                            recs.add(rec);
                        }
                        if (recs.size() > 0) {
                            Logger.LogInfo("BEAM: " + recs.size() + " items");
                            return new NdefMessage(recs.toArray(new NdefRecord[recs.size()]));
                        }
                    }
                    return null;
                }
            }, this);
        }
    }

    public void handleMediaReceiver() {
        storageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String data = intent.getDataString();
                final String path = data.replace("file://", "");
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED))
                    handleRefreshMedia(path, true, 10);
                else
                    refreshBookmarks();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        registerReceiver(storageReceiver, filter);

        ContentObserver mDbObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                // rebake(false, ImageManager.isMediaScannerScanning(
                // getContentResolver()));
            }
        };

        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, mDbObserver);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (IS_DEBUG_BUILD)
            Logger.LogDebug("OpenExplorer.onPostCreate");
        // mActionBarHelper.onPostCreate(savedInstanceState);
        ensureCursorCache();
        /*
         * if(mSettingsListener != null) {
         * mSettingsListener.onHiddenFilesChanged
         * (mPreferences.getBoolean(SettingsActivity.PREF_HIDDEN_KEY, false));
         * mSettingsListener
         * .onThumbnailChanged(mPreferences.getBoolean(SettingsActivity
         * .PREF_THUMB_KEY, true));
         * mSettingsListener.onViewChanged(mPreferences.
         * getString(SettingsActivity.PREF_VIEW_KEY, "list")); }
         */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (storageReceiver != null)
            unregisterReceiver(storageReceiver);
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    public void setActionMode(ActionMode mode) {
        mActionMode = mode;
        mViewPager.setLocked(mActionMode != null);
    }

    public void setViewPagerLocked(boolean locked) {
        mViewPager.setLocked(locked);
    }

    public OpenClipboard getClipboard() {
        if (mClipboard == null)
            mClipboard = new OpenClipboard(this);
        return mClipboard;
    }

    public void addHoldingFile(OpenPath path) {
        getClipboard().add(path);
        invalidateOptionsMenu();
    }

    public void clearHoldingFiles() {
        getClipboard().clear();
        invalidateOptionsMenu();
    }

    public static final OpenPathMerged getPhotoParent() {
        // if(mPhotoParent == null) refreshCursors();
        return mPhotosMerged;
    }

    public static final OpenPathMerged getVideoParent() {
        // if(mVideoParent == null) refreshCursors();
        return mVideosMerged;
    }

    public static final OpenCursor getMusicParent() {
        // if(mMusicParent == null) refreshCursors();
        return mMusicParent;
    }

    public static final OpenSmartFolder getDownloadParent() {
        return mDownloadParent;
    }

    private boolean findCursors() {
        mVideosMerged.setName(getString(R.string.s_videos));
        mPhotosMerged.setName(getString(R.string.s_photos));
        mMusicParent.setName(getString(R.string.s_music));
        mDownloadParent.setName(getString(R.string.s_downloads));

        final OpenFile extDrive = OpenFile.getExternalMemoryDrive(false);
        final OpenFile intDrive = OpenFile.getInternalMemoryDrive();
        final boolean mHasExternal = extDrive != null && extDrive.exists();
        final boolean mHasInternal = intDrive != null && intDrive.exists();

        if (mVideoParent.isLoaded()) {
            // Logger.LogDebug("Videos should be found");
        } else {
            if (bRetrieveExtraVideoDetails)
                bRetrieveExtraVideoDetails = !getSetting(null, "tag_novidinfo", false);
            if (IS_DEBUG_BUILD)
                Logger.LogVerbose("Finding videos");
            // if(!IS_DEBUG_BUILD)
            try {
                mVideosMerged.addParent(mVideoParent);
                mVideosMerged.addParent(mVideoSearchParent);
                getSupportLoaderManager().initLoader(0, null, this);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            if (mHasExternal && extDrive != null && intDrive.list() != null)
                                for (OpenPath kid : extDrive.list())
                                    if (kid.getName().toLowerCase().indexOf("movies") > -1
                                            || kid.getName().toLowerCase().indexOf("video") > -1)
                                        mVideoSearchParent.addSearch(new SmartSearch(kid,
                                                SmartSearch.SearchType.TypeIn, "avi", "mpg", "3gp",
                                                "mkv", "mp4"));
                            if (mHasInternal && intDrive != null && intDrive.list() != null)
                                for (OpenPath kid : intDrive.list())
                                    if (kid.getName().toLowerCase().indexOf("movies") > -1
                                            || kid.getName().toLowerCase().indexOf("video") > -1)
                                        mVideoSearchParent.addSearch(new SmartSearch(kid,
                                                SmartSearch.SearchType.TypeIn, "avi", "mpg", "3gp",
                                                "mkv", "mp4"));
                            if (isNook()) {
                                OpenFile files = OpenFile.getExternalMemoryDrive(true);
                                for (int i = 0; i < 2; i++) {
                                    if (i == 1)
                                        files = new OpenFile("/mnt/media");
                                    files = files.getChild("My Files");
                                    if (files != null && files.exists()) {
                                        files = files.getChild("Videos");
                                        if (files != null && files.exists())
                                            mVideoSearchParent.addSearch(new SmartSearch(files,
                                                    SmartSearch.SearchType.TypeIn, "avi", "mpg",
                                                    "3gp", "mkv", "mp4"));
                                    }
                                }
                            }
                            mVideosMerged.refreshKids();
                        } catch (Exception e) {
                            Logger.LogError("Couldn't refresh merged Videos");
                        }
                    }
                }).start();
            } catch (Exception e) {
                Logger.LogError("Couldn't query videos.", e);
            }
            Logger.LogDebug("Done looking for videos");
        }
        if (!mPhotoParent.isLoaded()) {
            if (bRetrieveDimensionsForPhotos)
                bRetrieveDimensionsForPhotos = !getSetting(null, "tag_nodims", false);
            if (IS_DEBUG_BUILD)
                Logger.LogVerbose("Finding Photos");
            try {
                mPhotosMerged.addParent(mPhotoParent);
                mPhotosMerged.addParent(mPhotoSearchParent);
                getSupportLoaderManager().initLoader(1, null, this);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            if (mHasExternal && extDrive != null && extDrive.list() != null)
                                for (OpenPath kid : Utils.ifNull(extDrive.list(), new OpenPath[0]))
                                    if (kid.getName().toLowerCase().indexOf("photo") > -1
                                            || kid.getName().toLowerCase().indexOf("picture") > -1
                                            || kid.getName().toLowerCase().indexOf("dcim") > -1
                                            || kid.getName().toLowerCase().indexOf("camera") > -1)
                                        mPhotoSearchParent.addSearch(new SmartSearch(kid,
                                                SmartSearch.SearchType.TypeIn, "jpg", "bmp", "png",
                                                "gif", "jpeg"));
                            if (mHasInternal)
                                for (OpenPath kid : intDrive.list())
                                    if (kid.getName().toLowerCase().indexOf("photo") > -1
                                            || kid.getName().toLowerCase().indexOf("picture") > -1
                                            || kid.getName().toLowerCase().indexOf("dcim") > -1
                                            || kid.getName().toLowerCase().indexOf("camera") > -1)
                                        mPhotoSearchParent.addSearch(new SmartSearch(kid,
                                                SmartSearch.SearchType.TypeIn, "jpg", "bmp", "png",
                                                "gif", "jpeg"));
                            if (isNook()) {
                                OpenFile files = intDrive.getChild("My Files");
                                for (int i = 0; i < 2; i++) {
                                    if (i == 1) {
                                        if (extDrive != null)
                                            files = extDrive.getChild("My Files");
                                        continue;
                                    }
                                    if (files != null && files.exists()) {
                                        files = files.getChild("Pictures");
                                        if (files != null && files.exists())
                                            mPhotoSearchParent.addSearch(new SmartSearch(files,
                                                    SmartSearch.SearchType.TypeIn, "jpg", "bmp",
                                                    "png",
                                                    "gif", "jpeg"));
                                    }
                                }
                            }
                            mPhotosMerged.refreshKids();
                        } catch (Exception e) {
                            Logger.LogError("Couldn't refresh merged Videos");
                        }
                    }
                }).start();
                Logger.LogDebug("Done looking for photos");

            } catch (IllegalStateException e) {
                Logger.LogError("Couldn't query photos.", e);
            }
        }
        if (!mMusicParent.isLoaded()) {
            if (IS_DEBUG_BUILD)
                Logger.LogVerbose("Finding Music");
            try {
                getSupportLoaderManager().initLoader(2, null, this);
                Logger.LogDebug("Done looking for music");
            } catch (IllegalStateException e) {
                Logger.LogError("Couldn't query music.", e);
            }
        }
        if (!mApkParent.isLoaded()) {
            if (IS_DEBUG_BUILD)
                Logger.LogVerbose("Finding APKs");
            try {
                getSupportLoaderManager().initLoader(3, null, this);
            } catch (IllegalStateException e) {
                Logger.LogError("Couldn't get Apks.", e);
            }
        }
        if (!mDownloadParent.isLoaded()) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                    if (mHasExternal && extDrive != null)
                        for (OpenPath kid : extDrive.list())
                            if (kid.getName().toLowerCase().indexOf("download") > -1)
                                mDownloadParent.addSearch(new SmartSearch(kid));
                    if (mHasInternal && intDrive != null && intDrive.list() != null)
                        for (OpenPath kid : intDrive.list())
                            if (kid.getName().toLowerCase().indexOf("download") > -1)
                                mDownloadParent.addSearch(new SmartSearch(kid));
                    if (isNook()) {
                        String[] bnRoots = new String[] {
                                "/data/media/", "/mnt/media/", "/mnt/sdcard/"
                        };
                        String[] bnDownloads = new String[] {
                                "/", "B&N Downloads/", "My Files/", "My Files/B&N Downloads/"
                        };
                        String[] bnSubs = new String[] {
                                "Magazines/", "Books/", "Newspapers/", "Extras/"
                        };
                        for (String root : bnRoots)
                            for (String dl : bnDownloads)
                                for (String folder : bnSubs) {
                                    OpenFile kid = new OpenFile(root + dl + folder);
                                    if (kid.exists() && kid.isDirectory())
                                        mDownloadParent.addSearch(new SmartSearch(kid));
                                }
                    }
                    } catch(Exception e) {
                        Logger.LogError("Unable to get downloads.", e);
                    }
                }
            }).start();
        }
        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("Done finding cursors");
        return true;
    }

    private void refreshCursors() {
        if (findCursors())
            return;
        // new Thread(new Runnable(){public void run() {
        // ensureCursorCache();
        // }}).start();
    }

    public void ensureCursorCache() {
        // findCursors();
        if (!Preferences.Pref_CacheThumbs)
            return;
        if (!Preferences.Pref_ShowThumbs)
            return;
        if (mRunningCursorEnsure
        // || mLastCursorEnsure == 0
        // || new Date().getTime() - mLastCursorEnsure < 10000 // at least 10
        // seconds
        ) {
            if (IS_DEBUG_BUILD)
                Logger.LogVerbose("Skipping ensureCursorCache");
            return;
        } else if (IS_DEBUG_BUILD)
            Logger.LogVerbose("Running ensureCursorCache");
        mRunningCursorEnsure = true;

        // group into blocks
        int iTotalSize = 0;
        for (OpenCursor cur : new OpenCursor[] {
                mVideoParent, mPhotoParent, mApkParent
        })
            if (cur != null)
                iTotalSize += cur.length();
        int enSize = Math.max(20, iTotalSize / 10);
        Logger.LogDebug("EnsureCursorCache size: " + enSize + " / " + iTotalSize);
        ArrayList<OpenPath> buffer = new ArrayList<OpenPath>(enSize);
        for (OpenCursor curs : new OpenCursor[] {
                mVideoParent, mPhotoParent, mApkParent
        }) {
            if (curs == null)
                continue;
            for (OpenMediaStore ms : curs.list()) {
                buffer.add(ms);
                if (buffer.size() == enSize) {
                    OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
                    buffer.toArray(buff);
                    buffer.clear();
                    try {
                        // Logger.LogDebug("Executing Task of " + buff.length +
                        // " items");
                        /*
                         * if(!BEFORE_HONEYCOMB) new
                         * EnsureCursorCacheTask().executeOnExecutor(new
                         * Executor() { public void execute(Runnable command) {
                         * command.run(); } }, buff); else
                         */
                        EventHandler.execute(new EnsureCursorCacheTask(), buff);
                    } catch (RejectedExecutionException e) {
                        Logger.LogWarning("Couldn't ensure cache.", e);
                        return;
                    }
                }
            }
        }
        if (buffer.size() > 0) {
            OpenMediaStore[] buff = new OpenMediaStore[buffer.size()];
            buffer.toArray(buff);
            buffer.clear();
            try {
                Logger.LogDebug("Executing Task of " + buff.length + " items");
                /*
                 * if(!BEFORE_HONEYCOMB) new
                 * EnsureCursorCacheTask().executeOnExecutor(new Executor() {
                 * public void execute(Runnable command) { command.run(); } },
                 * buff); else
                 */
                EventHandler.execute(new EnsureCursorCacheTask(), buff);
            } catch (RejectedExecutionException e) {
                Logger.LogWarning("Couldn't ensure cache.", e);
                return;
            }
        }

        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("Done with ensureCursorCache");

        // mLastCursorEnsure = new Date().getTime();
        mRunningCursorEnsure = false;
    }

    /**
     * Toggle Bookmarks View.
     * 
     * @param visible Whether to show Bookmarks. Will only affect popup.
     * @return Visibility of Bookmarks after event has occurred.
     */
    public boolean toggleBookmarks(boolean visible) {
        return toggleBookmarks(visible, true);
    }

    /**
     * Toggle Bookmarks View.
     * 
     * @param visible Whether to show Bookmarks.
     * @param auto Is automatic? If true, will only dismiss popup (when
     *            available), and will not affect fragment. If false, will
     *            affect fragment.
     * @return Visibility of Bookmarks after event has occurred.
     */
    public boolean toggleBookmarks(boolean visible, boolean auto) {
        if (isSinglePane() && mBookmarksPopup != null) {
            if (visible)
                mBookmarksPopup.showLikePopDownMenu();
            else
                mBookmarksPopup.dismiss();
        } else if (!auto) {
            setViewVisibility(visible, true, R.id.list_frag);
        }
        return visible;
    }

    /**
     * Toggle Bookmarks View. Only to be used in response to touch event.
     * 
     * @return Visibility of Bookmarks after event has occurred.
     */
    public boolean toggleBookmarks() {
        final View mBookmarks = findViewById(R.id.list_frag);
        final boolean in = mBookmarks == null || mBookmarks.getVisibility() == View.GONE;
        return toggleBookmarks(in, false);
    }

    public void refreshOperations() {
        initOpsPopup();
        int tasks = EventHandler.getTaskList().size();
        ViewUtils.setViewsVisible(this, tasks > 0, R.id.title_ops);
        checkTitleSeparator();
    }

    /**
     * Refresh list of bookmarks.
     */
    public void refreshBookmarks() {
        if (IS_DEBUG_BUILD)
            Logger.LogVerbose("refreshBookmarks()");
        refreshCursors();
        if (mBookmarks != null) {
            mBookmarks.scanBookmarks(this);
        }
    }

    public ContentFragment getDirContentFragment(Boolean activate) {
        return getDirContentFragment(activate, mLastPath);
    }

    public ContentFragment getDirContentFragment(Boolean activate, OpenPath path) {
        // Logger.LogDebug("getDirContentFragment");
        OpenFragment ret = null;
        int i = mViewPagerAdapter.getCount();
        if (mViewPagerAdapter != null && mViewPager != null) {
            if (mViewPager.getCurrentItem() > -1) {
                ret = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
            } else {
                Logger.LogWarning("Couldn't find current Page. Using last.");
                ret = mViewPagerAdapter.getItem(i);
            }
            while (!(ret instanceof ContentFragment) && i >= 0)
                ret = mViewPagerAdapter.getItem(i--);
        }
        if (ret == null)
            ret = (OpenFragment)fragmentManager.findFragmentById(R.id.content_frag);
        if (ret == null && path != null) {
            ret = ContentFragment.getInstance(path, getSetting(path, "view", 0),
                    getSupportFragmentManager());
            if (mViewPager != null && ret != null)
                mViewPagerAdapter.add(ret);

        }
        if (activate && ret != null && !ret.isVisible()
                && mViewPagerAdapter.getItemPosition(ret) > -1)
            setCurrentItem(mViewPagerAdapter.getItemPosition(ret), false);

        if (ret != null && ret instanceof ContentFragment)
            return (ContentFragment)ret;
        else
            return null;
    }

    public OpenFragment getSelectedFragment() {
        OpenFragment ret = null;
        // if(mViewPager != null && mViewPagerAdapter != null &&
        // mViewPagerAdapter instanceof OpenPathPagerAdapter &&
        // ((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem() instanceof
        // ContentFragment)
        // ret =
        // ((ContentFragment)((OpenPathPagerAdapter)mViewPagerAdapter).getLastItem());
        if (mViewPagerAdapter != null && mViewPager != null) {
            if (mViewPager.getCurrentItem() > -1) {
                // Logger.LogVerbose("Current Page: " +
                // (mViewPager.getCurrentItem() + 1) + " of " +
                // mViewPagerAdapter.getCount());
                ret = mViewPagerAdapter.getItem(mViewPager.getCurrentItem());
            } else {
                Logger.LogWarning("Couldn't find current Page. Using last.");
                ret = mViewPagerAdapter.getItem(mViewPagerAdapter.getCount() - 1);
            }
        }
        if (ret == null && fragmentManager != null)
            ret = (OpenFragment)fragmentManager.findFragmentById(R.id.content_frag);

        return ret;
    }

    public void updateTitle(CharSequence cs) {
        TextView title = (TextView)findViewById(R.id.title_path);
        if ((title == null || !title.isShown()) && mBar != null && mBar.getCustomView() != null)
            title = (TextView)mBar.getCustomView().findViewById(R.id.title_path);
        // if(BEFORE_HONEYCOMB || !USE_ACTION_BAR || mBar == null)
        if (title != null && title.getVisibility() != View.GONE)
            title.setText(cs, BufferType.SPANNABLE);
        if (!USE_ACTION_BAR && mBar != null && (title == null || !title.isShown()))
            mBar.setSubtitle(cs);
        // else
        {
            SpannableStringBuilder sb = new SpannableStringBuilder(getResources().getString(
                    R.string.app_title));
            if(!cs.equals(""))
            {
                sb.append(cs.equals("") ? "" : " - ");
                sb.append(cs);
            }
            setTitle(sb);
        }
    }

    private void saveOpenedEditors() {
        StringBuilder editing = new StringBuilder(",");
        for (int i = 0; i < mViewPagerAdapter.getCount(); i++) {
            OpenFragment f = mViewPagerAdapter.getItem(i);
            if (f instanceof TextEditorFragment) {
                TextEditorFragment tf = (TextEditorFragment)f;
                if (!tf.isSalvagable())
                    continue;
                OpenPath path = tf.getPath();
                if (path != null && editing.indexOf("," + path.getPath() + ",") == -1)
                    editing.append(path + ",");
            }
        }
        if (IS_DEBUG_BUILD && !editing.equals(","))
            Logger.LogDebug("Saving [" + editing.toString() + "] as TextEditorFragments");
        setSetting("editing", editing.toString());
    }

    private void restoreOpenedEditors() {
        String editing = getSetting(null, "editing", (String)null);
        Logger.LogDebug("Restoring [" + editing + "] to TextEditorFragments");
        if (editing == null)
            return;
        for (String s : editing.split(",")) {
            if (s == null || "".equals(s))
                continue;
            OpenPath path = FileManager.getOpenCache(s, this);
            if (path == null)
                continue;
            editFile(path, true);
        }
        setViewPageAdapter(mViewPagerAdapter, true);
    }

    public void closeFragment(final OpenFragment frag) {
        final int pos = mViewPagerAdapter.getItemPosition(frag);
        if (pos >= 0) {
            if (frag instanceof TextEditorFragment)
                ((TextEditorFragment)frag).setSalvagable(false);
            mViewPager.post(new Runnable() {
                public void run() {
                    int cp = mViewPager.getCurrentItem();
                    mViewPagerAdapter.remove(frag);

                    setViewPageAdapter(mViewPagerAdapter, false);
                    if (frag instanceof TextEditorFragment)
                        saveOpenedEditors();
                    if (mViewPagerAdapter.getCount() == 0)
                        finish();
                    if (cp == pos && pos > 0)
                        mViewPager.setCurrentItem(pos - 1, true);
                    else if (cp == pos && mViewPagerAdapter.getCount() > 1)
                        mViewPager.setCurrentItem(pos + 1, true);
                }
            });
        }
    }

    public boolean editFile(OpenPath path) {
        return editFile(path, false);
    }

    public boolean editFile(OpenPath path, boolean batch) {
        if (path == null)
            return false;
        if (!path.exists())
            return false;
        if (path.length() > Preferences.Pref_Text_Max_Size)
            return false;
        TextEditorFragment editor = TextEditorFragment.getInstance(path);
        if (mViewPagerAdapter != null) {
            int pos = mViewPagerAdapter.getItemPosition(editor);

            if (pos < 0) {
                mViewPagerAdapter.add(editor);

                setViewPageAdapter(mViewPagerAdapter, !batch);
                if (!batch) {
                    saveOpenedEditors();
                    pos = mViewPagerAdapter.getItemPosition(editor);

                    if (pos > -1)
                        setCurrentItem(pos, true);
                }
            } else if (!batch)
                setCurrentItem(pos, true);
        } else
            fragmentManager.beginTransaction().replace(R.id.content_frag, editor)

                    // .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
        // addTab(editor, path.getName(), true);
        return true;
    }

    @Override
    public void startActivity(Intent intent) {
        // if(handleIntent(intent)) return;
        super.startActivity(intent);
    }

    public void goHome() {
        Bundle b = new Bundle();
        b.putString("last", mLastPath.getPath());
        onSaveInstanceState(b);
        Intent intent = new Intent(this, OpenExplorer.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (mLastPath != null)
            intent.putExtra("last", mLastPath.getPath());
        else if (getDirContentFragment(false) != null)
            intent.putExtra("last", getDirContentFragment(false).getPath().getPath());
        intent.putExtra("state", b);
        if (OpenExplorer.BEFORE_HONEYCOMB) {
            finish();
            startActivity(intent);
        } else {
            setIntent(intent);
            recreate();
        }
    }

    /*
     * @Override public void onCreateContextMenu(ContextMenu menu, View v,
     * ContextMenuInfo menuInfo) { if(DEBUG)
     * Logger.LogDebug("<-- OpenExplorer.onCreateContextMenu");
     * super.onCreateContextMenu(menu, v, menuInfo); int contextMenuId =
     * MenuUtils.getMenuLookupSub(v.getId()); if(contextMenuId > -1)
     * getMenuInflater().inflate(contextMenuId, menu); //else if(v.getId() ==
     * R.id.menu_more) // onCreateOptionsMenu(menu, false); else
     * Logger.LogWarning("Submenu not found for " +
     * Integer.toHexString(v.getId())); if(DEBUG)
     * Logger.LogDebug("--> OpenExplorer.onCreateContextMenu"); }
     */

    /*
     * @Override public boolean onContextItemSelected(android.view.MenuItem
     * item) { return onClick(item.getItemId(), item, null); }
     */

    /*
     * @Override public void invalidateOptionsMenu() { mLastMenuClass = "";
     * //if(BEFORE_HONEYCOMB && !USE_PRETTY_MENUS) return;
     * //if(USE_PRETTY_MENUS) setupBaseBarButtons(); //if(!BEFORE_HONEYCOMB) try
     * { super.invalidateOptionsMenu(); } catch(Exception e) {
     * Logger.LogError("Unable to invalidateOptionsMenu", e); } }
     */

    public static int getVisibleChildCount(ViewGroup parent) {
        int ret = 0;
        for (int i = 0; i < parent.getChildCount(); i++)
            if (parent.getChildAt(i).getVisibility() != View.GONE)
                ret++;
        return ret;
    }

    private boolean shouldFlushMenu(Menu menu) {
        if (menu == null)
            return true;
        if (!menu.hasVisibleItems())
            return true;
        OpenFragment f = getSelectedFragment();
        if (f == null)
            return false;
        return !f.getClassName().equals(mLastMenuClass);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return onCreateOptionsMenu(menu, true);
    }

    public boolean onCreateOptionsMenu(Menu menu, boolean fromSystem) {
        // if(DEBUG)
        // Logger.LogDebug("OpenExplorer.onCreateOptionsMenu(" + menu + "," +
        // fromSystem + ")");
        // getSupportMenuInflater().inflate(R.menu.global_top, menu);
        // super.onCreateOptionsMenu(menu);
        // getSupportMenuInflater().inflate(R.menu.global, menu);
        // return true;
        MenuInflater menuInflater = getSupportMenuInflater();

        // menuInflater.inflate(R.menu.menu_main, menu);
        // menuInflater.inflate(R.menu.global_top, menu);
        OpenFragment f = getSelectedFragment();
        if (f != null)
            f.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.global, menu);

        handleSearchMenu(menu);

        MenuUtils.setMenuVisible(menu, Preferences.Run_Count > 5, R.id.menu_donate, R.id.menu_rate);

        mMenuPaste = menu.findItem(R.id.menu_context_paste);

        if (IS_KEYBOARD_AVAILABLE)
            MenuUtils.setMneumonics(menu);

        return super.onCreateOptionsMenu(menu);
    }

    private void handleSearchMenu(Menu menu) {
        final MenuItem mSearch = menu.findItem(R.id.menu_search) != null ? menu
                .findItem(R.id.menu_search) : menu.add(Menu.FIRST, R.id.menu_search, Menu.NONE,
                R.string.s_search).setIcon(
                getContext().getResources().getDrawable(
                        getThemedResourceId(R.styleable.AppTheme_actionIconSearch,
                                R.drawable.ic_action_search)));

        final SearchView searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint(getString(R.string.s_search_files));
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Logger.LogVerbose("QUERY: " + query);
                searchView.clearFocus();
                mSearch.collapseActionView();
                searchView.setIconified(true);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEARCH);
                Bundle appData = new Bundle();
                appData.putString("path", getDirContentFragment(false).getPath().getPath());
                intent.putExtra(SearchManager.APP_DATA, appData);
                intent.putExtra(SearchManager.QUERY, query);
                handleIntent(intent);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO Auto-generated method stub
                return false;
            }
        });
        mSearch.setActionView(searchView).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {

        // MenuUtils.setMenuChecked(menu, USE_SPLIT_ACTION_BAR,
        // R.id.menu_view_split);
        // MenuUtils.setMenuChecked(menu, mLogFragment != null &&
        // mLogFragment.isVisible(), R.id.menu_view_logview);
        // MenuUtils.setMenuChecked(menu, getPreferences().getBoolean("global",
        // "pref_fullscreen", false), R.id.menu_view_fullscreen);

        MenuUtils.setMenuVisible(menu, getResources().getBoolean(R.bool.allow_fullscreen),
                R.id.menu_view_fullscreen);
        MenuUtils.setMenuVisible(menu, getClipboard().size() > 0, R.id.menu_context_paste);
        MenuUtils.setMenuChecked(menu, getActionMode() != null, R.id.menu_multi);

        if (menu != null && mMenuPaste != null && getClipboard() != null
                && getClipboard().size() > 0) {
            SubMenu sub = menu.findItem(R.id.content_paste).getSubMenu();
            if (sub != null) {
                int i = 0;
                for (final OpenPath item : getClipboard().getAll()) {
                    sub.add(Menu.CATEGORY_CONTAINER, i++, i, item.getName())
                            .setIcon(ThumbnailCreator.getDefaultResourceId(item, 32, 32))
                            .setCheckable(true)
                            .setChecked(true)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem menuitem) {
                                    getClipboard().remove(item);
                                    return true;
                                }
                            });
                }
            }
        }

        OpenFragment f = getSelectedFragment();
        if (f != null)
            f.onPrepareOptionsMenu(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("ParserError")
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == null)
            return false;
        int id = item.getItemId();
        // if(id != R.id.title_icon_holder && id != android.R.id.home);
        // toggleBookmarks(false);
        OpenFragment f = getSelectedFragment();
        
        Logger.LogInfo("OpenExplorer.onOptionsItemSelected(" + item.getTitle() + ")");

        switch (id) {
            case R.id.menu_donate:
                launchUri(this, Uri.parse("http://brandroid.org/donate.php?ref=app_menu"));
                break;
            case R.id.menu_rate:
                launchReviews();
                break;
            case R.id.title_icon_holder:
            case android.R.id.home:
                setSetting("pref_show_bookmarks", toggleBookmarks());
                return true;

            case R.id.menu_view_grid:
                changeViewMode(OpenExplorer.VIEW_GRID, true);
                return true;

            case R.id.menu_view_list:
                changeViewMode(OpenExplorer.VIEW_LIST, true);
                return true;

            case R.id.menu_view_fullscreen:
                getPreferences().setSetting("global", "pref_fullscreen",
                        !getPreferences().getSetting("global", "pref_fullscreen", false));
                goHome();
                return true;

            case R.id.title_ops:
                refreshOperations();
                BetterPopupWindow pw = mOpsFragment.getPopup();
                pw.showLikePopDownMenu();
                return true;

            case R.id.title_log:
                if (mLogFragment == null)
                    mLogFragment = new LogViewerFragment();
                sendToLogView(null, 0);
                showLogFrag(mLogFragment, true);
                return true;

                /*
                 * case R.id.menu_flush:
                 * ThumbnailCreator.flushCache(getApplicationContext(), true);
                 * OpenPath.flushDbCache(); goHome(); return true;
                 */

            case R.id.menu_refresh2:
            case R.id.menu_refresh:
                ContentFragment content = getDirContentFragment(true);
                if (content != null) {
                    if (IS_DEBUG_BUILD)
                        Logger.LogDebug("Refreshing " + content.getPath().getPath());
                    FileManager.removeOpenCache(content.getPath().getPath());
                    content.getPath().deleteFolderFromDb();
                    content.runUpdateTask(true);
                    changePath(content.getPath(), false, true);
                }
                mBookmarks.refresh((OpenApp)this);
                return true;

            case R.id.menu_settings:
                showPreferences(null);
                return true;

            case R.id.menu_search:
                // if (BEFORE_HONEYCOMB)
                // onSearchRequested();
                return true;

            case R.id.menu_multi_all_delete:
                DialogHandler.showConfirmationDialog(
                        this,
                        getResources().getString(
                                R.string.s_confirm_delete,
                                getClipboard().getCount() + " "
                                        + getResources().getString(R.string.s_files)),
                        getResources().getString(R.string.s_menu_delete_all),
                        new DialogInterface.OnClickListener() { // yes
                            public void onClick(DialogInterface dialog, int which) {
                                getEventHandler().deleteFile(getClipboard(), OpenExplorer.this,
                                        false);
                            }
                        });
                break;

            case R.id.menu_multi_all_clear:
                getClipboard().clear();
                return true;

            case R.id.menu_multi_all_copy:
                getClipboard().DeleteSource = false;
                getDirContentFragment(false).executeMenu(R.id.content_paste, null,
                        getDirContentFragment(false).getPath());
                break;

            case R.id.menu_multi_all_move:
                getClipboard().DeleteSource = true;
                getDirContentFragment(false).executeMenu(R.id.content_paste, null,
                        getDirContentFragment(false).getPath());
                break;

            case R.id.title_paste:
            case R.id.title_paste_icon:
            case R.id.title_paste_text:
            case R.id.content_paste:
                getClipboard().setCurrentPath(getCurrentPath());
                onClipboardDropdown(null);
                return true;

            case R.id.menu_about:
                DialogHandler.showAboutDialog(this);
                return true;

            case R.id.menu_exit:
                showExitDialog();
                return true;
        }

        // return super.onOptionsItemSelected(item);

        if (item.getSubMenu() != null) {
            onPrepareOptionsMenu(item.getSubMenu());
            View anchor = findViewById(item.getItemId());
            if (anchor == null && item.getActionView() != null)
                anchor = item.getActionView();
            if (anchor == null) {
                anchor = mBar.getCustomView();
                if (anchor.findViewById(item.getItemId()) != null)
                    anchor = anchor.findViewById(item.getItemId());
            }
            if (anchor == null)
                anchor = mToolbarButtons;
            if (anchor == null)
                anchor = findViewById(android.R.id.home);
            if (anchor == null && USE_ACTION_BAR)
                anchor = mBar.getCustomView().findViewById(android.R.id.home);
            if (anchor == null)
                anchor = getCurrentFocus().getRootView();
            if (f != null)
                if (f.onClick(item.getItemId(), anchor))
                    return true;
        }

        if (item.isCheckable())
            item.setChecked(item.getGroupId() > 0 ? true : !item.isChecked());

        if (f != null && f.onOptionsItemSelected(item))
            return true;

        return onClick(item.getItemId(), item, null);
    }

    private void showExitDialog() {
        DialogHandler.showConfirmationDialog(this, getString(R.string.s_alert_exit),
                getString(R.string.s_menu_exit), getPreferences(), "exit", true,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null)
                            dialog.dismiss();
                        finish();
                    }
                });
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        if (v == null)
            return;
        int id = v.getId();

        if (id == R.id.title_icon_holder)
            setSetting("pref_show_bookmarks", toggleBookmarks());

        if (id == R.id.title_paste_icon) {
            onClipboardDropdown(v);
            return;
        }

        OpenFragment f = getSelectedFragment();
        if (f != null && f.onClick(id, v))
            return;
        if (v.getTag() != null && v.getTag() instanceof MenuItem
                && id != ((MenuItem)v.getTag()).getItemId()) {
            id = ((MenuItem)v.getTag()).getItemId();
            if (f.onClick(id, v))
                return;
            if (MenuUtils.getMenuLookupID(id) > -1
                    && showMenu(MenuUtils.getMenuLookupSub(id), v, true))
                return;
        }
        if (!v.showContextMenu())
            onClick(id, (MenuItem)null, v);
    }

    public boolean onClick(int id, MenuItem item, View from) {
        super.onClick(id);
        if (from == null || !from.isShown())
            from = findViewById(id);

        switch (id) {
            case R.id.title_log:
                showLogFrag(mLogFragment, true);
                break;
            case R.id.title_ops:
                BetterPopupWindow op = mOpsFragment.getPopup();
                if (op != null)
                    op.showLikePopDownMenu();
                break;
        }

        // showToast("oops");
        return false;
        // return super.onOptionsItemSelected(item);
    }

    private void showLogFrag(OpenFragment frag, boolean toggle) {
        View frag_log = findViewById(R.id.frag_log);
        ViewUtils.setViewsVisible(this, true, R.id.title_log);
        if (frag_log == null) {
            BetterPopupWindow pw = ((Poppable)frag).getPopup();
            if (!pw.hasShown() || toggle)
                pw.showLikePopDownMenu();
        } else {
            boolean isVis = frag_log.getVisibility() == View.VISIBLE;
            boolean isFragged = false;
            Fragment fl = fragmentManager.findFragmentById(R.id.frag_log);
            if (isVis && (fl != null && fl.equals(frag)))
                isFragged = true;
            if (isFragged) {
                if (toggle) {
                    Logger.LogDebug("OpenExplorer.showLogFrag : Toggling " + frag.getTitle());
                    ViewUtils.setViewsVisible(frag_log, !isVis);
                } else
                    Logger.LogDebug("OpenExplorer.showLogFrag : Doing nothing for "
                            + frag.getTitle());
            } else if (isVis) {
                Logger.LogDebug("OpenExplorer.showLogFrag : Adding " + frag.getTitle());
                fragmentManager.beginTransaction().replace(R.id.frag_log, frag)
                        .disallowAddToBackStack().commitAllowingStateLoss();
                ViewUtils.setViewsVisible(frag_log, true);
            } else {
                Logger.LogDebug("OpenExplorer.showLogFrag : Showing " + frag.getTitle());
                ViewUtils.setViewsVisible(frag_log, true);
            }
        }
    }

    private void debugTest() {
        //startActivity(new Intent(this, Authenticator.class));
        DEBUG_TOGGLE = !DEBUG_TOGGLE;
        notifyPager();
    }

    public boolean isSinglePane() {
        return mSinglePane;
    }

    private void onClipboardDropdown(View anchor) {
        ViewUtils.setViewsVisible(mStaticButtons, true, R.id.title_paste);
        if (anchor == null)
            anchor = ViewUtils.getFirstView(this, R.id.title_paste, R.id.title_icon_holder,
                    R.id.frag_holder);

        final BetterPopupWindow clipdrop = new BetterPopupWindow(this, anchor);
        View root = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.clipboard_layout, null);
        final TextView tvStatus = (TextView)root.findViewById(R.id.multiselect_status);
        tvStatus.setText(getClipboard().size() + " " + getString(R.string.s_files) + " :: "
                + OpenPath.formatSize(getClipboard().getTotalSize()));
        GridView mGridCommands = (GridView)root.findViewById(R.id.multiselect_command_grid);
        final ListView mListClipboard = (ListView)root.findViewById(R.id.multiselect_item_list);
        mListClipboard.setAdapter(getClipboard());
        mClipboard.setClipboardUpdateListener(new OnClipboardUpdateListener() {
            @Override
            public void onClipboardUpdate() {
                tvStatus.setText(getClipboard().size() + " " + getString(R.string.s_files) + " :: "
                        + OpenPath.formatSize(getClipboard().getTotalSize()));
                OpenExplorer.this.onClipboardUpdate();
            }
        });
        mListClipboard.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> list, View view, final int pos, long id) {
                // OpenPath file = mClipboard.get(pos);
                // if(file.getParent().equals(mLastPath))
                int animTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
                Animation anim = AnimationUtils.loadAnimation(OpenExplorer.this,
                        R.anim.slide_out_left);
                anim.setDuration(animTime);
                // anim.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
                view.startAnimation(anim);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        getClipboard().remove(pos);
                        getClipboard().notifyDataSetChanged();
                        if (getClipboard().getCount() == 0)
                            clipdrop.dismiss();
                    }
                }, animTime);
            }
        });
        final Menu menu = IconContextMenu.newMenu(this, R.menu.multiselect);
        MenuUtils.setMenuChecked(menu, mActionMode != null, R.id.menu_multi);
        MenuUtils.setMenuVisible(menu, getClipboard().hasPastable(), R.id.menu_multi_all_copy,
                R.id.menu_multi_all_copy, R.id.menu_multi_all_move);
        final IconContextMenuAdapter cmdAdapter = new IconContextMenuAdapter(this, menu);
        cmdAdapter.setTextLayout(R.layout.context_item);
        mGridCommands.setAdapter(cmdAdapter);
        mGridCommands.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
                MenuItem item = menu.getItem(pos);
                if (!onOptionsItemSelected(item))
                    onClick(item.getItemId(), item, view);
                clipdrop.dismiss();
            }
        });

        float w = getResources().getDimension(R.dimen.popup_width) * (3 / 2);
        if (w > getWindowWidth())
            w = getWindowWidth() - 20;
        clipdrop.setPopupWidth((int)w);
        clipdrop.setContentView(root);

        clipdrop.showLikePopDownMenu();
        // dropdown.setAdapter(this, new IconContextMenuAdapter(context, menu))
        // BetterPopupWindow win = new BetterPopupWindow(this, anchor);
        // ListView list =
        // ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.multiselect,
        // null);
        // win.setContentView(root)
    }

    public OpenPath getCurrentPath() {
        OpenFragment f = getSelectedFragment();
        if (f instanceof OpenPathFragmentInterface)
            return ((OpenPathFragmentInterface)f).getPath();
        return null;
    }

    public void changeViewMode(int newView, boolean doSet) {
        int mViewMode = getSetting(getCurrentPath(), "view", 0);
        if (mViewMode == newView) {
            Logger.LogWarning("changeViewMode called unnecessarily! " + newView + " = " + mViewMode);
            // return;
        }
        // Logger.LogVerbose("Changing view mode to " + newView);
        int oldView = mViewMode;
        if (newView == VIEW_CAROUSEL && !CAN_DO_CAROUSEL)
            newView = oldView;
        // setViewMode(newView);
        if (doSet)
            setSetting(getCurrentPath(), "view", newView);
        if (!mSinglePane) {
            ContentFragment cf = getDirContentFragment(true);
            if (cf != null)
                cf.onViewChanged(newView);
            invalidateOptionsMenu();
        } else if (oldView == VIEW_CAROUSEL && newView != VIEW_CAROUSEL && CAN_DO_CAROUSEL) {
            if (IS_DEBUG_BUILD)
                Logger.LogDebug("Switching from carousel!");
            if (mViewPagerEnabled) {
                setViewVisibility(true, false, R.id.content_frag);
                setViewVisibility(false, false, R.id.content_pager_frame_stub, R.id.content_pager,
                        R.id.content_pager_indicator);
                changePath(getCurrentPath(), false, true);
            } else {
                fragmentManager
                        .beginTransaction()
                        .replace(
                                R.id.content_frag,
                                ContentFragment.getInstance(getCurrentPath(), mViewMode,
                                        getSupportFragmentManager()))
                        .setBreadCrumbTitle(getCurrentPath().getAbsolutePath())
                        // .addToBackStack(null)
                        .commit();
                updateTitle(getCurrentPath().getPath());
            }

            invalidateOptionsMenu();
        } else {
            getDirContentFragment(true).onViewChanged(newView);
            invalidateOptionsMenu();
        }
    }

    public void showPreferences(OpenPath path) {
        Intent intent = new Intent(this, SettingsActivity.class);
        if (path != null)
            intent.putExtra("path", path.getPath());
        startActivityForResult(intent, REQ_PREFERENCES);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Logger.LogInfo("OpenExplorer.onKeyUp(" + keyCode + "," + event + ")");
        if (event.getAction() != KeyEvent.ACTION_UP)
            return super.onKeyUp(keyCode, event);
        if (MenuUtils.getMenuShortcut(event) != null) {
            if (getCurrentFocus() != null) {
                View cf = getCurrentFocus();
                if (cf instanceof TextView)
                    return false;
            }
            MenuItem item = MenuUtils.getMenuShortcut(event);
            if (item != null)
                if (onOptionsItemSelected(item)) {
                    showToast(item.getTitle(), Toast.LENGTH_SHORT);
                    return true;
                }
        }
        if (keyCode == KeyEvent.KEYCODE_BOOKMARK) {
            OpenPath path = getDirContentFragment(false).getPath();
            if (mBookmarks.hasBookmark(path))
                addBookmark(path);
            else
                removeBookmark(path);
        } else if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            int pos = keyCode - KeyEvent.KEYCODE_1;
            if (mToolbarButtons != null) {
                if (pos < mToolbarButtons.getChildCount()
                        && mToolbarButtons.getChildAt(pos).performClick())
                    return true;
                return false;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU && (isNook() || isBlackBerry())) {
            try {
                @SuppressWarnings("rawtypes")
                Class cbar = ActionBar.class;
                @SuppressWarnings("unchecked")
                java.lang.reflect.Method m = cbar.getDeclaredMethod("showOverflowMenu",
                        (Class[])null);
                if (m != null)
                    m.invoke(getSupportActionBar());
            } catch (Exception e) {
                Logger.LogError("Unable to show Overflow");
            }
        }

        /*
         * if (keyCode == KeyEvent.KEYCODE_BACK) { if (mBackQuit) { return
         * super.onKeyUp(keyCode, event); } else { Toast.makeText(this,
         * "Press back again to quit", Toast.LENGTH_SHORT).show(); mBackQuit =
         * true; return true; } }
         */
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            refreshBookmarks();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.LogDebug("OpenExplorer.onActivityResult(" + requestCode + ", " + resultCode + ", "
                + (data != null ? data.toString() : "null") + ")");
        if (requestCode == REQ_PREFERENCES) {
            boolean needRestart = getPreferences().getSetting("global", "restart", false);
            if (resultCode == RESULT_RESTART_NEEDED
                    || (data != null && data.hasExtra("restart") && data.getBooleanExtra("restart",
                            true)) || needRestart) {
                getPreferences().setSetting("global", "restart", false);
                showToast(R.string.s_alert_restart);
                goHome(); // just restart
            } else {
                loadPreferences();
                refreshBookmarks();
                notifyPager();
                getDirContentFragment(false).refreshData();
                if (!mSinglePane)
                    toggleBookmarks(
                            getSetting(null, "pref_show_bookmarks",
                                    getResources().getBoolean(R.bool.large)), false);
                invalidateOptionsMenu();
            }
        } else if (requestCode == REQ_SPLASH) {
            if (resultCode == RESULT_OK && data != null && data.hasExtra("start")) {
                String start = data.getStringExtra("start");
                getPreferences().setSetting("global", "pref_splash", true);
                getPreferences().setSetting("global", "pref_start", start);
                if (!start.equals(getCurrentPath().getPath())) {
                    if ("Videos".equals(start))
                        changePath(getVideoParent(), true);
                    else if ("Photos".equals(start))
                        changePath(mPhotoParent, true);
                    else if ("External".equals(start))
                        changePath(OpenFile.getExternalMemoryDrive(true).setRoot(), true);
                    else if ("Internal".equals(start))
                        changePath(OpenFile.getInternalMemoryDrive().setRoot(), true);
                    else
                        changePath(new OpenFile("/").setRoot(), true);
                }
            }
        } else if (requestCode == REQ_INTENT) {
        } else if (requestCode == REQ_SERVER_MODIFY || requestCode == REQ_SERVER_NEW) {
            refreshBookmarks();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
            if (getSelectedFragment() != null)
                getSelectedFragment().onActivityResult(requestCode, resultCode, data);
        }
    }

    public void goBack() {
        // new FileIOTask().execute(new FileIOCommand(FileIOCommandType.PREV,
        // mFileManager.getLastPath()));
        if (fragmentManager.getBackStackEntryCount() == 0)
            return;
        BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager
                .getBackStackEntryCount() - 1);
        OpenPath last = null;
        if (entry != null && entry.getBreadCrumbTitle() != null)
            try {
                last = FileManager.getOpenCache(entry.getBreadCrumbTitle().toString(), false,
                        OpenPath.Sorting);
            } catch (IOException e) {
            }
        if (last == null)
            return;
        Logger.LogDebug("Going back to " + last.getPath());
        changePath(last, false, true);
        // updateData(last.list());
    }

    @Override
    public void onBackPressed() {
        if (mViewPagerAdapter.getCount() > 0 && getSelectedFragment().onBackPressed())
            return;
        try {
            super.onBackPressed();
        } catch (Exception e) {
            Logger.LogWarning("Bad back?", e);
        }
    }

    public void onBackStackChanged() {
        int i = fragmentManager.getBackStackEntryCount();
        final Boolean isBack = i < mLastBackIndex;
        BackStackEntry entry = null;
        if (i > 0)
            entry = fragmentManager.getBackStackEntryAt(i - 1);
        Logger.LogDebug("Back Stack " + i
                + (entry != null ? ": " + entry.getBreadCrumbTitle() : ""));
        if (isBack) {
            if (i > 0) {
                if (entry != null && entry.getBreadCrumbTitle() != null) {
                    try {
                        mLastPath = FileManager.getOpenCache(
                                entry.getBreadCrumbTitle().toString(), true, OpenPath.Sorting);
                    } catch (Exception e) {
                        Logger.LogError("Couldn't get back cache.", e);
                    }
                    if (mLastPath != null) {
                        Logger.LogDebug("last path set to " + mLastPath.getPath());
                        changePath(mLastPath, false, true);
                        // updateTitle(mLastPath.getPath());
                    } else
                        showExitDialog();

                } else {
                    fragmentManager.popBackStack();
                }
            } else {
                try {
                    showExitDialog();
                } catch (Exception e) {
                    Logger.LogWarning("Unable to show exit dialog.", e);
                }
            }
        }
        mLastBackIndex = i;
    }

    private void changePath(OpenPath path, Boolean addToStack) {
        changePath(path, addToStack, false);
    }

    private boolean needsDisconectDuringChange(OpenPath from, OpenPath to)
    {
        if (to == null)
            return true;
        if (!to.getClass().equals(from.getClass()))
            return true;
        return ((OpenNetworkPath)from).getServerIndex() != ((OpenNetworkPath)to).getServerIndex();
    }

    private void changePath(OpenPath path, Boolean addToStack, Boolean force) {
        try {
            // if(mLastPath != null && !mLastPath.equals(path) && mLastPath
            // instanceof OpenNetworkPath)
            // ((OpenNetworkPath)mLastPath).disconnect();
        } catch (Exception e) {
            Logger.LogError("Couldn't disconnect while changing paths.", e);
        }
        toggleBookmarks(false);
        if (path == null)
            path = mLastPath;
        if (path == null)
            return;
        if (mLastPath == null && getDirContentFragment(false) != null)
            mLastPath = getDirContentFragment(false).getPath();
        if (!(mLastPath instanceof OpenFile) || !(path instanceof OpenFile))
            force = true;

        if (mLastPath instanceof OpenNetworkPath.PipeNeeded)
            if (needsDisconectDuringChange(mLastPath, path))
                ((PipeNeeded)mLastPath).disconnect();

        onClipboardUpdate();

        // if(!BEFORE_HONEYCOMB) force = true;
        // if(!force)
        // if(!addToStack && path.getPath().equals("/")) return;
        // if(mLastPath.getPath().equalsIgnoreCase(path.getPath())) return;
        int newView = getSetting(path, "view", 0);
        if (!CAN_DO_CAROUSEL && newView == VIEW_CAROUSEL) {
            setSetting(path, "view", VIEW_LIST);
            newView = VIEW_LIST;
        }
        getSetting(mLastPath, "view", 0);

        if (path instanceof OpenNetworkPath.PipeNeeded) {
            if (mLogFragment != null && mLogFragment.getPopup() == null)
                initLogPopup();
            if (mLogViewEnabled && mLogFragment != null && !mLogFragment.isAdded())
                showLogFrag(mLogFragment, false);
        } else if(!(path instanceof OpenNetworkPath))
            setViewVisibility(false, false, R.id.frag_log);

        /*
         * final ImageView icon = (ImageView)findViewById(R.id.title_icon);
         * if(icon != null) ThumbnailCreator.setThumbnail(this, icon, path, 96,
         * 96, new OnUpdateImageListener() { public void updateImage(Bitmap b) {
         * BitmapDrawable d = new BitmapDrawable(getResources(), b);
         * d.setGravity(Gravity.CENTER); icon.setImageDrawable(d); } });
         */

        // mFileManager.setShowHiddenFiles(getSetting(path, "hide", false));
        // setViewMode(newView);
        // if(!BEFORE_HONEYCOMB && Build.VERSION.SDK_INT < 14 && newView ==
        // VIEW_CAROUSEL) {

        // setViewVisibility(true, false, R.id.content_pager_frame);
        // setViewVisibility(false, false, R.id.content_frag);

        List<OpenPath> familyTree = path.getAncestors(true);

        if (addToStack) {
            int bsCount = fragmentManager.getBackStackEntryCount();
            String last = null;
            if (bsCount > 0) {
                BackStackEntry entry = fragmentManager.getBackStackEntryAt(bsCount - 1);
                last = entry.getBreadCrumbTitle() != null ? entry.getBreadCrumbTitle().toString()
                        : "";
                if (IS_DEBUG_BUILD)
                    Logger.LogDebug("Changing " + last + " to " + path.getPath() + "? "
                            + (last.equalsIgnoreCase(path.getPath()) ? "No" : "Yes"));
            } else if (IS_DEBUG_BUILD)
                Logger.LogDebug("First changePath to " + path.getPath());
            String ap = path.getAbsolutePath();
            if (mStateReady && (last == null || !last.equalsIgnoreCase(ap))) {
                fragmentManager.beginTransaction().setBreadCrumbTitle(ap)
                        .addToBackStack("path").commit();
            }
        }
        final OpenFragment cf = ContentFragment.getInstance(path, newView,
                getSupportFragmentManager());

        if (force || addToStack || path.requiresThread()) {
            int common = 0;

            for (int i = mViewPagerAdapter.getCount() - 1; i >= 0; i--) {
                OpenFragment f = mViewPagerAdapter.getItem(i);
                if (f == null || !(f instanceof ContentFragment))
                    continue;
                OpenPath tp = ((ContentFragment)f).getPath();
                if (tp instanceof OpenSmartFolder || !familyTree.contains(tp)) {
                    Logger.LogDebug("Removing from View Pager Adapter: " + tp);
                    mViewPagerAdapter.remove(i);
                    // removed = true;
                } else
                    common++;
            }

            if (common < 0)
                mViewPagerAdapter.add(cf);
            else
                mViewPagerAdapter.add(common, cf);

            ArrayList<OpenPath> vpaPaths = new ArrayList<OpenPath>();
            for (OpenFragment frag : mViewPagerAdapter.getFragments())
                if (frag instanceof OpenPathFragmentInterface)
                    vpaPaths.add(((OpenPathFragmentInterface)frag).getPath());

            OpenPath tmp = path.getParent();
            while (tmp != null) {
                ContentFragment cft = ContentFragment.getInstance(tmp);
                if (!vpaPaths.contains(tmp)) {
                    Logger.LogDebug("Adding Parent: " + tmp);
                    mViewPagerAdapter.add(common, cft);
                    vpaPaths.add(common, tmp);
                }
                tmp = tmp.getParent();
            }

            setViewPageAdapter(mViewPagerAdapter, false); // DONE: Sped up app
                                                          // considerably!

            int index = vpaPaths.indexOf(path);
            if (index >= 0)
                setCurrentItem(index, addToStack);
            else
                Logger.LogDebug("No need to set index to 0");
        } else {
            OpenPath commonBase = null;
            for (int i = mViewPagerAdapter.getCount() - 1; i >= 0; i--) {
                if (!(mViewPagerAdapter.getItem(i) instanceof ContentFragment))
                    continue;
                ContentFragment c = (ContentFragment)mViewPagerAdapter.getItem(i);
                if (path.getPath().startsWith(c.getPath().getPath()))
                    continue;
                commonBase = ((ContentFragment)mViewPagerAdapter.remove(i)).getPath();
            }
            int depth = 0;
            if (commonBase != null)
                depth = commonBase.getDepth() - 1;
            OpenPath tmp = path;
            while (tmp != null && (commonBase == null || !tmp.equals(commonBase))) {
                mViewPagerAdapter.add(depth, cf);
                tmp = tmp.getParent();
                if (tmp == null)
                    break;
            }
            setViewPageAdapter(mViewPagerAdapter, false);
            // mViewPager.setAdapter(mViewPagerAdapter);
            setCurrentItem(path.getDepth() - 1, false);
            // getDirContentFragment(false).refreshData(null, false);
        }
        // }
        // refreshContent();
        // invalidateOptionsMenu();
        /*
         * if(content instanceof ContentFragment)
         * ((ContentFragment)content).setSettings( SortType.DATE_DESC,
         * getSetting(path, "thumbs", true), getSetting(path, "hide", true) );
         */
        if (path instanceof OpenFile && !path.requiresThread())
            EventHandler.execute(new PeekAtGrandKidsTask(), path);
        // ft.replace(R.id.content_frag, content);
        // ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        Logger.LogDebug("Setting path to " + path.getPath());
        mLastPath = path;
    }

    private String getFragmentPaths(List<OpenFragment> frags) {
        String ret = "";
        for (int i = 0; i < frags.size(); i++) {
            OpenFragment f = frags.get(i);
            if (f instanceof OpenPathFragmentInterface)
                ret += ((OpenPathFragmentInterface)f).getPath().getPath();
            else
                ret += f.getClassName();
            if (i < frags.size() - 1)
                ret += ",";
        }
        return ret;
    }

    @SuppressWarnings("deprecation")
    public void setLights(Boolean on) {
        try {
            View root = getCurrentFocus().getRootView();
            int vis = on ? View.STATUS_BAR_VISIBLE : View.STATUS_BAR_HIDDEN;
            if (root.getSystemUiVisibility() != vis)
                root.setSystemUiVisibility(vis);
        } catch (Exception e) {
        }
    }

    public void changePath(OpenPath path) {
        changePath(path, true, false);
    }

    public static final FileManager getFileManager() {
        return mFileManager;
    }

    public static final EventHandler getEventHandler() {
        return mEvHandler;
    }

    public class EnsureCursorCacheTask extends AsyncTask<OpenPath, Integer, Integer> {
        @Override
        protected Integer doInBackground(OpenPath... params) {
            // int done = 0;
            final Context c = getApplicationContext();
            for (OpenPath path : params) {
                if (path.isDirectory()) {
                    try {
                        for (OpenPath kid : path.list()) {
                            ThumbnailCreator.generateThumb(OpenExplorer.this, kid, 36, 36, c);
                            ThumbnailCreator.generateThumb(OpenExplorer.this, kid, 128, 128, c);
                            // done++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ThumbnailCreator.generateThumb(OpenExplorer.this, path, 36, 36, c);
                    ThumbnailCreator.generateThumb(OpenExplorer.this, path, 128, 128, c);
                    // done++;
                }
            }
            // Logger.LogDebug("cursor cache of " + done + " generated.");
            return null;
        }

    }

    public void setOnClicks(int... ids) {
        for (int id : ids)
            if (findViewById(id) != null) {
                View v = findViewById(id);
                if (v.isLongClickable())
                    v.setOnLongClickListener(this);
                v.setOnClickListener(this);
            }
    }

    public static String getVolumeName(String sPath2) {
        Process mLog = null;
        BufferedReader reader = null;
        try {
            mLog = Runtime.getRuntime().exec(new String[] {
                    "logcat", "-d", "MediaVolume:D *:S"
            });
            reader = new BufferedReader(new InputStreamReader(mLog.getInputStream()));
            String check = sPath2.substring(sPath2.lastIndexOf("/") + 1);
            if (check.indexOf(".") > -1)
                check = check.substring(check.indexOf(".") + 1);
            String s = null;
            String last = null;
            do {
                s = reader.readLine();
                if (s == null)
                    break;
                if (s.indexOf("New volume - Label:[") > -1 && s.indexOf(check) > -1) {
                    last = s.substring(s.indexOf("[") + 1);
                    if (last.indexOf("]") > -1)
                        last = last.substring(0, last.indexOf("]"));
                }
            } while (s != null);
            if (last == null) {
                sPath2 = sPath2.substring(sPath2.lastIndexOf("/") + 1);
                if (sPath2.indexOf("_") > -1 && sPath2.indexOf("usb") < sPath2.indexOf("_"))
                    sPath2 = sPath2.substring(0, sPath2.indexOf("_"));
                else if (sPath2.indexOf("_") > -1 && sPath2.indexOf("USB") > sPath2.indexOf("_"))
                    sPath2 = sPath2.substring(sPath2.indexOf("_") + 1);
                sPath2 = sPath2.toUpperCase();
                return sPath2;
            }
            if (IS_DEBUG_BUILD)
                Logger.LogDebug("OpenExplorer.getVolumeName(" + sPath2 + ") = " + last);
            sPath2 = last;
        } catch (IOException e) {
            Logger.LogError("Couldn't read LogCat :(", e);
            sPath2 = sPath2.substring(sPath2.lastIndexOf("/") + 1);
            if (sPath2.indexOf("_") > -1 && sPath2.indexOf("usb") < sPath2.indexOf("_"))
                sPath2 = sPath2.substring(0, sPath2.indexOf("_"));
            else if (sPath2.indexOf("_") > -1 && sPath2.indexOf("USB") > sPath2.indexOf("_"))
                sPath2 = sPath2.substring(sPath2.indexOf("_") + 1);
            sPath2 = sPath2.toUpperCase();
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return sPath2;
    }

    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.title_icon_holder:
                goHome();
                return true;
            case R.id.title_ops:
                showToast(R.string.s_title_operations);
                ViewUtils.setViewsVisible(this, false, R.id.title_ops);
                checkTitleSeparator();
                return true;
            case R.id.title_log:
                showToast(R.string.s_pref_logview);
                ViewUtils.setViewsVisible(this, false, R.id.title_log);
                checkTitleSeparator();
                return true;

                /*
                 * case R.id.menu_favorites:
                 * MenuUtils.setMenuShowAsAction(mMainMenu,
                 * MenuItem.SHOW_AS_ACTION_NEVER, R.id.menu_favorites);
                 * if(BEFORE_HONEYCOMB) {
                 * mToolbarButtons.removeView(mToolbarButtons
                 * .findViewById(R.id.menu_favorites)); mMainMenu.add(0,
                 * R.id.menu_favorites, 0, R.string.s_menu_favorites)
                 * .setIcon(R.drawable.ic_favorites); } return true;
                 */
        }
        OpenFragment f = getSelectedFragment();
        if (f != null) {
            if (f.onLongClick(v))
                return true;
            Logger.LogDebug("No onLongClick?");
        }
        return false;
    }
    
    public void addBookmark(OpenPath file)
    {
        addBookmark(this, file, mBookmarkListener);
    }

    public static void addBookmark(OpenApp app, OpenPath file, OpenApp.OnBookMarkChangeListener mBookmarkListener) {
        Logger.LogDebug("Adding Bookmark: " + file.getPath());
        Preferences prefs = app.getPreferences();
        String sBookmarks = prefs.getSetting("bookmarks", "bookmarks", "");
        sBookmarks += (sBookmarks != "" ? ";" : "") + file.getPath();
        Logger.LogVerbose("Bookmarks: " + sBookmarks);
        prefs.setSetting("bookmarks", "bookmarks", sBookmarks);
        Intent intent = new Intent(OpenExplorer.INTENT_BROADCAST_ACTION);
        intent.putExtra("action", "bookmark");
        intent.putExtra("path", (Parcelable)file);
        app.getContext().sendBroadcast(intent);
        if (mBookmarkListener != null)
            mBookmarkListener.onBookMarkAdd(app, file);
    }

    public void removeBookmark(OpenPath file) {
        Logger.LogDebug("Removing Bookmark: " + file.getPath());
        String sBookmarks = ";" + getPreferences().getSetting("bookmarks", "bookmarks", "") + ";";
        sBookmarks = sBookmarks.replace(";" + file.getPath() + ";", "");
        if (sBookmarks.startsWith(";"))
            sBookmarks = sBookmarks.substring(1);
        if (sBookmarks.endsWith(";"))
            sBookmarks = sBookmarks.substring(0, sBookmarks.length() - 1);
        getPreferences().setSetting("bookmarks", "bookmarks", sBookmarks);
        refreshBookmarks();
    }

    public static void setOnBookMarkAddListener(
            OpenApp.OnBookMarkChangeListener bookmarkListener) {
        mBookmarkListener = bookmarkListener;
    }

    public class PeekAtGrandKidsTask extends AsyncTask<OpenPath, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(OpenPath... params) {
            int ret = 0;
            for (OpenPath file : params)
                if (file instanceof OpenFile)
                    ret += ((OpenFile)file).listFiles(true).length;
            return ret;
        }

    }

    public OpenPath getLastPath() {
        return mLastPath;
    }

    public View getRootView() {
        if (getCurrentFocus() != null)
            return getCurrentFocus().getRootView();
        else if (findViewById(android.R.id.home) != null)
            return findViewById(android.R.id.home).getRootView();
        else
            return null;
    }

    public void onClipboardUpdate() {
        if (getClipboard().size() == mLastClipSize)
            return;
        if (IS_DEBUG_BUILD)
            Logger.LogDebug("onClipboardUpdate(" + getClipboard().size() + ")");
        View pb = null;
        if (mStaticButtons != null)
            pb = mStaticButtons.findViewById(R.id.title_paste);
        mLastClipSize = getClipboard().size();
        TextView pbt = (TextView)pb.findViewById(R.id.title_paste_text);
        ViewUtils.setViewsVisible(pb, mLastClipSize > 0 || mLastClipState);
        // ViewUtils.setText(pb, "" + mLastClipSize, R.id.title_paste_text);
        if (pbt != null) {
            pbt.setText("" + mLastClipSize);
            pbt.setTextColor(getResources().getColor(
                    getThemedResourceId(R.styleable.AppTheme_dialogBackgroundColorPrimary,
                            R.color.white)));
        }
        ViewUtils.setImageResource(
                pb,
                getThemedResourceId(R.styleable.AppTheme_actionIconClipboard,
                        R.drawable.ic_menu_clipboard), R.id.title_paste_icon);
        checkTitleSeparator();
        // invalidateOptionsMenu();

        ContentFragment cf = getDirContentFragment(false);
        if (cf != null && cf.isAdded() && cf.isVisible())
            cf.notifyDataSetChanged();
    }

    @Override
    public boolean onPageTitleLongClick(int position, View titleView) {
        try {
            OpenFragment f = mViewPagerAdapter.getItem(position);
            if (f instanceof OnFragmentTitleLongClickListener)
                return ((OnFragmentTitleLongClickListener)f).onTitleLongClick(titleView);
            if (f instanceof TextEditorFragment)
                return false;
            if (!(f instanceof ContentFragment))
                return false;
            OpenPath path = ((ContentFragment)mViewPagerAdapter.getItem(position)).getPath();
            if (path.requiresThread())
                return false;
            OpenPath parent = path.getParent();
            if (path instanceof OpenCursor)
                parent = new OpenPathArray(new OpenPath[] {
                        mVideoParent, mPhotoParent, mMusicParent, mDownloadParent
                });
            if (parent == null)
                parent = new OpenPathArray(new OpenPath[] {
                        path
                });
            ArrayList<OpenPath> arr = new ArrayList<OpenPath>();
            for (OpenPath kid : parent.list())
                if ((path.equals(kid) || kid.isDirectory()) && !kid.isHidden())
                    arr.add(kid);
            Collections.sort(arr, new Comparator<OpenPath>() {
                public int compare(OpenPath a, OpenPath b) {
                    return a.getName().compareTo(b.getName());
                }
            });
            OpenPath[] siblings = arr.toArray(new OpenPath[arr.size()]);
            ArrayList<OpenPath> siblingArray = new ArrayList<OpenPath>();
            siblingArray.addAll(arr);
            OpenPath foster = new OpenPathArray(siblings);
            // Logger.LogVerbose("Siblings of " + path.getPath() + ": " +
            // siblings.length);

            Context mContext = this;
            View anchor = titleView; // findViewById(R.id.title_bar);
            int[] offset = new int[2];
            titleView.getLocationInWindow(offset);
            int offsetX = 0;
            int arrowLeft = 0;
            if (anchor == null && findViewById(R.id.content_pager_indicator) != null) {
                offsetX = titleView.getLeft();
                arrowLeft += titleView.getWidth() / 2;
                Logger.LogDebug("Using Pager Indicator as Sibling anchor (" + offsetX + ")");
                anchor = findViewById(R.id.content_pager_indicator);
                // if(anchor != null)
                // offsetX -= anchor.getLeft();
            }
            final BetterPopupWindow mSiblingPopup = new BetterPopupWindow(mContext, anchor);
            // mSiblingPopup.USE_INDICATOR = false;
            OpenPathList mSiblingList = new OpenPathList(foster, this);
            mSiblingList.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                    final OpenPath path = (OpenPath)((BaseAdapter)arg0.getAdapter()).getItem(pos);
                    mSiblingPopup.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            changePath(path, true);
                        }
                    });
                    mSiblingPopup.dismiss();
                }
            });
            mSiblingPopup.setContentView(mSiblingList);
            mSiblingPopup.setAnchorOffset(arrowLeft);
            mSiblingPopup.showLikePopDownMenu(offsetX, 0);
            return true;
        } catch (Exception e) {
            Logger.LogError("Couldn't show sibling dropdown", e);
        }
        return false;
    }

    @Override
    public void onWorkerThreadComplete(EventType type, String... results) {
        try {
            Thread.sleep(50);
            Logger.LogVerbose("Time to wake up!");
        } catch (InterruptedException e) {
            Logger.LogWarning("Woken up too early!");
        }
        ContentFragment frag = getDirContentFragment(false);
        if (frag != null) {
            frag.onWorkerThreadComplete(type, results);
            // changePath(frag.getPath(), false, true);
        }
        if (getClipboard().ClearAfter)
            getClipboard().clear();
    }

    @Override
    public void onWorkerProgressUpdate(int pos, int total) {
        ContentFragment frag = getDirContentFragment(false);
        if (frag != null)
            frag.onWorkerProgressUpdate(pos, total);
    }

    @Override
    public void onWorkerThreadFailure(EventType type, OpenPath... files) {
        String[] paths = new String[files.length];
        for(int i = 0; i < paths.length; i++)
            paths[i] = files[i].getAbsolutePath();
        sendToLogView(type.name() + " error on " + Utils.joinArray(paths, " :: "), Color.RED);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
        CursorLoader loader = null;
        int flag = (int)Math.pow(2, id + 1);
        if ((OpenCursor.LoadedCursors & flag) == flag)
            return null;
        switch (id) {
            case 0: // videos
                loader = new CursorLoader(getApplicationContext(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        // Uri.parse("content://media/external/video/media"),
                        bRetrieveExtraVideoDetails ? new String[] {
                                "_id", "_display_name", "_data", "_size", "date_modified",
                                MediaStore.Video.VideoColumns.RESOLUTION,
                                MediaStore.Video.VideoColumns.DURATION
                        } : new String[] {
                                "_id", "_display_name", "_data", "_size", "date_modified"
                        }, MediaStore.Video.Media.SIZE + " > 10000", null,
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC, "
                                + MediaStore.Video.Media.DATE_MODIFIED + " DESC");
                if (bRetrieveExtraVideoDetails)
                    try {
                        loader.loadInBackground();
                    } catch (SQLiteException e) {
                        bRetrieveExtraVideoDetails = false;
                        setSetting("tag_novidinfo", true);
                        return onCreateLoader(id, arg1);
                    }
                break;
            case 1: // images
                loader = new CursorLoader(getApplicationContext(),
                        Uri.parse("content://media/external/images/media"),
                        bRetrieveDimensionsForPhotos ? // It seems that < 2.3.3
                                                       // don't have width &
                                                       // height
                        new String[] {
                                "_id", "_display_name", "_data", "_size", "date_modified", "width",
                                "height"
                        }
                                : new String[] {
                                        "_id", "_display_name", "_data", "_size", "date_modified"
                                }, MediaStore.Images.Media.SIZE + " > 10000", null,
                        MediaStore.Images.Media.DATE_ADDED + " DESC");
                if (bRetrieveDimensionsForPhotos) {
                    try {
                        loader.loadInBackground();
                    } catch (SQLiteException e) {
                        bRetrieveDimensionsForPhotos = false;
                        setSetting("tag_nodims", true);
                        return onCreateLoader(id, arg1);
                    }
                }
                break;
            case 2: // music
                loader = new CursorLoader(getApplicationContext(),
                        Uri.parse("content://media/external/audio/media"), new String[] {
                                "_id", "_display_name", "_data", "_size", "date_modified",
                                MediaStore.Audio.AudioColumns.DURATION
                        }, MediaStore.Audio.Media.SIZE + " > 10000", null,
                        MediaStore.Audio.Media.DATE_ADDED + " DESC");
                break;
            case 3: // apks
                if (bRetrieveCursorFiles)
                    loader = new CursorLoader(getApplicationContext(),
                            MediaStore.Files.getContentUri(OpenFile.getExternalMemoryDrive(true)
                                    .getParent().getPath()), new String[] {
                                    "_id", "_display_name", "_data", "_size", "date_modified"
                            }, "_size > 10000 AND _data LIKE '%apk'", null, "date modified DESC");
                break;
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> l, Cursor c) {
        if (c == null)
            return;
        int flag = (int)Math.pow(2, l.getId() + 1);
        if ((OpenCursor.LoadedCursors & flag) == flag) {
            c.close();
            return;
        }
        OpenCursor.LoadedCursors |= flag;
        Logger.LogVerbose("LoadedCursors: " + OpenCursor.LoadedCursors);
        OpenCursor mParent = mVideoParent;
        if (l.getId() == 1)
            mParent = mPhotoParent;
        else if (l.getId() == 2)
            mParent = mMusicParent;
        else if (l.getId() == 3)
            mParent = mApkParent;

		try {
	        mParent.setCursor(c);
	    } catch(android.database.CursorIndexOutOfBoundsException cex) {
	    	Logger.LogError("Unable to set parent cursor.", cex);
	    }

        if (l.getId() == 0)
            try {
                mVideosMerged.refreshKids();
            } catch (IOException e) {
                Logger.LogError("Unable to merge videos after Cursor", e);
            }
        else if (l.getId() == 1)
            try {
                mPhotosMerged.refreshKids();
            } catch (IOException e) {
                Logger.LogError("Unable to merge photos after Cursor", e);
            }
        /*
         * mBookmarks.refresh(); OpenFragment f = getSelectedFragment(); if(f
         * instanceof ContentFragment &&
         * ((ContentFragment)f).getPath().equals(mParent))
         * ((ContentFragment)f).refreshData(null, false);
         */
    }

    @Override
    public void onLoaderReset(Loader<Cursor> l) {
        onLoadFinished(l, null);
    }

    public void setProgressVisibility(boolean visible) {
        setProgressBarIndeterminateVisibility(visible);
        ViewUtils.setViewsVisible(this, visible, R.id.title_progress);
    }

    public void removeFragment(OpenFragment frag) {
        setCurrentItem(mViewPagerAdapter.getCount() - 1, false);
        if (!mViewPagerAdapter.remove(frag))
            Logger.LogWarning("Unable to remove fragment");
        setViewPageAdapter(mViewPagerAdapter, false);
        // refreshContent();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageSelected(int position) {
        Logger.LogDebug("onPageSelected(" + position + ")");
        final OpenFragment f = getSelectedFragment();
        if (f == null)
            return;
        if (!f.isDetached()) {
            invalidateOptionsMenu();
            new Thread(new Runnable() {
                public void run() {
                    final ImageView icon = (ImageView)findViewById(R.id.title_icon);
                    final Drawable d = f.getIcon();
                    if (d == null && f instanceof OpenPathFragmentInterface) {
                        OpenPath path = ((OpenPathFragmentInterface)f).getPath();
                        ThumbnailCreator.setThumbnail(OpenExplorer.this, icon, path, 96, 96,
                                new OnUpdateImageListener() {
                                    public void updateImage(Bitmap b) {
                                        final BitmapDrawable bd = new BitmapDrawable(
                                                getResources(), b);
                                        bd.setGravity(Gravity.CENTER);
                                        post(new Runnable() {
                                            public void run() {
                                                ImageUtils.fadeToDrawable(icon, bd);
                                            }
                                        });
                                    }
                                });
                    } else if (d != null)
                        post(new Runnable() {
                            public void run() {
                                ImageUtils.fadeToDrawable(icon, d);
                            }
                        });
                }
            }).start();
        }
        // if((f instanceof ContentFragment) && (((ContentFragment)f).getPath()
        // instanceof OpenNetworkPath)) ((ContentFragment)f).refreshData(null,
        // false);
    }

    public void notifyPager() {
        mViewPager.post(new Runnable() {
            public void run() {
                mViewPager.notifyDataSetChanged();
            }
        });
    }

    public static Handler getHandler() {
        return mHandler;
    }

    public static void post(Runnable r) {
        getHandler().post(r);
    }

    public OpenApplication getOpenApplication() {
        return (OpenApplication)getApplication();
    }

    @Override
    public DataManager getDataManager() {
        return getOpenApplication().getDataManager();
    }

    @Override
    public ImageCacheService getImageCacheService() {
        return getOpenApplication().getImageCacheService();
    }

    @Override
    public DownloadCache getDownloadCache() {
        return getOpenApplication().getDownloadCache();
    }

    @Override
    public ThreadPool getThreadPool() {
        return getOpenApplication().getThreadPool();
    }

    @Override
    public LruCache<String, Bitmap> getMemoryCache() {
        return getOpenApplication().getMemoryCache();
    }

    @Override
    public DiskLruCache getDiskCache() {
        return getOpenApplication().getDiskCache();
    }

    @Override
    public Context getContext() {
        return super.getContext();
    }

    public ShellSession getShellSession() {
        return getOpenApplication().getShellSession();
    }

    @Override
    public void onIconContextItemSelected(IconContextMenu menu, MenuItem item, Object info,
            View view) {
        if (menu != null)
            menu.dismiss();
        if (onClick(item.getItemId(), item, view))
            return;
        if (onOptionsItemSelected(item))
            return;
        int index = Utils.getArrayIndex(MenuUtils.MENU_LOOKUP_IDS, item.getItemId());
        if (index > -1)
            showMenu(MenuUtils.getMenuLookupSub(index), view, true);
        else
            onClick(item.getItemId());
    }

    public boolean onIconContextItemSelected(PopupMenu menu, MenuItem item,
            ContextMenuInfo menuInfo, View view) {
        if (menu != null)
            menu.dismiss();
        if (onClick(item.getItemId(), item, view))
            return true;
        if (onOptionsItemSelected(item))
            return true;
        int index = Utils.getArrayIndex(MenuUtils.MENU_LOOKUP_IDS, item.getItemId());
        if (index > -1)
            return showMenu(MenuUtils.getMenuLookupSub(index), view, true);
        else {
            View v = findViewById(item.getItemId());
            if (v != null && v.isClickable()) {
                onClick(v);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (v == null)
            return false;
        Logger.LogInfo("OpenExplorer.onKey(" + keyCode + "," + event + ") on " + v);
        if (event.getAction() != KeyEvent.ACTION_UP)
            return false;
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (MenuUtils.getMenuLookupID(v.getId()) > 0)
                if (showMenu(MenuUtils.getMenuLookupSub(v.getId()), v, false))
                    return true;
        }
        return false;
    }

    public boolean onFragmentDPAD(OpenFragment frag, boolean toRight) {
        int pos = mViewPagerAdapter.getItemPosition(frag);
        Logger.LogDebug("onFragmentDPAD(" + pos + "," + (toRight ? "RIGHT" : "LEFT") + ")");
        if (toRight) {
            if (frag.getTitleView() != null)
                if (frag.getTitleView().requestFocus())
                    return true;
            if (ViewUtils.requestFocus(this, findViewById(R.id.content_frag).getNextFocusRightId(),
                    R.id.log_clear, android.R.id.home, R.id.menu_search))
                return true;
        } else if (!toRight) {
            if (frag.getTitleView() != null)
                if (frag.getTitleView().requestFocus())
                    return true;
            if (findViewById(R.id.menu_search) != null)
                if (findViewById(findViewById(R.id.menu_search).getNextFocusLeftId())
                        .requestFocus())
                    return true;
            if (ViewUtils.requestFocus(this, findViewById(R.id.content_frag).getNextFocusLeftId(),
                    R.id.bookmarks_list, R.id.list_frag, android.R.id.home, R.id.frag_log,
                    R.id.menu_search))
                return true;
        }
        pos += toRight ? 1 : -1;
        pos = pos % mViewPagerAdapter.getCount();
        mViewPager.setCurrentItem(pos);
        return true;
    }

    public View getPagerTitleView(OpenFragment frag) {
        int pos = mViewPagerAdapter.getItemPosition(frag);
        if (pos < 0)
            return null;
        return ((TabPageIndicator)mViewPager.getIndicator()).getView(pos);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        /*
         * if(hasFocus && mMainMenu.findItem(v.getId()) != null &&
         * mMainMenu.findItem(v.getId()).hasSubMenu()) v.requestFocus();
         */
    }

    @Override
    public int getThemedResourceId(int styleableId, int defaultResourceId) {
        return getOpenApplication().getThemedResourceId(styleableId, defaultResourceId);
    }

    public int getThemeId() {
        String themeName = getPreferences().getString("global", "pref_themes", "dark");
        if (themeName.equals("dark"))
            return R.style.AppTheme_Dark;
        else if (themeName.equals("light"))
            return R.style.AppTheme_Light;
        else if (themeName.equals("lightdark"))
            return R.style.AppTheme_LightAndDark;
        else if (themeName.equals("custom"))
            return R.style.AppTheme_Custom;
        return 0;
    }

    public static void changePath(Context context, OpenPath path) {
        Intent intent = new Intent(context, OpenExplorer.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(path.getUri());
        context.startActivity(intent);
    }

    @Override
    public void onBookmarkSelect(OpenPath path) {
        changePath(path, true, true);
    }

    public void setProgressClickHandler(android.view.View.OnClickListener listener) {
        ViewUtils.setOnClicks(this, listener, R.id.title_progress);
        ViewUtils.setViewsVisible(this, true, R.id.title_progress);
    }

}
