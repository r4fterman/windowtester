package abbot.script;

import static java.util.stream.Collectors.toMap;

import abbot.Log;
import abbot.Platform;
import abbot.finder.AWTHierarchy;
import abbot.finder.Hierarchy;
import abbot.i18n.Strings;
import abbot.script.parsers.FileParser;
import abbot.script.parsers.Parser;
import abbot.tester.Robot;
import abbot.util.Properties;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.WeakHashMap;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Provide a structure to encapsulate actions invoked on GUI components and tests performed on those
 * components. Scripts need to be short and concise (and therefore easy to read/write). Extensions
 * don't have to be. This takes a single filename as a constructor argument.
 *
 * @see StepRunner
 * @see Fixture
 * @see Launch
 */
public class Script extends Sequence implements Resolver {

  public static final String INTERPRETER = "bsh";
  private static final String USAGE =
      "<AWTTestScript [desc=\"\"] [forked=\"true\"] [slow=\"true\"]"
          + " [awt=\"true\"] [vmargs=\"...\"]>...</AWTTestScript>\n";

  /**
   * Robot delay for slow playback.
   */
  private static int slowDelay = 250;

  private String filename;
  private File relativeDirectory;
  private boolean fork;
  private boolean slow;
  private boolean awt;
  private int lastSaved;
  private String vmargs;
  private final Map<String, Object> properties = new HashMap<>();
  private transient Hierarchy hierarchy;
  public static final String UNTITLED_FILE = Strings.get("script.untitled_filename");
  protected static final String UNTITLED = Strings.get("script.untitled");

  /**
   * Read-only map of ref IDs into ComponentReferences.
   */
  private transient Map<String, ComponentReference> refs =
      Collections.unmodifiableMap(new HashMap<>());

  /**
   * Maps components to references.  This cache provides a 20% speedup when adding new references.
   */
  private final transient Map<Component, ComponentReference> components = new WeakHashMap<>();

  static {
    slowDelay = Properties.getProperty("abbot.script.slow_delay", slowDelay, 0, 60000);
    String defValue = Platform.JAVA_VERSION < Platform.JAVA_1_4 ? "false" : "true";
  }

  private static Map<String, String> createDefaultMap(File file) {
    Map<String, String> map = new HashMap<>();
    map.put(TAG_FILENAME, file.getAbsolutePath());
    return map;
  }

  private static File toFile(Resolver parent, String filename) {
    File directory = parent != null ? parent.getDirectory() : null;
    return filename != null ? new File(filename) : getTempFile(directory);
  }

  /**
   * Create a new, empty <code>Script</code>.  Used as a temporary {@link Resolver}, uses the
   * default {@link Hierarchy}.
   *
   * @deprecated Use an explicit {@link Hierarchy} instead.
   */
  @Deprecated
  public Script() {
    // This is roughly equivalent to what
    // DefaultComponentFinder.getFinder() used to do
    this("");
  }

  /**
   * Create a <code>Script</code> from the given filename.  Uses the default {@link Hierarchy}.
   *
   * @param filename filename
   * @deprecated use constructor Script(File) instead
   */
  @Deprecated
  public Script(String filename) {
    // This is roughly equivalent to what
    // DefaultComponentFinder.getFinder() used to do
    this(filename, AWTHierarchy.getDefault());
  }

  public Script(String filename, Hierarchy hierarchy) {
    this(toFile(null, filename), hierarchy);
  }

  public Script(Hierarchy hierarchy) {
    this((File) null, hierarchy);
  }

  public Script(File file) {
    this(null, createDefaultMap(file));
  }

  public Script(File file, Hierarchy hierarchy) {
    this(null, createDefaultMap(file));
    setHierarchy(hierarchy);
  }

  public Script(Resolver parent, Map<String, String> attributes) {
    super(parent, attributes);

    setFile(Optional.ofNullable(attributes.get(TAG_FILENAME)).map(File::new).orElse(null));
    if (parent != null) {
      setRelativeTo(parent.getDirectory());
    }

    try {
      load();
    } catch (IOException e) {
      setScriptError(e);
    }
  }

  /**
   * Since we allow ComponentReference IDs to be changed, make sure our map is always up-to-date.
   */
  private synchronized void synchReferenceIDs() {
    Map<String, ComponentReference> map =
        refs.values().stream().collect(toMap(ComponentReference::getID, value -> value));

    if (!refs.equals(map)) {
      // atomic update of references map
      refs = Collections.unmodifiableMap(map);
    }
  }

