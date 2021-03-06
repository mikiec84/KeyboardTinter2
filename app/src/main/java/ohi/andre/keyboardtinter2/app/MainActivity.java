package ohi.andre.keyboardtinter2.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.keyboardtinter2.R;
import ohi.andre.keyboardtinter2.utils.ColorAdapter;
import ohi.andre.keyboardtinter2.utils.ColorSet;
import ohi.andre.keyboardtinter2.utils.ColorStorage;
import ohi.andre.keyboardtinter2.utils.Keyboards;
import ohi.andre.keyboardtinter2.utils.RandomColor;
import ohi.andre.keyboardtinter2.utils.Utils;
import ohi.andre.reflectionutils.ReflectionUtils;

public class MainActivity extends AppCompatActivity implements OnClickListener, OnItemClickListener, ColorChooserDialog.ColorCallback {

    public static final String prefs_name = "colors";
    public static final String color_key = "color";
    public static final String color_n_key = "color_n";
    public static final String preset_key = "preset";

    public static final int MATERIAL_LIGHT = 0;
    public static final int MATERIAL_DARK = 1;
    public static final int SHINY = 2;
    public static final int CUSTOM = 3;

    public static final int XPOSED_LOG = 0;
    public static final int ANDROID_LOG = 1;
    public static final int SUPPORTED_KEYBOARDS = 2;

    private final String show_activity = "show_activity";

    private final String email = "andreuzzi.francesco@gmail.com";
    private final String object = "KT2 request";

    private final String APP_FOLDER = "KT2";

