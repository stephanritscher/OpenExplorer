
package org.brandroid.openmanager.util;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

public class ActionModeHelper {
    private ActionMode mInstance;

    static {
        try {
            Class.forName("android.view.ActionMode");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void checkAvailable() {
    }

    public interface Callback extends ActionMode.Callback {
        public boolean onCreateActionMode(ActionMode mode, Menu menu);

        public boolean onPrepareActionMode(ActionMode mode, Menu menu);

        public boolean onActionItemClicked(ActionMode mode, MenuItem item);

        public void onDestroyActionMode(ActionMode mode);
    }
}