  public void setHierarchy(Hierarchy h) {
    hierarchy = h;
    components.clear();
  }

  private static File getTempFile(File dir) {
    File file;
    try {
      file =
          (dir != null
              ? File.createTempFile(UNTITLED_FILE, ".xml", dir)
              : File.createTempFile(UNTITLED_FILE, ".xml"));
      // We don't actually need the file on disk
      file.delete();
    } catch (IOException io) {
      file =
          (dir != null ? new File(dir, UNTITLED_FILE + ".xml") : new File(UNTITLED_FILE + ".xml"));
    }
    return file;
  }

  public String getName() {
    return filename;
  }

  public void setForked(boolean fork) {
    this.fork = fork;
  }

  public boolean isForked() {
    return fork;
  }

  public void setVMArgs(String args) {
    if ("".equals(args)) {
      args = null;
    }
    vmargs = args;
  }

  public String getVMArgs() {
    return vmargs;
  }

  public boolean isSlowPlayback() {
    return slow;
  }

  public void setSlowPlayback(boolean slow) {
    this.slow = slow;
  }

  public boolean isAWTMode() {
    return awt;
  }

  public void setAWTMode(boolean awt) {
    this.awt = awt;
  }

  public File getFile() {
    File file = new File(filename);
    if (!file.isAbsolute()) {
      String path = getRelativeTo().getPath() + File.separator + filename;
      file = new File(path);
    }
    return file;
  }

  /**
   * Change the file system basis for the current script.  Does not affect the script contents.
   *
   * @param file file
   * @deprecated Use {@link #setFile(File)}.
   */
  @Deprecated
  public void changeFile(File file) {
    setFile(file);
  }

  public void setFile(File file) {
    Log.debug("Script file set to " + file);
    if (file == null) {
      throw new IllegalArgumentException("File must not be null");
    }
    if (filename == null || !file.equals(getFile())) {
      filename = file.getPath();
      Log.debug("Script filename set to " + filename);
      if (relativeDirectory != null) {
        setRelativeTo(relativeDirectory);
      }
    }
    lastSaved = getHash() + 1;
  }

  /**
   * Typical xml header, so we can know about how much file prefix to skip.
   */
  private static final String XML_INFO = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

  /**
   * Flag to indicate whether emitted XML should contain the script contents.  Sometimes we just
   * want a one-liner (like when displaying in the script editor), and sometimes we want the full
   * contents (when writing to file).
   */
  private boolean formatForSave = false;

  public void save(Writer writer) throws IOException {
    formatForSave = true;
    Element el = toXML();
    formatForSave = false;
    el.setName(TAG_AWT_TEST_SCRIPT);

    Document doc = DocumentHelper.createDocument(el);
    doc.write(writer);
  }

  @Override
  public String toEditableString() {
    return getFilename();
  }

  public boolean isDirty() {
    return getHash() != lastSaved;
  }

  public void save() throws IOException {
    File file = getFile();
    Log.debug("Saving script to '" + file + "' " + hashCode());
    OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
    save(new BufferedWriter(writer));
    writer.close();
    lastSaved = getHash();
  }

  /**
   * Ensure that all referenced components are actually in the components list.
   */
  private synchronized void verify() throws InvalidScriptException {
    Log.debug("verifying all referenced refs exist");
    for (ComponentReference ref : refs.values()) {
      String id = ref.getAttribute(TAG_PARENT);
      if (id != null && refs.get(id) == null) {
        String msg = Strings.get("script.parent_missing", new Object[] {id});
        throw new InvalidScriptException(msg);
      }
      id = ref.getAttribute(TAG_WINDOW);
      if (id != null && refs.get(id) == null) {
        String msg = Strings.get("script.window_missing", new Object[] {id});
        throw new InvalidScriptException(msg);
      }
    }
  }

  /**
   * Make the path to the given child script relative to this one.
   */
  private void updateRelativePath(Step child) {
    // Make sure included scripts are located relative to this one
    if (child instanceof Script) {
      ((Script) child).setRelativeTo(getDirectory());
    }
  }

  @Override
  protected void parseChild(Element el) throws InvalidScriptException {
    if (el.getName().equals(TAG_COMPONENT)) {
      addComponentReference(el);
    } else {
      synchronized (steps()) {
        super.parseChild(el);
        updateRelativePath((Step) steps().get(size() - 1));
      }
    }
  }

