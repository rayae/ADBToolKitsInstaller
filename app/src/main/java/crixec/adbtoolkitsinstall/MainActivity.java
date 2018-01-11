package crixec.adbtoolkitsinstall;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import cn.bavelee.donatedialog.DonateToMe;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, ShellUtils.Result, AdapterView.OnItemSelectedListener, View.OnClickListener {

    private CheckBox adbCheckBox;
    private CheckBox fastbootCheckBox;
    private static StringBuilder output = new StringBuilder();
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    private List<String> locations = new ArrayList<>();
    private TextView adbServerStatus;
    private TextView adbIpAddress;
    private TextView adbPort;
    private TextView adbBinary;
    private TextView fastbootBinary;
    private Buttons mButtons = new Buttons();

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.install: {
                if (hasCheckedToolKits()) {
                    new RunCommandsTask(this, obtainInstallCommands(adbCheckBox.isChecked(), fastbootCheckBox.isChecked(), locations.get(spinner.getSelectedItemPosition())), getString(R.string.installing)).execute();
                }
                break;
            }
            case R.id.uninstall: {
                uninstall();
                break;
            }
            case R.id.start_adb_service: {
                List<String> commands = new ArrayList<>();
                commands.add("setprop service.adb.tcp.port 5555");
                commands.add("stop adbd");
                commands.add("start adbd");
                new RunCommandsTask(this, commands, getString(R.string.opening)).execute();
                break;
            }
            case R.id.stop_adb_service: {
                List<String> commands = new ArrayList<>();
                commands.add("setprop service.adb.tcp.port 0");
                commands.add("stop adbd");
                new RunCommandsTask(this, commands, getString(R.string.opening)).execute();
                break;
            }
            case R.id.run_commands: {
                runCommands();
                break;
            }
            case R.id.refersh: {
                refreshAll();
                break;
            }
        }
    }

    public void donate(View view) {
        DonateToMe.show(this);
    }

    private class Buttons {
        Button install;
        Button uninstall;
        Button startAdbServer;
        Button stopAdbServer;
        Button runCommands;
        Button refresh;

        private void setOnClickListener(View.OnClickListener clickListener) {
            install.setOnClickListener(clickListener);
            uninstall.setOnClickListener(clickListener);
            stopAdbServer.setOnClickListener(clickListener);
            startAdbServer.setOnClickListener(clickListener);
            runCommands.setOnClickListener(clickListener);
            refresh.setOnClickListener(clickListener);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adbCheckBox = (CheckBox) findViewById(R.id.adb);
        fastbootCheckBox = (CheckBox) findViewById(R.id.fastboot);
        spinner = (Spinner) findViewById(R.id.spinner);
        adbServerStatus = findViewById(R.id.text_view_adb_server);
        adbIpAddress = findViewById(R.id.text_view_ip_address);
        adbPort = findViewById(R.id.text_view_adb_port);
        adbBinary = findViewById(R.id.text_view_adb_location);
        fastbootBinary = findViewById(R.id.text_view_fastboot_location);
        adbCheckBox.setOnCheckedChangeListener(this);
        fastbootCheckBox.setOnCheckedChangeListener(this);
        Collections.addAll(locations, getResources().getStringArray(R.array.locations));
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(this);
        spinner.setAdapter(adapter);
        mButtons.install = findViewById(R.id.install);
        mButtons.uninstall = findViewById(R.id.uninstall);
        mButtons.startAdbServer = findViewById(R.id.start_adb_service);
        mButtons.stopAdbServer = findViewById(R.id.stop_adb_service);
        mButtons.runCommands = findViewById(R.id.run_commands);
        mButtons.refresh = findViewById(R.id.refersh);
        mButtons.setOnClickListener(this);
        refreshAll();
    }

    private boolean hasCheckedToolKits() {
        return adbCheckBox != null && fastbootCheckBox != null && (adbCheckBox.isChecked() || fastbootCheckBox.isChecked());
    }

    public void runCommands() {
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

    public void uninstall() {
        StringBuilder cleanablePath = new StringBuilder();
        final String[] locations = getResources().getStringArray(R.array.locations);
        cleanablePath.append(getString(R.string.delete_installed_files_in_above_folders)).append("\n\n").append(locations[0]).append("\n")
                .append(locations[1]).append("\n")
                .append(locations[2]).append("\n");
        new AlertDialog.Builder(this).setTitle(R.string.uninstall)
                .setMessage(cleanablePath)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        List<String> commands = new ArrayList<>();
                        commands.add(obtainCommand(locations[0]));
                        commands.add(obtainCommand(locations[1]));
                        commands.add(obtainCommand(locations[2]));
                        new RunCommandsTask(MainActivity.this, commands, getString(R.string.cleaning)).execute();
                    }

                    private String obtainCommand(String location) {
                        return "rm -f " + location + "/adb " + location + "/fastboot 2>>/dev/null";
                    }
                }).setCancelable(false)
                .show();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mButtons.install.setEnabled(hasCheckedToolKits());
    }

    @Override
    public void onStdout(String text) {
        output.append("[Stdout] ").append(text).append("\n");
        Log.i("Stdout", text);
    }

    @Override
    public void onStderr(String text) {
        output.append("[Stderr] ").append(text).append("\n");
        Log.i("Stderr", text);
    }

    @Override
    public void onCommand(String command) {
        output.append("[Run command] ").append(command).append("\n");
        Log.i("Run command => ", command);
    }

    @Override
    public void onFinish(int resultCode) {
        if (resultCode != 0)
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

    public void refreshAll() {
        adbIpAddress.setText(getLocalIpAddress());
        String isRunning = getString(R.string.is_running);
        String notRunning = getString(R.string.not_running);
        adbServerStatus.setText(ShellUtils.exec("ps|grep adbd", null, true) == 0 ? isRunning : notRunning);
        adbPort.setText(ShellUtils.execSynchronizedCommand("getprop service.adb.tcp.port"));
        adbBinary.setText(linuxWhich("adb"));
        fastbootBinary.setText(linuxWhich("fastboot"));
    }

    private String linuxWhich(String param) {
        String systemPath = System.getenv("PATH");
        String[] pathDirs = systemPath.split(File.pathSeparator);
        for (String pathDir : pathDirs) {
            File file = new File(pathDir, param);
            if (file.isFile()) {
                return file.getPath();
            }
        }
        return getString(R.string.unknow_path);
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "0.0.0.0";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.developer_mode) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.DevelopmentSettings");
                startActivity(intent);
            } catch (Exception ignore) {

            }
        }
        return super.onOptionsItemSelected(item);
    }

    private class RunCommandsTask extends AsyncTask<Void, Void, Boolean> {
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
            refreshAll();
        }

    }
}
