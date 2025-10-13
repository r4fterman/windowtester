package abbot.script;

import static java.util.stream.Collectors.toMap;

import abbot.Log;
import abbot.i18n.Strings;
import abbot.tester.ComponentTester;
import abbot.tester.Robot;
import java.awt.Component;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Provides access to one step (line) from a script.  A Step is the basic unit of execution.
 * <b>Custom Step classes</b><p>
 * All custom {@link Step} classes must supply a {@link Constructor} with the signature
 * <code>&lt;init&gt;(${link Resolver}, {@link Map})</code>.  If the step has contents (e.g.
 * {@link Sequence}), then it should also provide a {@link Constructor} with the signature
 * <code>&lt;init&gt;({@link Resolver}, {@link Element}, {@link Map})</code>.
 * <p>
 * The XML tag for a given {@link Step} will be used to auto-generate the {@link Step} class name,
 * e.g. the tag &lt;aphrodite/&gt; causes the parser to create an instance of class
 * <code>abbot.script.Aphrodite</code>, using one of the {@link Constructor}s described above.
 * <p>
 * All derived classes should include an entry in the
 * <a href={@docRoot}/../abbot.xsd>schema</a>, or validation must be turned
 * off by setting the System property
 * <code>abbot.script.validate=false</code>.
 * <p>
 * You can make the custom <code>Aphrodite</code> step do just about anything by overriding the
 * {@link #runStep()} method.
 * <p>
 * See the source for any {@link Step} implementation in this package for examples.
 */
public abstract class Step implements XMLConstants, XMLifiable, Serializable {

  private String description;
  private final Resolver resolver;

  /**
   * Error encountered on parse.
   */
  private Throwable invalidScriptError = null;

  public Step(Resolver resolver, Map<String, String> attributes) {
    this(resolver, "");
    Log.debug("Instantiating " + getClass());
    if (Log.expectDebugOutput) {
      attributes.forEach((key, value) -> Log.debug(key + "=" + value));
    }
    parseStepAttributes(attributes);
  }

  public Step(Resolver resolver, String description) {
    // Kind of a hack; a Script is its own resolver
    if (resolver == null) {
      if (!(this instanceof Resolver)) {
        throw new Error("Resolver must be provided");
      }
      resolver = (Resolver) this;
    } else if (this instanceof Resolver res) {
      resolver = res;
    }
    this.resolver = resolver;
    if ("".equals(description)) {
      description = null;
    }
    this.description = description;
  }

  protected final void parseStepAttributes(Map<String, String> attributes) {
    Log.debug("Parsing attributes for " + getClass());
    description = attributes.get(TAG_DESC);
  }

  public final void run() throws Throwable {
    if (invalidScriptError != null) {
      throw invalidScriptError;
    }
    Log.debug("Running " + this);
    runStep();
  }

  protected abstract void runStep() throws Throwable;

  public String getDescription() {
    return description != null ? description : getDefaultDescription();
  }

  public void setDescription(String desc) {
    description = desc;
  }

  public abstract String getXMLTag();

  public abstract String getUsage();

  public abstract String getDefaultDescription();

  protected void setScriptError(Throwable thr) {
    if (invalidScriptError == null) {
      invalidScriptError = thr;
    } else {
      Log.warn("More than one script error encountered: " + thr);
      Log.warn("Already have: " + invalidScriptError);
    }
  }

  /**
   * Throw an invalid script exception describing the proper script usage.  This should be used by
   * derived classes whenever parsing indicates invalid input.
   */
  protected void usage() {
    usage(null);
  }

  protected void usage(String details) {
    String msg = getUsage();
    if (details != null) {
      msg = Strings.get("step.usage", new Object[] {msg, details});
    }
    setScriptError(new InvalidScriptException(msg));
  }

  public Map<String, String> getAttributes() {
    Map<String, String> map = new HashMap<>();
    if (description != null && !description.equals(getDefaultDescription())) {
      map.put(TAG_DESC, description);
    }
    return map;
  }

  public Resolver getResolver() {
    return resolver;
  }

  protected Element addContent(Element el) {
    return el;
  }

  protected Element addAttributes(Element el) {
    // Use a TreeMap to keep the attributes sorted on output
    new TreeMap<>(getAttributes())
        .forEach(
            (key, value) -> {
              if (value == null) {
                Log.warn("Attribute '" + key + "' value was null in step " + getXMLTag());
                value = "";
              }
              el.addAttribute(key, value);
            });
    return el;
  }

  public String toEditableString() {
    return toXMLString(this);
  }

  public static String toXMLString(XMLifiable obj) {
    // Comments are the only things that aren't actually elements...
    if (obj instanceof Comment comment) {
      return "<!-- " + comment.getDescription() + " -->";
    }
    Element el = obj.toXML();
    StringWriter writer = new StringWriter();
    try {
      el.write(writer);
    } catch (IOException io) {
      Log.warn(io);
    }
    return writer.toString();
  }

  public Element toXML() {
    Element el = DocumentHelper.createElement(getXMLTag());
    return addAttributes(addContent(el));
  }

  public static Step createStep(Resolver resolver, String str)
      throws InvalidScriptException, IOException {
    try {
      Document doc = DocumentHelper.parseText(str);
      Element el = doc.getRootElement();
      return createStep(resolver, el);
    } catch (DocumentException e) {
      throw new InvalidScriptException(e.getMessage());
    }
  }

  protected static Map<String, String> createAttributeMap(Element el) {
    Log.debug("Creating attribute map for " + el);
    return el.attributes().stream().collect(toMap(Attribute::getName, Attribute::getValue));
  }

  public static Step createStep(Resolver resolver, Element el) throws InvalidScriptException {
    String tag = el.getName();
    String name = tag.substring(0, 1).toUpperCase() + tag.substring(1);

    Map<String, String> attributes;
    if (tag.equals(TAG_WAIT)) {
      attributes =
          Stream.concat(
                  createAttributeMap(el).entrySet().stream(),
                  Map.of(TAG_WAIT, "true").entrySet().stream())
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      name = "Assert";
    } else {
      attributes = createAttributeMap(el);
    }
    try {
      name = "abbot.script." + name;
      Log.debug("Instantiating " + name);
      Class<?> cls = Class.forName(name);
      try {
        // Steps with contents require access to the XML element
        Class<?>[] argTypes = new Class<?>[] {Resolver.class, Element.class, Map.class};
        Constructor<?> ctor = cls.getConstructor(argTypes);
        return (Step) ctor.newInstance(resolver, el, attributes);
      } catch (NoSuchMethodException nsm) {
        // All steps must support this ctor
        Class<?>[] argTypes = new Class<?>[] {Resolver.class, Map.class};
        Constructor<?> ctor = cls.getConstructor(argTypes);
        return (Step) ctor.newInstance(resolver, attributes);
      }
    } catch (ClassNotFoundException cnf) {
      String msg = Strings.get("step.unknown_tag", new Object[] {tag});
      throw new InvalidScriptException(msg);
    } catch (InvocationTargetException ite) {
      Log.warn(ite);
      throw new InvalidScriptException(ite.getTargetException().getMessage());
    } catch (Exception exc) {
      Log.warn(exc);
      throw new InvalidScriptException(exc.getMessage());
    }
  }

  protected String simpleClassName(Class<?> cls) {
    return Robot.simpleClassName(cls);
  }

  public String toString() {
    return getDescription();
  }

  public Class<?> resolveClass(String className) throws ClassNotFoundException {
    ClassLoader cl = getResolver().getContextClassLoader();
    return Class.forName(className, true, cl);
  }

  /**
   * Look up an appropriate ComponentTester given an arbitrary Component-derived class. If the class is derived from
   * abbot.tester.ComponentTester, instantiate one; if it is derived from java.awt.Component, return a matching
   * Tester. Otherwise, return abbot.tester.ComponentTester.
   *
   * @param className class name
   * @return component tester
   * @throws ClassNotFoundException   If the given class can't be found.
   * @throws IllegalArgumentException If the tester cannot be instantiated.
   */
  protected ComponentTester resolveTester(String className) throws ClassNotFoundException {
    Class<?> testedClass = resolveClass(className);
    if (Component.class.isAssignableFrom(testedClass)) {
      return ComponentTester.getTester(testedClass);
    } else if (ComponentTester.class.isAssignableFrom(testedClass)) {
      try {
        return (ComponentTester) testedClass.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        String msg =
            "Custom ComponentTesters must provide "
                + "an accessible no-args Constructor: "
                + e.getMessage();
        throw new IllegalArgumentException(msg);
      }
    }
    String msg = "The given class '" + className + "' is neither a Component nor a ComponentTester";
    throw new IllegalArgumentException(msg);
  }

  @Serial
  private void writeObject(ObjectOutputStream out) {
    // NOTE: this is only to avoid drag/drop errors
    out = null;
  }

  @Serial
  private void readObject(ObjectInputStream in) {
    // NOTE: this is only to avoid drag/drop errors
    in = null;
  }
}