  protected void parseAttributes(Map<String, String> map) {
    parseStepAttributes(map);
    fork = Boolean.parseBoolean(map.get(TAG_FORKED));
    slow = Boolean.parseBoolean(map.get(TAG_SLOW));
    awt = Boolean.parseBoolean(map.get(TAG_AWT));
    vmargs = map.get(TAG_VMARGS);
  }

  /**
   * Loads the XML test script.  Performs a check against the XML schema.
   *
   * @param reader Provides the script data
   * @throws InvalidScriptException invalid script
   */
  public void load(Reader reader) throws InvalidScriptException {
    clear();
    /*        try {
                // Set things up to optionally validate on load
                SAXBuilder builder =
                    new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
                if (validate) {
                    URL url = getClass().getClassLoader().getResource("abbot/abbot.xsd");
                    if (url != null) {
                        builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
                        builder.setFeature("http://apache.org/xml/features/validation/schema", true);
                        builder.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", url.toString());
                    }
                    else {
                        Log.warn("Could not find abbot/abbot.xsd, disabling XML validation");
                        validate = false;
                    }
                }
                Document doc = builder.build(reader);
                Element root = doc.getRootElement();
                Map map = createAttributeMap(root);
                parseAttributes(map);
                parseChildren(root);
            }
            catch(JDOMException e) {
                throw new InvalidScriptException(e.getMessage());
            }
    */
    // Make sure we have all referenced components
    synchronized (this) {
      synchReferenceIDs();
      verify();
    }
    lastSaved = getHash();
  }

  @Override
  public void addStep(int index, Step step) {
    super.addStep(index, step);
    updateRelativePath(step);
  }

  @Override
  public void addStep(Step step) {
    super.addStep(step);
    updateRelativePath(step);
  }

  @Override
  public void setStep(int index, Step step) {
    super.setStep(index, step);
    updateRelativePath(step);
  }

  public void load() throws IOException {
    File file = getFile();
    if (!file.exists()) {
      if (!getFilename().contains(Script.UNTITLED_FILE)) {
        Log.warn("Script " + this + " does not exist, ignoring it");
      }
      return;
    }
    if (!file.isFile()) {
      throw new InvalidScriptException("Path " + getFilename() + " refers to a directory");
    }
    if (file.length() != 0) {
      try (Reader reader =
          new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {

        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
          load(bufferedReader);
        }
      } catch (FileNotFoundException e) {
        // should have been detected
        Log.warn("File '" + file + "' exists but is not found");
      }
    } else {
      Log.warn("Script file " + this + " is empty");
    }
  }

  protected String getFullXMLString() {
    try {
      formatForSave = true;
      return toXMLString(this);
    } finally {
      formatForSave = false;
    }
  }

  private int getHash() {
    return getFullXMLString().hashCode();
  }

  @Override
  public String getXMLTag() {
    return TAG_SCRIPT;
  }

  @Override
  public Element addContent(Element el) {
    // Only save content if writing to disk
    if (formatForSave) {
      synchReferenceIDs();
      Collection<ComponentReference> values = refs.values();
      for (ComponentReference cref : new TreeSet<>(values)) {
        el.add(cref.toXML());
      }
      // Now collect our child steps
      return super.addContent(el);
    }
    return el;
  }

  public String getFilename() {
    return filename;
  }

  @Override
  public Map<String, String> getAttributes() {
    Map<String, String> map;
    if (!formatForSave) {
      map = new HashMap<>();
      map.put(TAG_FILENAME, getFilename());
    } else {
      map = super.getAttributes();
      // default is no fork
      if (fork) {
        map.put(TAG_FORKED, "true");
        if (vmargs != null) {
          map.put(TAG_VMARGS, vmargs);
        }
      }
      if (slow) {
        map.put(TAG_SLOW, "true");
      }
      if (awt) {
        map.put(TAG_AWT, "true");
      }
    }
    return map;
  }

  @Override
  protected void runStep(StepRunner runner) throws Throwable {
    components.clear();
    properties.clear();
    // Make all files relative to this script
    Parser fc =
        new FileParser() {
          @Override
          public String relativeTo() {
            Log.debug(
                "All file references will be relative to " + getDirectory().getAbsolutePath());
            return getDirectory().getAbsolutePath();
          }
        };
    Parser oldfc = ArgumentParser.setParser(File.class, fc);
    int oldDelay = Robot.getAutoDelay();
    int oldMode = Robot.getEventMode();
    if (slow) {
      Robot.setAutoDelay(slowDelay);
    }
    if (awt) {
      Robot.setEventMode(Robot.EM_AWT);
    }

    try {
      super.runStep(runner);
    } finally {
      Robot.setAutoDelay(oldDelay);
      Robot.setEventMode(oldMode);
      ArgumentParser.setParser(File.class, oldfc);
    }
  }