    private SharedPreferences prefs;
    private ListView lv;
    private ColorAdapter adapter;
    private ColorChooserDialog dialog;
    private boolean showActivity;
    private ColorSet colorSet;

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.color_layout);

        lv = (ListView) findViewById(R.id.color_list);
        findViewById(R.id.add_color).setOnClickListener(this);

        prefs = getSharedPreferences(MainActivity.prefs_name, Activity.MODE_WORLD_READABLE);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(selectMenu(), menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.send_email_id)
            sendEmail();
        else if (id == R.id.add_random_color_id)
            onColorAdded();
        else if (id == R.id.about_developer_id)
            aboutDeveloper();
        else if (id == R.id.presets_id)
            showPresets();
        else {
            showActivity = id == R.id.restore_activity_id;
            invalidateOptionsMenu();
        }

        return true;
    }

    @Override
    public void onStop() {
        super.onStop();

        SharedPreferences.Editor edit = prefs.edit();

        edit.putInt(preset_key, colorSet.getPresetNumber());
        ColorStorage.storeColors(edit, adapter.getAll(), color_n_key, color_key);

        updateActivityState();
        edit.putBoolean(show_activity, showActivity);

        edit.commit();
    }

    private void init() {
        int presetN = prefs.getInt(preset_key, 0);
        colorSet = new ColorSet(prefs, presetN);

        adapter = new ColorAdapter(this, colorSet.getColors());
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        showActivity = prefs.getBoolean(show_activity, true);
    }

    private void showColorDialog() {
        if (colorSet.getPresetNumber() != CUSTOM) {
            Toast.makeText(this, R.string.cantadd_color_toast, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dialog == null) {
            dialog = new ColorChooserDialog.Builder(this, R.string.colordialog_title)
                    .accentMode(true)
                    .titleSub(R.string.colordialog_title)
                    .show();
        } else {
            dialog.show(this);
        }
    }

    private int selectMenu() {
        return showActivity ? R.menu.menu_active : R.menu.main_inactive;
    }

    private void sendEmail() {
        requestAttachments();
    }

    private void requestAttachments() {
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.sendattachment_title)
                .items(R.array.attachment_types)
                .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        return true;
                    }
                })
                .positiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        manageAttachmentRequest(dialog.getSelectedIndices());
                    }
                })
                .build();

        ListView view = dialog.getListView();
        view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for(int count = 0; count < view.getChildCount(); count++) {
            view.setItemChecked(count, true);
        }

        dialog.show();
    }

    private void manageAttachmentRequest(Integer[] selected) {
        List<File> attachments = new ArrayList<>();

        File kt2Folder = new File(Environment.getExternalStorageDirectory(), APP_FOLDER);
        if (!kt2Folder.exists() && !kt2Folder.mkdir()) {
            return;
        }

        if(ReflectionUtils.arrayContains(selected, XPOSED_LOG)) {
            new File(kt2Folder, "error.log").delete();

            try {
                Runtime.getRuntime().exec("cp /data/data/de.robv.android.xposed.installer/log/error.log " +
                        kt2Folder.getAbsolutePath());
            } catch (IOException e) {}

            File logFile = new File(kt2Folder, "error.log");
            attachments.add(logFile);
        }
        if(ReflectionUtils.arrayContains(selected, ANDROID_LOG)) {
            File logFile = Utils.getAndroidLog(kt2Folder);
            attachments.add(logFile);
        }
        if(ReflectionUtils.arrayContains(selected, SUPPORTED_KEYBOARDS)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager mgr = getPackageManager();
            List<ApplicationInfo> pkgAppsList = mgr.getInstalledApplications(PackageManager.GET_META_DATA);
            List<ApplicationInfo> keyboards = new ArrayList<>();
            for (ApplicationInfo app: pkgAppsList) {
                String packageName = app.packageName;
                if(Keyboards.isSupported(packageName)) {
                    keyboards.add(app);
                }
            }
            File keyboardsFile = new File(kt2Folder, "keyboards.log");
            try {
                FileWriter writer = new FileWriter(keyboardsFile);
                String separator = " - ";
                String newLine = "\n";
                for (ApplicationInfo info : keyboards) {
                    String label = info.loadLabel(mgr).toString();
                    String packageName = info.packageName;
                    PackageInfo pckgInfo = mgr.getPackageInfo(packageName, 0);
                    String versionCode = String.valueOf(pckgInfo.versionCode);
                    String versionName = pckgInfo.versionName;
                    writer.write(packageName + separator + label + separator + versionCode + separator + versionName + newLine);
                }
                writer.close();
                attachments.add(keyboardsFile);
            }
            catch (FileNotFoundException e) {}
            catch (IOException e) {}
            catch (PackageManager.NameNotFoundException e) {}
        }

        MainActivity.this.startActivity(Intent.createChooser(Utils.getEmailIntent(email, object,
                attachments.toArray(new File[attachments.size()])), getString(R.string.sendemail_title)));
    }

    private void aboutDeveloper() {
        new MaterialDialog.Builder(this)
                .title(R.string.about_developer_dialog_title)
                .content(R.string.about_developer_dialog_text)
                .show();
    }

    private void showPresets() {
        new MaterialDialog.Builder(this)
                .items(R.array.presets)
                .title(R.string.presets_title)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                        updatePreset(i);
                    }
                })
                .show();
    }

    private void updatePreset(int newSet) {
        colorSet.update(newSet);

        adapter.removeAll();
        adapter.addAll(colorSet.getColors());
    }

    private void onColorAdded() {
        if (colorSet.getPresetNumber() != CUSTOM) {
            Toast.makeText(this, R.string.cantadd_color_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        onColorAdded(new RandomColor().randomColor());
    }

    private void onColorAdded(int color) {
        if (colorSet.getPresetNumber() != CUSTOM) {
            Toast.makeText(this, R.string.cantadd_color_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.add(color);
        colorSet.add(color);
        Toast.makeText(this, getString(R.string.reboot_toast), Toast.LENGTH_SHORT).show();
    }

    private void updateActivityState() {
        int mode = showActivity ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, "ohi.andre.keyboardtinter2.activities.MainActivityAlias"),
                mode, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (colorSet.getPresetNumber() != CUSTOM) {
            Toast.makeText(this, R.string.cantadd_color_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.remove(position);
        Toast.makeText(this, getString(R.string.reboot_toast), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_color)
            showColorDialog();
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog colorChooserDialog, int i) {
        onColorAdded(i);
    }

}
