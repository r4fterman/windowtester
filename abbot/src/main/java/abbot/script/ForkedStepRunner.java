package abbot.script;

import abbot.AssertionFailedError;
import abbot.Log;
import abbot.finder.AWTHierarchy;
import abbot.i18n.Strings;
import abbot.util.ProcessOutputHandler;
import abbot.util.Properties;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A StepRunner that runs the step in a separate VM.  Behavior should be indistinguishable from the
 * base StepRunner.
 */
public class ForkedStepRunner extends StepRunner {

  private static final int LAUNCH_TIMEOUT =
      Properties.getProperty("abbot.runner.launch_delay", 60000, 0, 300000);
  private static final int TERMINATE_TIMEOUT =
      Properties.getProperty("abbot.runner.terminate_delay", 30000, 0, 300000);

  private static ServerSocket serverSocket = null;
  private Socket connection = null;

  /**
   * When actually within the separate VM, this is what gets run.
   */
  protected static class SlaveStepRunner extends StepRunner {

    private Socket connection = null;
    private Script script = null;

    /**
     * Translate the given event into something we can send back to the master.
     */
    private void forwardEvent(StepEvent event) {
      Step step = event.getStep();
      var encodedStep = encodeStep(script, step);
      final StringBuilder sb =
          new StringBuilder(encodedStep == null ? "" : encodedStep)
              .append("\n")
              .append(event.getType())
              .append("\n")
              .append(event.getID());
      Throwable thr = event.getError();
      if (thr != null) {
        sb.append("\nMSG:").append(thr.getMessage()).append("\nSTR:").append(thr).append("\nTRC:");

        StringWriter writer = new StringWriter();
        thr.printStackTrace(new PrintWriter(writer));
        sb.append(writer);
      }
      try {
        writeMessage(connection, sb.toString());
      } catch (IOException io) {
        // nothing we can do
      }
    }

    public void launchSlave(int port) {
      // make connection back to the originating port
      try {
        InetAddress local = InetAddress.getLocalHost();
        connection = new Socket(local, port);
      } catch (Throwable thr) {
        // Can't communicate, so the only option is to quit
        Log.warn(thr);
        System.exit(1);
      }
      script = new Script(new AWTHierarchy());
      try {
        String dirName = readMessage(connection);
        // Make sure the relative directory of this script is set
        // properly.
        script.setFile(new File(dirName, script.getFile().getName()));

        String contents = readMessage(connection);
        script.load(new StringReader(contents == null ? "" : contents));
        Log.debug("Successfully loaded script, dir=" + dirName);
        // Make sure we only fork once!
        script.setForked(false);
      } catch (IOException io) {
        Log.warn(io);
        System.exit(2);
      } catch (Throwable thr) {
        Log.debug(thr);
        StepEvent event = new StepEvent(script, StepEvent.STEP_ERROR, 0, thr);
        forwardEvent(event);
      }

      // add listener to send messages back to the master
      addStepListener(this::forwardEvent);

      // Run the script like we normally would.  The listener handles
      // all events and communication back to the launching process
      try {
        SlaveStepRunner.this.run(script);
      } catch (Throwable thr) {
        // Listener catches all events and forwards them, so no one
        // else is interested.  just quit.
        Log.debug(thr);
      }

      try {
        connection.close();
      } catch (IOException io) {
        // not much we can do
        Log.warn(io);
      }
      // Return zero even on failure/error, since the script run itself
      // worked, regardless of test results.
      System.exit(0);
    }
  }

  public ForkedStepRunner() {
    this(null);
  }

  public ForkedStepRunner(StepRunner parent) {
    super(parent != null ? parent.helper : null);
    if (parent != null) {
      setStopOnError(parent.getStopOnError());
      setStopOnFailure(parent.getStopOnFailure());
      setTerminateOnError(parent.getTerminateOnError());
    }
  }

  Process fork(String vmargs, String[] cmdArgs) throws IOException {
    String java =
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    List<String> args = new ArrayList<>();
    args.add(java);
    args.add("-cp");
    String cp = System.getProperty("java.class.path");
    // Ensure the framework is included in the class path
    String acp = System.getProperty("abbot.class.path");
    if (acp != null) {
      cp += File.pathSeparator + acp;
    }
    args.add(cp);
    if (vmargs != null) {
      StringTokenizer st = new StringTokenizer(vmargs);
      while (st.hasMoreTokens()) {
        args.add(st.nextToken());
      }
    }
    args.addAll(Arrays.asList(cmdArgs));
    if (Log.isClassDebugEnabled(getClass())) {
      args.add("--debug");
      args.add(getClass().getName());
    }
    cmdArgs = args.toArray(new String[0]);
    return Runtime.getRuntime().exec(cmdArgs);
  }