  @Override
  public void clear() {
    setScriptError(null);
    refs = Collections.unmodifiableMap(new HashMap<>());
    components.clear();
    super.clear();
  }

  @Override
  public String getUsage() {
    return USAGE;
  }

  @Override
  public String getDefaultDescription() {
    String ext = fork ? " &" : "";
    String desc = Strings.get("script.desc", new Object[] {getFilename(), ext});
    return desc.contains(UNTITLED_FILE) ? UNTITLED : desc;
  }

  public boolean hasLaunch() {
    // First step might be a Launch or a Fixture
    return size() > 0 && (steps().get(0) instanceof UIContext);
  }

  public UIContext getUIContext() {
    synchronized (steps()) {
      if (hasLaunch()) {
        return (UIContext) steps().get(0);
      }
    }
    return null;
  }

  /**
   * Defer to the {@link UIContext} to obtain a {@link ClassLoader}, or use the current
   * {@link Thread}'s context class loader.
   *
   * @return class loader
   * @see Thread#getContextClassLoader()
   */
  public ClassLoader getContextClassLoader() {
    UIContext context = getUIContext();
    return context != null
        ? context.getContextClassLoader()
        : Thread.currentThread().getContextClassLoader();
  }

  public boolean hasTerminate() {
    return size() > 0 && (steps().get(size() - 1) instanceof Terminate);
  }

  public File getRelativeTo() {
    if (relativeDirectory == null) {
      return new File(System.getProperty("user.dir"));
    }
    return relativeDirectory;
  }

  public void setRelativeTo(File dir) {
    Log.debug("Want relative dir " + dir);
    relativeDirectory = dir;
    if (dir != null) {
      // FIXME ideally, we'd want a more robust "make relative" here.
      // for now, simply check to see if the relpath is a prefix.
      // or if the file itself is relative
      String relPath = dir.getPath();
      if (filename.startsWith(relPath) && relPath.length() < filename.length()) {
        char ch = filename.charAt(relPath.length());
        if (ch == '/' || ch == '\\') {
          filename = filename.substring(relPath.length() + 1);
        }
      }
    }
    Log.debug("Relative dir set to " + relativeDirectory + " for " + this);
  }

