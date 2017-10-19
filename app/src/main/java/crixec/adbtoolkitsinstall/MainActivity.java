package crixec.adbtoolkitsinstall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener, ShellUtils.Result, AdapterView.OnItemSelectedListener {

    private CheckBox adbCheckBox;
    private CheckBox fastbootCheckBox;
    private Button installButton;
    private static StringBuilder output = new StringBuilder();
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    private List<String> locations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adbCheckBox = (CheckBox) findViewById(R.id.adb);
        fastbootCheckBox = (CheckBox) findViewById(R.id.fastboot);
        installButton = (Button) findViewById(R.id.install);
        spinner = (Spinner) findViewById(R.id.spinner);
        adbCheckBox.setOnCheckedChangeListener(this);
        fastbootCheckBox.setOnCheckedChangeListener(this);
        Collections.addAll(locations, getResources().getStringArray(R.array.locations));
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(this);
        spinner.setAdapter(adapter);
    }

    private boolean hasCheckedToolKits() {
        return adbCheckBox != null && fastbootCheckBox != null && (adbCheckBox.isChecked() || fastbootCheckBox.isChecked());
    }

    public void installButton(View view) {
        if (hasCheckedToolKits()) {
            new RunCommandsTask(this, obtainInstallCommands(adbCheckBox.isChecked(), fastbootCheckBox.isChecked(), locations.get(spinner.getSelectedItemPosition())), getString(R.string.installing)).execute();
        }
    }

    public void openAdbService(View view) {
        List<String> commands = new ArrayList<>();
        commands.add("setprop service.adb.tcp.port 5555");
        commands.add("stop adbd");
        commands.add("start adbd");
        new RunCommandsTask(this, commands, getString(R.string.opening)).execute();
    }

    public void closeAdbService(View view) {
        List<String> commands = new ArrayList<>();
        commands.add("setprop service.adb.tcp.port 0000");
        commands.add("stop adbd");
        new RunCommandsTask(this, commands, getString(R.string.opening)).execute();

    }

    public void runCommands(View view) {
        final View dialogView = getLayoutInflater().inflate(R.layout.layout_input, null, false);
        final EditText editText = (EditText) dialogView.findViewById(R.id.editText);
        final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.checkbox);
        new AlertDialog.Builder(this).setTitle(R.string.run_commands)
                .setView(dialogView)
                .setCancelable(false)
                .setNeutralButton(android.R.string.cancel, null).setPositiveButton(R.string.run, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                List<String> commands = new ArrayList<>();
                commands.add("alias adb=\'" + getApplicationInfo().nativeLibraryDir + "/libadb.so\'");
                commands.add("alias fastboot=\'" + getApplicationInfo().nativeLibraryDir + "/libfastboot.so\'");
                commands.add(editText.getText().toString());
                new RunCommandsTask(dialogView.getContext(), commands, getString(R.string.running), checkBox.isChecked()).execute();
            }
        })
                .show();
    }

    private List<String> obtainInstallCommands(boolean installAdb, boolean installFastboot, String targetPath) {
        List<String> commands = new ArrayList<>();
        commands.add("mount -o rw,remount /system");
        commands.add("mount -o remount,rw /system");
        commands.add("cp -f \'" + getApplicationInfo().nativeLibraryDir + "/libadb.so\' \'" + targetPath + "/adb\'");
        commands.add("cp -f \'" + getApplicationInfo().nativeLibraryDir + "/libfastboot.so\' \'" + targetPath + "/fastboot\'");
        return commands;
    }

    public void cleanUp(View view) {
        StringBuilder cleanablePath = new StringBuilder();
        final String[] locations = getResources().getStringArray(R.array.locations);
        cleanablePath.append(getString(R.string.delete_installed_files_in_above_folders)).append("\n\n").append(locations[0]).append("\n")
        .append(locations[1]).append("\n")
        .append(locations[2]).append("\n");
        new AlertDialog.Builder(this).setTitle(R.string.clean_up)
                .setMessage(cleanablePath)
        .setNeutralButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                List<String> commands = new ArrayList<>();
                commands.add(obtainCommand(locations[0]));
                commands.add(obtainCommand(locations[1]));
                commands.add(obtainCommand(locations[2]));
                commands.add("echo === CLEAN UP ===");
                new RunCommandsTask(MainActivity.this, commands, getString(R.string.cleaning)).execute();
            }
            private String obtainCommand(String location){
                return "rm -f " + location + "/adb " + location + "/fastboot 2>>/dev/null";
            }
        }).setCancelable(false)
        .show();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        installButton.setEnabled(hasCheckedToolKits());
    }

    @Override
    public void onStdout(String text) {
        output.append(text).append("\n");
        Log.i("ADBToolKitsInstaller", text);
    }

    @Override
    public void onStderr(String text) {
        output.append(text).append("\n");
        Log.i("ADBToolKitsInstaller", text);
    }

    @Override
    public void onCommand(String command) {
        output.append("==> " + command).append("\n");
        Log.i("ADBToolKitsInstaller", command);
    }

    @Override
    public void onFinish(int resultCode) {
        output.append("======\nexited with code ").append(resultCode).append("\n======\n").append("\n");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (locations.get(position).equals(getResources().getStringArray(R.array.locations)[3])) {
            // add custom location
            final EditText editText = new EditText(this);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        String location = editText.getText().toString();
                        if (!TextUtils.isEmpty(location)) {
                            locations.add(locations.size() - 1, location);
                            adapter.notifyDataSetChanged();
                            spinner.setSelection(locations.size() - 2);
                            return;
                        } else {
                            spinner.setSelection(0);
                            Toast.makeText(getApplicationContext(), getString(R.string.incorrect_installation_location), Toast.LENGTH_SHORT).show();
                        }
                    }
                    spinner.setSelection(0);
                }
            };
            editText.setHint(R.string.install_location);
            new AlertDialog.Builder(this).setCancelable(false)
                    .setView(editText)
                    .setNeutralButton(android.R.string.cancel, onClickListener)
                    .setPositiveButton(android.R.string.ok, onClickListener)
                    .show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        spinner.setSelection(0);
    }

    private static class RunCommandsTask extends AsyncTask<Void, Void, Boolean> {
        private List<String> mCommands;
        private ProgressDialog dialog;
        private AlertDialog.Builder dialogBuilder;
        private String mTitle;
        private Context mContext;
        private boolean runWithRoot = false;

        public RunCommandsTask(Context context, List<String> commands, String title) {
            this(context, commands, title, true);
        }
        public RunCommandsTask(Context context, List<String> commands, String title, boolean runWithRoot) {
            mCommands = commands;
            this.mTitle = title;
            this.mContext = context;
            dialog = new ProgressDialog(context);
            dialogBuilder = new AlertDialog.Builder(context);
            this.runWithRoot = runWithRoot;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return ShellUtils.exec(mCommands, (MainActivity) mContext, runWithRoot) == 0;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            output.setLength(0);
            dialog.setTitle(mTitle);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.dismiss), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            dialog.show();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            dialog.dismiss();
            String title = mContext.getString(R.string.successful);
            if (!aBoolean)
                title = mContext.getString(R.string.failure);
            dialogBuilder.setTitle(title);
            dialogBuilder.setPositiveButton(android.R.string.ok, null);
            View view = ((MainActivity) mContext).getLayoutInflater().inflate(R.layout.layout_output, null, false);
            dialogBuilder.setView(view);
            TextView textView = (TextView) view.findViewById(R.id.text1);
            textView.setText(output.toString());
            dialogBuilder.setCancelable(true);
            dialogBuilder.show();
        }

    }
}