  /**
   * Launch a new process, using this class as the main class.
   */
  Process fork(String vmargs) throws IOException {
    if (serverSocket == null) {
      serverSocket = new ServerSocket(0);
    }
    int localPort = serverSocket.getLocalPort();
    String[] args = {
      getClass().getName(),
      String.valueOf(localPort),
      String.valueOf(getStopOnFailure()),
      String.valueOf(getStopOnError()),
      String.valueOf(getTerminateOnError())
    };
    Process p = fork(vmargs, args);
    new ProcessOutputHandler(p) {
      public void handleOutput(byte[] buf, int count) {
        System.out.println("[out] " + new String(buf, 0, count));
      }

      public void handleError(byte[] buf, int count) {
        System.err.println("[err] " + new String(buf, 0, count));
      }
    };
    return p;
  }

  /**
   * Running the step in a separate VM should be indistinguishable from running a regular script.
   * When running as master, nothing actually runs locally.  We just fork a subprocess and run the
   * script in that, reporting back its progress as if it were running locally.
   */
  public void runStep(Step step) throws Throwable {
    Log.debug("run step " + step);
    // Fire the start event prior to forking, then ignore the subsequent
    // forked script start event when we get it.
    fireStepStart(step);
    Process process = null;
    try {
      Script script = (Script) step;
      process = forkProcess(script.getVMArgs());
      sendScript(script);
      try {
        trackScript(script);
        try {
          process.waitFor();
        } catch (InterruptedException e) {
          // ignore
        }
        try {
          process.exitValue();
        } catch (IllegalThreadStateException its) {
          try {
            Thread.sleep(TERMINATE_TIMEOUT);
          } catch (InterruptedException ie) {
            // ignore
          }
        }
      } catch (IOException io) {
        fireStepError(script, io);
        if (getStopOnError()) {
          throw io;
        }
      }
    } catch (AssertionFailedError afe) {
      fireStepFailure(step, afe);
      if (getStopOnFailure()) {
        throw afe;
      }
    } catch (Throwable thr) {
      fireStepError(step, thr);
      if (getStopOnError()) {
        throw thr;
      }
    } finally {
      // Destroy it whether it's terminated or not.
      if (process != null) {
        process.destroy();
      }
    }
    fireStepEnd(step);
  }

  private Process forkProcess(String vmargs) throws Throwable {
    try {
      // fork new VM
      // wait for connection
      Process p = fork(vmargs);
      serverSocket.setSoTimeout(LAUNCH_TIMEOUT);
      connection = serverSocket.accept();
      Log.debug("Got slave connection on " + connection);
      return p;
    } catch (InterruptedIOException ie) {
      Log.warn(ie);
      throw new RuntimeException(Strings.get("runner.slave_timed_out"));
    }
  }

  private void sendScript(Script script) throws IOException {
    StringWriter writer = new StringWriter();
    script.save(writer);
    writeMessage(connection, script.getDirectory().toString());
    writeMessage(connection, writer.toString());
  }

  private void trackScript(Script script) throws IOException, ForkedFailure, ForkedError {
    StepEvent ev;
    while (!stopped() && (ev = receiveEvent(script)) != null) {
      Log.debug("Forked event received: " + ev);
      // If it's the script start event, ignore it since we
      // already sent one before launching the process
      if (ev.getStep() == script
          && (StepEvent.STEP_START.equals(ev.getType())
              || StepEvent.STEP_END.equals(ev.getType()))) {
        continue;
      }

      Log.debug("Replaying forked event locally " + ev);
      Throwable err = ev.getError();
      if (err != null) {
        setError(ev.getStep(), err);
        fireStepEvent(ev);
        if (err instanceof ForkedFailure) {
          if (getStopOnFailure()) {
            throw (ForkedFailure) err;
          }
        } else {
          if (getStopOnError()) {
            throw (ForkedError) err;
          }
        }
      } else {
        fireStepEvent(ev);
      }
    }
  }

  static Step decodeStep(Sequence root, String code) {
    if (code.equals("-1")) {
      return root;
    }
    int comma = code.indexOf(",");
    if (comma == -1) {
      // Let number format exceptions propagate up, since it's a fatal
      // script error.
      int index = Integer.parseInt(code);
      return root.getStep(index);
    }
    String ind = code.substring(0, comma);
    code = code.substring(comma + 1);
    return decodeStep((Sequence) root.getStep(Integer.parseInt(ind)), code);
  }

  /**
   * Encode the given step into a set of indices.
   */
  private static String encodeStep(Sequence root, Step step) {
    if (root.equals(step)) {
      return "-1";
    }
    synchronized (root.steps()) {
      int index = root.indexOf(step);
      if (index != -1) {
        return String.valueOf(index);
      }
      index = 0;
      for (Step seq : root.steps()) {
        if (seq instanceof Sequence) {
          String encoding = encodeStep((Sequence) seq, step);
          if (encoding != null) {
            return index + "," + encoding;
          }
        }
        ++index;
      }
      return null;
    }
  }