  public static boolean isScript(File file) {
    if (file.length() == 0) {
      return true;
    }
    if (!file.exists() || !file.isFile() || file.length() < TAG_AWT_TEST_SCRIPT.length() * 2 + 5) {
      return false;
    }
    try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
      int len = XML_INFO.length() + TAG_AWT_TEST_SCRIPT.length() + 15;
      byte[] buf = new byte[len];
      is.read(buf, 0, buf.length);
      String str = new String(buf);
      return str.contains(TAG_AWT_TEST_SCRIPT);
    } catch (Exception exc) {
      return false;
    }
  }

  public File getDirectory() {
    return getFile().getParentFile();
  }

  public Collection<ComponentReference> getComponentReferences() {
    return new TreeSet<>(refs.values());
  }

  public void addComponentReference(ComponentReference ref) {
    Log.debug("adding " + ref);
    synchReferenceIDs();
    Map<String, ComponentReference> map = new HashMap<>(refs);
    map.put(ref.getID(), ref);
    // atomic update of references map
    refs = Collections.unmodifiableMap(map);
  }

  // FIXME: a repaint (tree locked) which accesses the refs list
  // deadlocks with cref creation (locks refs, asks for tree lock)
  // Either get tree lock first or don't require refs lock on read
  public ComponentReference addComponent(Component comp) {
    synchReferenceIDs();
    Log.debug("look up existing for " + Robot.toString(comp));
    Map<String, ComponentReference> newRefs = new HashMap<>();
    ComponentReference ref = ComponentReference.getReference(this, comp, newRefs);
    Log.debug("adding " + Robot.toString(comp));
    Map<String, ComponentReference> map = new HashMap<>(refs);
    map.putAll(newRefs);
    for (ComponentReference r : newRefs.values()) {
      Component c = r.getCachedLookup(getHierarchy());
      if (c != null) {
        components.put(c, r);
      }
    }
    // atomic update of references map
    refs = Collections.unmodifiableMap(map);
    return ref;
  }

  /**
   * Add a new component reference to the script.  For use only when parsing a script.
   */
  private ComponentReference addComponentReference(Element el) throws InvalidScriptException {
    synchReferenceIDs();
    ComponentReference ref = new ComponentReference(this, el);
    Log.debug("adding " + el);
    Map<String, ComponentReference> map = new HashMap<>(refs);
    map.put(ref.getID(), ref);
    // atomic update of references map
    refs = Collections.unmodifiableMap(map);
    return ref;
  }

  public ComponentReference getComponentReference(Component comp) {
    if (!getHierarchy().contains(comp)) {
      String msg = Strings.get("script.not_in_hierarchy", new Object[] {comp.toString()});
      throw new IllegalArgumentException(msg);
    }
    synchReferenceIDs();
    // Clear the component map if any one of the mappings is invalid
    for (ComponentReference cr : refs.values()) {
      if (cr.getCachedLookup(getHierarchy()) == null) {
        components.clear();
        break;
      }
    }
    ComponentReference ref = components.get(comp);
    if (ref != null) {
      if (ref.getCachedLookup(getHierarchy()) != null) {
        return ref;
      }
      components.remove(comp);
    }
    ref = ComponentReference.matchExisting(comp, refs.values());
    if (ref != null) {
      components.put(comp, ref);
    }
    return ref;
  }

  public ComponentReference getComponentReference(String name) {
    synchReferenceIDs();
    return refs.get(name);
  }

  public void setProperty(String name, Object value) {
    if (value == null) {
      properties.remove(name);
    } else {
      properties.put(name, value);
    }
  }

  public Object getProperty(String name) {
    Object value = properties.get(name);
    // Lazy-load the interpreter, so it's only instantiated when required
    if (value == null && INTERPRETER.equals(name)) {
      //       Interpreter bsh = new Interpreter(this);
      //      properties.put(name, value = bsh);
      properties.put(name, "bsh");
    }
    return value;
  }

  public Hierarchy getHierarchy() {
    Resolver r = getResolver();
    if (r != null && r != this) {
      return r.getHierarchy();
    }
    return hierarchy != null ? hierarchy : AWTHierarchy.getDefault();
  }

  public String getContext(Step step) {
    return getFile().toString() + ":" + getLine(this, step);
  }

  public static File getFile(Step step) {
    String context = step.getResolver().getContext(step);
    int colon = context.indexOf(":");
    if (colon == 1
        && Character.isLetter(context.charAt(0))
        && Platform.isWindows()
        && context.length() > 2) {
      colon = context.indexOf(":", 2);
    }
    if (colon != -1) {
      context = context.substring(0, colon);
    }
    return new File(context);
  }

  public static int getLine(Step step) {
    String context = step.getResolver().getContext(step);
    int colon = context.indexOf(":");
    if (colon == 1
        && Character.isLetter(context.charAt(0))
        && Platform.isWindows()
        && context.length() > 2) {
      colon = context.indexOf(":", 2);
    }
    context = context.substring(colon + 1);
    try {
      return Integer.parseInt(context);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static int getLine(Sequence seq, Step step) {
    int line = -1;
    int index = seq.indexOf(step);
    if (index == -1) {
      List<Step> list = seq.steps();
      for (int i = 0; i < list.size(); i++) {
        Step sub = list.get(i);
        if (sub instanceof Sequence) {
          int subline = getLine((Sequence) sub, step);
          if (subline != -1) {
            line = countLines(seq, i) + subline;
            break;
          }
        }
      }
    } else {
      line = countLines(seq, index);
    }
    return line;
  }

  public static int countLines(Sequence seq, int index) {
    int count = 1;
    int limit = index;
    if (limit == -1) {
      limit = seq.size();
      // Empty sequences take a single line
      if (limit == 0) {
        return 1;
      }
      // Otherwise, 2 + the line count of the contents
      count = 2;
    }
    if (seq instanceof Script) {
      // Add in one line per component reference in the script
      // Plus two lines for the <xml> and <AWTTestScript> lines
      count += ((Script) seq).getComponentReferences().size() + 2;
    }
    for (int i = 0; i < limit; i++) {
      Step step = seq.getStep(i);
      // Included scripts take only one line
      if (step instanceof Script) {
        ++count;
      } else if (step instanceof Sequence) {
        count += countLines((Sequence) step, -1);
      } else {
        ++count;
      }
    }
    return count;
  }
}
