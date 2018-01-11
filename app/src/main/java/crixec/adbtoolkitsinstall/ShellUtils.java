package crixec.adbtoolkitsinstall;

/**
 * Created by crixec on 17-3-18.
 */

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ShellUtils {
    private static String TAG = "ShellUtils";
    private static boolean isRunning = true;

    public interface Result {
        void onStdout(String text);

        void onStderr(String text);

        void onCommand(String command);

        void onFinish(int resultCode);
    }

    private static interface Output {
        public void output(String text);
    }

    public static class OutputReader extends Thread {
        private Output output = null;
        private BufferedReader reader = null;
        private boolean isRunning = false;

        public OutputReader(BufferedReader reader, Output output) {
            this.output = output;
            this.reader = reader;
            this.isRunning = true;
        }

        public void close() {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            String line = null;
            while (isRunning) {
                try {
                    line = reader.readLine();
                    if (line != null)
                        output.output(line);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                }
            }
        }

        public void cancel() {
            synchronized (this) {
                isRunning = false;
                this.notifyAll();
            }
        }
    }

    private static int exec(final String sh, final List<String> cmds, final Result result) {
        Process process = null;
        DataOutputStream stdin = null;
        OutputReader stdout = null;
        OutputReader stderr = null;
        int resultCode = -1;

        try {
            process = Runtime.getRuntime().exec(sh);
            stdin = new DataOutputStream(process.getOutputStream());
            if (result != null) {
                stdout = new OutputReader(new BufferedReader(new InputStreamReader(process.getInputStream())),
                        new Output() {
                            @Override
                            public void output(String text) {
                                // TODO Auto-generated method stub
                                if (result != null)
                                    result.onStdout(text);
                            }
                        });
                stderr = new OutputReader(new BufferedReader(new InputStreamReader(process.getErrorStream())),
                        new Output() {
                            @Override
                            public void output(String text) {
                                // TODO Auto-generated method stub
                                if (result != null)
                                    result.onStderr(text);
                            }
                        });
                stdout.start();
                stderr.start();
            }
            for (String cmd : cmds) {
                if (result != null)
                    result.onCommand(cmd);
                stdin.writeBytes(cmd);
                stdin.writeBytes("\n");
                stdin.flush();
            }
            stdin.writeBytes("exit $?\n");
            stdin.flush();
            resultCode = process.waitFor();
            if (result != null)
                result.onFinish(resultCode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stdout.cancel();
                stderr.cancel();
                stdin.close();
                stdout.close();
                stderr.close();
            } catch (Exception ignored) {

            }
        }
        return resultCode;
    }

    public static int exec(final List<String> cmds, final Result result, final boolean isRoot) {
        String sh = isRoot ? "su" : "sh";
        return exec(sh, cmds, result);
    }

    public static int exec(final String cmd, final Result result, boolean isRoot) {
        List<String> cmds = new ArrayList<String>();
        cmds.add(cmd);
        return exec(cmds, result, isRoot);
    }

    public static int exec(final String cmd) {
        return exec(cmd, null, false);
    }

    public static int execRoot(final String cmd) {
        return exec(cmd, null, true);
    }

    public static int execRoot(final String cmd, final Result result) {
        return exec(cmd, result, true);
    }

    public static String execSynchronizedCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}