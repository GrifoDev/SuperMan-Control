package com.wubydax.romcontrol.v2;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.wubydax.romcontrol.v2.utils.BackupRestoreIntentService;
import com.wubydax.romcontrol.v2.utils.Constants;
import com.wubydax.romcontrol.v2.utils.FileHelper;
import com.wubydax.romcontrol.v2.utils.MyDialogFragment;
import com.wubydax.romcontrol.v2.utils.SuTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/*      Created by Roberto Mariani and Anna Berkovitch, 2015-2016
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SuTask.OnSuCompletedListener,
        MyDialogFragment.OnDialogFragmentListener {
    private ProgressDialog mProgressDialog;
    private FragmentManager mFragmentManager;
    private SharedPreferences mSharedPreferences;
    private ArrayList<Integer> mNavMenuItemsIds;
    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;
    private static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmLs1lm/yKYgwzRShKYnsxbhzeIzGzX6tOLEnEzbOoWqkqAhYw+hEziLeJttoqrizXYNDPXKSq/f0YoZC204O+K6aWYub4Jz9o6+k390JCAZ67nyrmKyTU9pnnLfExb7+6JYmgRiLX0neMOxLrU8LHhqCipnld8p/ghr4Tsk2RSiphJv4YHtIKsBWpSseyhtLJsLhgAsNrns4LSeYmhY53kHpWaOrDtHGL3x0irsC/ShZKPYlJSWbWmdzLJPsQWlmSOjzR0aWRCz6klwyqCyM2Ca7yh8TicjXtTrXAIXWVnHLF4tRsKhXG14Yt3HC28zOKqaAApEQumMgheCTMpUPCwIDAQAB";
    private final String TEMP_FILE_NAME = ".other_temp.xml";
    private final String cmdRT = "cat /system/csc/others.xml > " + Environment.getExternalStorageDirectory() + "/.tmpRC/" + TEMP_FILE_NAME + "\n";
    private final String cmdTR = "cat " +Environment.getExternalStorageDirectory() + "/.tmpRC/" + TEMP_FILE_NAME + " > /system/csc/others.xml\n";
    private String result;
    private final File tempFile = new File(Environment.getExternalStorageDirectory() + "/.tmpRC/" + TEMP_FILE_NAME);
    File direct = new File(Environment.getExternalStorageDirectory() + "/.tmpRC");
    Process p;

    // Generate your own 20 random bytes, and put them here.
    private static final byte[] SALT = new byte[] {
            35, 95, -43, -13, 123, 28, -123, -76, 12, 39, -75, 15, 94, -13, -100, 96, 55, -32, 46,
            39
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(mSharedPreferences.getInt(Constants.THEME_PREF_KEY, getResources().getInteger(R.integer.default_theme)) == 0 ? R.style.AppTheme_NoActionBar : R.style.AppTheme_NoActionBar_Dark);
        setContentView(R.layout.activity_main);
        mFragmentManager = getFragmentManager();
        int lastFragmentIndex = mSharedPreferences.getInt(Constants.LAST_FRAGMENT, 0);
        String[] titles = getResources().getStringArray(R.array.nav_menu_prefs_titles);
        if (savedInstanceState == null) {
            String[] xmlNames = getResources().getStringArray(R.array.nav_menu_xml_file_names);
            loadPrefsFragment(xmlNames[lastFragmentIndex]);
            SuTask suTask = new SuTask();
            suTask.setOnSuCompletedListener(this);
            suTask.execute();
            if (mSharedPreferences.getBoolean(Constants.FIRST_LAUNCH_KEY, true)) {
                mProgressDialog = ProgressDialog.show(this, getString(R.string.root_progress_dialog_title), getString(R.string.root_progress_dialog_message), false);
            }
        }
        setTitle(titles[lastFragmentIndex]);
        initViews();
        try {
            p = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!direct.exists()) {
            direct.mkdir(); //directory is created;
        }

        FileHelper.copyFileToTemp(cmdRT, p);
        result = FileHelper.readFile(tempFile);
        //FileHelper.investInput(result, tempFile);
        //FileHelper.copyFileToRoot(cmdTR, p);

        String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY);
        doCheck();

    }

    private void initViews() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        Menu navigationMenu = navigationView.getMenu();
        setUpPrefsMenu(navigationMenu);
    }

    private void setUpPrefsMenu(Menu navigationMenu) {
        String[] titles = getResources().getStringArray(R.array.nav_menu_prefs_titles);
        TypedArray iconIds = getResources().obtainTypedArray(R.array.nav_menu_prefs_drawables);
        mNavMenuItemsIds = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            Integer id = View.generateViewId();
            mNavMenuItemsIds.add(id);
            MenuItem item = navigationMenu.add(Menu.NONE, id, 0, titles[i]).setIcon(iconIds.getResourceId(i, -1));
            if (titles[i] != null && titles[i].equals(getTitle().toString())) {
                item.setChecked(true);
            }
        }
        iconIds.recycle();
        navigationMenu.setGroupCheckable(Menu.NONE, true, true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.rebootMenu:
                mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance(Constants.REBOOT_MENU_DIALOG_REQUEST_CODE), "reboot_dialog").commit();
                break;
        }


        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mNavMenuItemsIds.contains(id)) {
            int index = mNavMenuItemsIds.indexOf(id);
            loadPrefsFragment(getResources().getStringArray(R.array.nav_menu_xml_file_names)[index]);
            setTitle(item.getTitle().toString());
            mSharedPreferences.edit().putInt(Constants.LAST_FRAGMENT, index).apply();
        } else {
            switch (id) {

                case R.id.themes:
                    mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance(Constants.THEME_DIALOG_REQUEST_CODE), "theme_dialog").commit();
                    break;
                case R.id.changeLog:
                    mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance(Constants.CHANGELOG_DIALOG_REQUEST_CODE), "changelog").commit();
                    break;
                case R.id.about_us:
                    startActivity(new Intent(this, AboutActivity.class));
                    break;
                case R.id.backup_restore:
                    mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance(Constants.BACKUP_OR_RESTORE_DIALOG_REQUEST_CODE), "backup_restore").commit();
                    break;
            }

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void launchBackupRestoreService(int which, String filePath) {
        String action;
        Intent intent = new Intent(this, BackupRestoreIntentService.class);
        switch (which) {
            case 0:
                action = Constants.SERVICE_INTENT_ACTION_BACKUP;
                break;
            case 1:
                action = Constants.SERVICE_INTENT_ACTION_RESTORE;
                intent.putExtra(Constants.BACKUP_FILE_PATH_EXTRA_KEY, filePath);
                break;
            default:
                action = null;
        }
        if (action != null) {
            intent.setAction(action);
            startService(intent);
            if (which == 1) {
                finish();
            }
        }
    }

    private void loadPrefsFragment(String prefName) {
        mFragmentManager.beginTransaction().replace(R.id.fragment_container, PrefsFragment.newInstance(prefName)).commit();
    }


    @Override
    public void onTaskCompleted(boolean isGranted) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        boolean isFirstLaunch = mSharedPreferences.getBoolean(Constants.FIRST_LAUNCH_KEY, true);
        if (!isGranted && isFirstLaunch) {
            mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance(Constants.NO_SU_DIALOG_REQUEST_CODE), "no_su_dialog_fragment").commit();
        }
        if (isFirstLaunch) {
            mSharedPreferences.edit().putBoolean(Constants.FIRST_LAUNCH_KEY, false).apply();
        }
    }


    @Override
    public void onDialogResult(int requestCode) {
        switch (requestCode) {
            case Constants.THEME_DIALOG_REQUEST_CODE:
                finish();
                this.overridePendingTransition(0, R.animator.fadeout);
                startActivity(new Intent(this, MainActivity.class));
                this.overridePendingTransition(R.animator.fadein, 0);
                break;
        }

    }

    @Override
    public void onBackupRestoreResult(int which) {
        switch (which) {
            case 0:
                launchBackupRestoreService(which, null);
                break;
            case 1:
                mFragmentManager.beginTransaction().add(MyDialogFragment.newInstance(Constants.RESTORE_FILE_SELECTOR_DIALOG_REQUEST_CODE), "restore_selector").commit();
                break;
        }
    }

    @Override
    protected void onPause() {
        Runtime.getRuntime().gc();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onRestoreRequested(String filePath, boolean isConfirmed) {
        if (isConfirmed) {
            launchBackupRestoreService(1, filePath);
        } else {
            mFragmentManager.beginTransaction().add(MyDialogFragment.backupRestoreInstance(Constants.RESTORE_FILE_SELECTOR_DIALOG_REQUEST_CODE, true, filePath), "restore_confirm").commit();
        }
    }

    @Override
    public View getDecorView() {
        return getWindow().getDecorView();
    }

    private void doCheck() {
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {
        public void allow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
        }

        public void dontAllow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
        }

        public void applicationError(int errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChecker.onDestroy();
        if(direct.isDirectory()) {
            String[] children = direct.list();
            for(int i = 0; i < children.length; i++) {
                new File(direct, children[i]).delete();
            }
            direct.delete();
        }
    }
}