  /**
   * Receive a serialized event on the connection and convert it back into a real event, setting the
   * local representation of the given step's exception/error if necessary.
   */
  private StepEvent receiveEvent(Script script) throws IOException {
    String buf = readMessage(connection);
    if (buf == null) {
      Log.debug("End of stream");
      return null;
    }
    StringTokenizer st = new StringTokenizer(buf, "\n");
    String code = st.nextToken();
    String type = st.nextToken();
    String id = st.nextToken();
    Step step = decodeStep(script, code);
    Throwable thr = null;
    if (st.hasMoreTokens()) {
      StringBuilder msg = new StringBuilder(st.nextToken());
      msg = new StringBuilder(msg.substring(4));
      String next = st.nextToken();
      while (!next.startsWith("STR:")) {
        msg.append(next);
        next = st.nextToken();
      }
      StringBuilder string = new StringBuilder(next.substring(4));
      next = st.nextToken();
      while (!next.startsWith("TRC:")) {
        string.append(next);
        next = st.nextToken();
      }
      StringBuilder trace = new StringBuilder(next.substring(4));
      while (st.hasMoreTokens()) {
        trace.append("\n").append(st.nextToken());
      }

      if (type.equals(StepEvent.STEP_FAILURE)) {
        Log.debug("Creating local forked step failure");
        thr = new ForkedFailure(msg.toString(), string.toString(), trace.toString());
      } else {
        Log.debug("Creating local forked step error");
        thr = new ForkedError(msg.toString(), string.toString(), trace.toString());
      }
    }
    return new StepEvent(step, type, Integer.parseInt(id), thr);
  }

  private static void writeMessage(Socket connection, String msg) throws IOException {
    OutputStream os = connection.getOutputStream();
    byte[] buf = msg.getBytes();
    int len = buf.length;
    for (int i = 0; i < 4; i++) {
      byte val = (byte) (len >> 24);
      os.write(val);
      len <<= 8;
    }
    os.write(buf, 0, buf.length);
    os.flush();
  }

  private static String readMessage(Socket connection) throws IOException {
    InputStream is = connection.getInputStream();
    // FIXME probably want a socket timeout, in case the slave script
    // hangs
    int len = 0;
    for (int i = 0; i < 4; i++) {
      int data = is.read();
      if (data == -1) {
        return null;
      }
      len = (len << 8) | data;
    }
    byte[] buf = new byte[len];
    int offset = 0;
    while (offset < len) {
      int count = is.read(buf, offset, buf.length - offset);
      if (count == -1) {
        return null;
      }
      offset += count;
    }
    return new String(buf, 0, len);
  }

  /**
   * An exception that for all purposes looks like another exception.
   */
  private static class ForkedFailure extends AssertionFailedError {

    private final String msg;
    private final String str;
    private final String trace;

    public ForkedFailure(String msg, String str, String trace) {
      this.msg = msg + " (forked)";
      this.str = str + " (forked)";
      this.trace = trace + " (forked)";
    }

    public String getMessage() {
      return msg;
    }

    public String toString() {
      return str;
    }

    public void printStackTrace(PrintWriter writer) {
      synchronized (writer) {
        writer.print(trace);
      }
    }

    public void printStackTrace(PrintStream s) {
      synchronized (s) {
        s.print(trace);
      }
    }

    public void printStackTrace() {
      printStackTrace(System.err);
    }
  }

  /**
   * An exception that for all purposes looks like another exception.
   */
  private static class ForkedError extends RuntimeException {

    private final String msg;
    private final String str;
    private final String trace;

    public ForkedError(String msg, String str, String trace) {
      this.msg = msg + " (forked)";
      this.str = str + " (forked)";
      this.trace = trace + " (forked)";
    }

    public String getMessage() {
      return msg;
    }

    public String toString() {
      return str;
    }

    public void printStackTrace(PrintWriter writer) {
      synchronized (writer) {
        writer.print(trace);
      }
    }

    public void printStackTrace(PrintStream s) {
      synchronized (s) {
        s.print(trace);
      }
    }

    public void printStackTrace() {
      printStackTrace(System.err);
    }
  }

  public static void main(String[] args) {
    args = Log.init(args);
    try {
      final int port = Integer.parseInt(args[0]);
      final SlaveStepRunner runner = new SlaveStepRunner();
      runner.setStopOnFailure("true".equals(args[1]));
      runner.setStopOnError("true".equals(args[2]));
      runner.setTerminateOnError("true".equals(args[3]));
      new Thread(() -> runner.launchSlave(port), "Forked script").start();
    } catch (Throwable e) {
      System.err.println("usage: abbot.script.ForkedStepRunner <port>");
      System.exit(1);
    }
  }
}
