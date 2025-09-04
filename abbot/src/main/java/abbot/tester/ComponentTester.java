package abbot.tester;

import abbot.BugReport;
import abbot.Log;
import abbot.WaitTimedOutError;
import abbot.finder.AWTHierarchy;
import abbot.finder.BasicFinder;
import abbot.finder.ComponentNotFoundException;
import abbot.finder.ComponentSearchException;
import abbot.finder.Hierarchy;
import abbot.finder.MultipleComponentsFoundException;
import abbot.finder.matchers.WindowMatcher;
import abbot.i18n.Strings;
import abbot.script.ComponentReference;
import abbot.script.Condition;
import abbot.util.AWT;
import abbot.util.Bugs;
import abbot.util.WeakAWTEventListener;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleIcon;

/**
 * Provides basic programmatic operation of a {@link Component} and related UI objects such as
 * windows, menus and menu bars throuh action methods. Also provides some useful assertions about
 * properties of a {@link Component}.
 * <p>
 * There are two sets of event-generating methods.  The internal, protected methods inherited from
 * {@link abbot.tester.Robot abbot.tester.Robot} are for normal programmatic use within derived
 * Tester classes.  No event queue synchronization should be performed except when modifying a
 * component for which results are required for the action itself.
 * <p>
 * The public <code>actionXXX</code> functions are meant to be invoked from a script or directly
 * from a hand-written test.  These actions are distinguished by name, number of arguments, and by
 * argument type.  The actionX methods will be synchronized with the event dispatch thread when
 * invoked, so you should only do synchronization with waitForIdle when you depend on the results of
 * a particular event prior to sending the next one (event.g. scrolling a table cell into view
 * before selecting it). All public action methods should ensure that the actions they trigger are
 * finished on return, or will be finished before any subsequent actions are requested.
 *
 * <i>Action</i> methods generally represent user-driven actions such
 * as menu selection, table selection, popup menus, etc.  All actions should have the following
 * signature:
 * <blockquote>
 * <code>public void actionSpinMeRoundLikeARecord(Component component, ...);</code>
 * <code>public void actionPinchMe(Component component, ComponentLocation location);</code>
 * </blockquote>
 * <p>
 * It is essential that the argument is of type {@link Component}; if you use a more-derived class,
 * then the actual invocation becomes ambiguous since method parsing doesn't attempt to determine
 * which identically-named method is the most-derived.
 * <p>
 * The {@link ComponentLocation} abstraction allows all derived tester classes to inherit click,
 * popup menu, and drag variants without having to explicitly define new methods for
 * component-specific substructures.  The new class need only define the
 * {@link #parseLocation(String)} method, which should return a location specific to the component
 * in question.
 *
 * <i>Assertions</i> are either independent of any component (and should be
 * implemented in this class), or take a component as the first argument, and perform some check on
 * that component.  All assertions should have one of the following signatures:
 * <blockquote>
 * <code>public boolean assertMyself(...);</code><br>
 * <code>public boolean assertBorderIsAtrociouslyUgly(Component component, ...);</code>
 * </blockquote>
 * Note that these assertions do not throw exceptions but rather return a
 * <code>boolean</code> value indicating success or failure.  Normally these
 * assertions will be wrapped by an abbot.script.Assert step if you want to cause a test failure, or
 * you can manually throw the proper failure exception if the method returns false.
 *
 * <i>Property checks</i> may also be implemented in cases location the component
 * "property" might not be readily available or easily comparable, event.g. see
 * {@link abbot.tester.JPopupMenuTester#getMenuLabels(Component)}.
 * <blockquote>
 * <code>public Object getHairpiece(Component component);</code><br>
 * <code>public boolean isRighteouslyIndignant(Component component);</code>
 * </blockquote>
 * Any non-property methods with the property signature, should be added to the
 * {@link #IGNORED_METHODS} set, since property-like methods are scanned dynamically to populate the
 * {@link abbot.editor.ScriptEditor editor}'s action menus.
 *
 * <h2>Extending ComponentTester</h2>
 * Following are the steps required to implement a Tester object for a custom class.
 * <ul>
 * <li><h3>Create the Tester Class</h3>
 * Derive from this class to implement actions and assertions specific to
 * a given component class.  Testers for any classes found in the JRE
 * (i.event. in the {@link java.awt} or {@link javax.swing} packages) should be
 * in the {@link abbot.tester abbot.tester} package.  Extensions (testers for
 * any <code>Component</code> subclasses not found in the JRE) must be in the
 * <code>abbot.tester.extensions</code> package and be
 * named the name of the <code>Component</code> subclass followed by "Tester".
 * For example, the {@link javax.swing.JButton javax.swing.JButton} tester
 * class is {@link JButtonTester abbot.tester.JButtonTester}, and a tester
 * for <code>org.me.PR0NViewer</code> would be
 * <code>abbot.tester.extensions.PR0NViewerTester</code>.
 * <li><h3>Add Action Methods</h3>
 * Add <code>action</code> methods which effect user actions.
 * See the section on <a href=Naming>naming conventions</a> below.
 * <li><h3>Add Assertion Methods</h3>
 * Add <code>assert</code> methods to access attributes not readily available
 * as properties<br>
 * <li><h3>Add Substructure Support</h3>
 * Add a corresponding <code>ComponentLocation</code> implementation to
 * handle any substructure present in your <code>Component</code>.
 * See {@link ComponentLocation} for details.
 * <li><h3>Add Substructure-handling Methods</h3>
 * <ul>
 * <li>{@link #parseLocation(String)}<br>
 * Convert the given <code>String</code> into an instance of
 * {@link ComponentLocation} specific to your Tester.  Here is an example
 * implementation from {@link JListTester}:<br>
 * <pre>
 * public ComponentLocation parseLocation(String encoded) {
 * return new JListLocation().parse(encoded);
 * }
 * </pre>
 * <li>{@link #getLocation(Component, Point)}<br>
 * Use the given <code>Point</code> to create a {@link ComponentLocation}
 * instance appropriate for the <code>Component</code> substructure
 * available at that location.  For example, on a {@link javax.swing.JList},
 * you would return a {@link JListLocation} based on the
 * stringified value at that location, the row/index at that
 * location, or the raw <code>Point</code>,
 * in order of preference.  If a stringified value is unavailable,
 * fall back to an indexed position; if that is not possible (if, for
 * instance, the location is outside the list contents), return a
 * {@link ComponentLocation} based on the raw <code>Point</code>.
 * </ul>
 * <li><h3>Set Editor Properties</h3>
 * Add-on tester classes should set the following system properties so that
 * the actions provided by their tester can be properly displayed in the
 * script editor.  For an action <code>actionWiggle</code> provided by class
 * <code>abbot.tester.extensions.PR0NViewerTester</code>, the following
 * properties should be defined:<br>
 * <ul>
 * <li><code>actionWiggle.menu</code> short name for Insert menu
 * <li><code>actionWiggle.description</code> short description (optional)
 * <li><code>actionWiggle.icon</code> icon for the action (optional)
 * <li><code>PR0NViewerTester.actionWiggle.args</code> javadoc-style
 * description of method, displayed when asking the user for its arguments
 * </ul>
 * Since these properties are global, if your Tester class defines a method
 * which is also defined by another Tester class, you must supply the class
 * name as a prefix to the property.
 * </ul>
 *
 * <h2>Method Naming Conventions</h2>
 * Action methods should be named according to the human-centric action that
 * is being performed if at all possible.  For example, use
 * <code>actionSelectRow</code> in a List rather than
 * <code>actionSelectIndex</code>.  If there's no common usage, or if the
 * usage is too vague or diverse, use the specific terms used in code.
 * For example, {@link JScrollBarTester} uses
 * {@link JScrollBarTester#actionScrollUnitUp(Component) actionScrollUnitUp()}
 * and
 * {@link JScrollBarTester#actionScrollBlockUp(Component) actionScrollBlockUp()};
 * since there is no common language usage for these
 * concepts (line and page exist, but are not appropriate if what is scrolled
 * is not text).
 * <p>
 * When naming a selection method, include the logical substructure target of
 * the selection in the name (event.g.
 * {@link JTableTester#actionSelectCell(Component, JTableLocation)}
 * JTableTester.actionSelectCell()),
 * since some components may have more than one type of selectable item
 * within them.
 */
public class ComponentTester extends Robot {

  /**
   * Add any method names here which should <em>not</em> show up in a dynamically generated list of
   * property methods. Omit from method lookup deprecated methods or others we want to ignore
   */
  private static final Set<String> IGNORED_METHODS =
      Set.of(
          "actionSelectAWTMenuItemByLabel",
          "actionSelectAWTPopupMenuItemByLabel",
          "getTag",
          "getTester",
          "isOnPopup",
          "getLocation");

  /**
   * Maps class names to their corresponding Tester object.
   */
  private static final HashMap<String, ComponentTester> testers = new HashMap<>();

  public static void setTester(Class<?> forClass, ComponentTester tester) {
    testers.put(forClass.getName(), tester);
  }

  public static ComponentTester getTester(Component comp) {
    return comp != null ? getTester(comp.getClass()) : getTester(Component.class);
  }

  public static ComponentTester getTester(Class<?> componentClass) {
    String className = componentClass.getName();
    ComponentTester tester = testers.get(className);
    if (tester == null) {
      if (!Component.class.isAssignableFrom(componentClass)) {
        String msg = "Class " + className + " is not derived from java.awt.Component";
        throw new IllegalArgumentException(msg);
      }
      Log.debug("Looking up tester for " + componentClass);
      String testerName = simpleClassName(componentClass) + "Tester";
      Package pkg = ComponentTester.class.getPackage();
      String pkgName = "";
      if (pkg == null) {
        Log.warn(
            "ComponentTester.class has null package; "
                + "the class loader is likely flawed: "
                + ComponentTester.class.getClassLoader()
                + ", "
                + Thread.currentThread().getContextClassLoader(),
            Log.FULL_STACK);
        pkgName = "abbot.tester";
      } else {
        pkgName = pkg.getName();
      }
      if (className.startsWith("javax.swing.") || className.startsWith("java.awt.")) {
        tester = findTester(pkgName + "." + testerName, componentClass);
      }
      if (tester == null) {
        tester = findTester(pkgName + ".extensions." + testerName, componentClass);
        if (tester == null) {
          tester = getTester(componentClass.getSuperclass());
        }
      }
      if (!tester.isExtension()) {
        // Only cache it if it's part of the standard framework,
        // but cache it for every level that we looked up, so we
        // don't repeat the effort.
        testers.put(componentClass.getName(), tester);
      }
    }

    return tester;
  }

  public final boolean isExtension() {
    return getClass().getName().startsWith("abbot.tester.extensions");
  }

  /**
   * Look up the given class, using special class loading rules to maintain framework consistency.
   */
  private static Class<?> resolveClass(String testerName, Class<?> componentClass)
      throws ClassNotFoundException {
    // Extension testers must be loaded in the context of the code under
    // test.
    Class<?> cls;
    if (testerName.startsWith("abbot.tester.extensions")) {
      cls = Class.forName(testerName, true, componentClass.getClassLoader());
    } else {
      cls = Class.forName(testerName);
    }
    Log.debug("Loaded class " + testerName + " with " + cls.getClassLoader());
    return cls;
  }

  /**
   * Look up the given class with a specific class loader.
   */
  private static ComponentTester findTester(String testerName, Class<?> componentClass) {
    ComponentTester tester = null;
    Class<?> testerClass = null;
    try {
      testerClass = resolveClass(testerName, componentClass);
      tester = (ComponentTester) testerClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException ie) {
      Log.warn(ie);
    } catch (ClassNotFoundException cnf) {
      // Log.debug("Class " + testerName + " not found");
    } catch (ClassCastException cce) {
      throw new BugReport(
          "Class loader conflict: environment "
              + ComponentTester.class.getClassLoader()
              + " vs. "
              + testerClass.getClassLoader());
    } catch (InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    return tester;
  }

  protected String deriveAccessibleTag(AccessibleContext context) {
    String tag = null;
    if (context != null) {
      if (context.getAccessibleName() != null) {
        tag = context.getAccessibleName();
      }
      if ((tag == null || tag.isEmpty())
          && context.getAccessibleIcon() != null
          && context.getAccessibleIcon().length > 0) {
        AccessibleIcon[] icons = context.getAccessibleIcon();
        tag = icons[0].getAccessibleIconDescription();
        if (tag != null) {
          tag = tag.substring(tag.lastIndexOf("/") + 1);
          tag = tag.substring(tag.lastIndexOf("\\") + 1);
        }
      }
    }
    return tag;
  }

  /**
   * Default methods to use to derive a component-specific tag.  These are things that will probably
   * be useful in a custom component if the method is supported.
   */
  private static final String[] tagMethods = {
    "getLabel", "getTitle", "getText",
  };

  public static String getTag(Component component) {
    return getTester(component.getClass()).deriveTag(component);
  }

  protected boolean isCustom(Class<?> cls) {
    return !(cls.getName().startsWith("javax.swing.") || cls.getName().startsWith("java.awt."));
  }

  public String deriveTag(Component comp) {
    // If the component class is custom, don't provide a tag
    if (isCustom(comp.getClass())) {
      return null;
    }

    Method method;
    String tag = null;
    // Try a few default methods
    for (String tagMethod : tagMethods) {
      // Don't use getText on text components
      if (((comp instanceof javax.swing.text.JTextComponent)
              || (comp instanceof java.awt.TextComponent))
          && "getText".equals(tagMethod)) {
        continue;
      }
      try {
        method = comp.getClass().getMethod(tagMethod, null);
        String tmp = (String) method.invoke(comp, null);
        // Don't ever use empty strings for tags
        if (tmp != null && !tmp.isEmpty()) {
          tag = tmp;
          break;
        }
      } catch (Exception e) {
        // do nothing
      }
    }

    // In the absence of any other tag, try to derive one from something
    // recognizable on one of its ancestors.
    if (tag == null || tag.isEmpty()) {
      Component parent = comp.getParent();
      if (parent != null) {
        String ptag = getTag(parent);
        if (ptag != null) {
          // Don't use the tag if it's simply the window title; that
          // doesn't provide any extra information.
          if (!ptag.endsWith(" Root Pane")) {
            StringBuilder buf = new StringBuilder(ptag);
            int under = ptag.indexOf(" under ");
            if (under != -1) {
              buf.delete(0, under + 7);
            }
            buf.insert(0, " under ");
            buf.insert(0, simpleClassName(comp.getClass()));
            tag = buf.toString();
          }
        }
      }
    }

    return tag;
  }

  /**
   * Wait for an idle AWT event queue.  Will return when there are no more events on the event
   * queue.
   */
  public void actionWaitForIdle() {
    waitForIdle();
  }

  public void actionDelay(int ms) {
    delay(ms);
  }

  /**
   * @param menuFrame menu frame
   * @param path      path
   * @deprecated Renamed to {@link #actionSelectAWTMenuItem(Frame, String)}.
   */
  @Deprecated
  public void actionSelectAWTMenuItemByLabel(Frame menuFrame, String path) {
    actionSelectAWTMenuItem(menuFrame, path);
  }

  /**
   * Selects an AWT menu item ({@link java.awt.MenuItem}) and returns when the invocation has
   * triggered (though not necessarily completed).
   *
   * @param menuFrame menu frame
   * @param path      either a unique label or the menu path.
   * @see Robot#selectAWTMenuItem(Frame, String)
   */
  public void actionSelectAWTMenuItem(Frame menuFrame, String path) {
    AWTMenuListener listener = new AWTMenuListener();
    new WeakAWTEventListener(listener, AWTEvent.ACTION_EVENT_MASK);
    selectAWTMenuItem(menuFrame, path);
    long start = System.currentTimeMillis();
    while (listener.isNotEventFired()) {
      if (System.currentTimeMillis() - start > defaultDelay) {
        throw new ActionFailedException("Menu item '" + path + "' failed to fire");
      }
      sleep();
    }
    waitForIdle();
  }

  /**
   * @param invoker invoker
   * @param path    path
   * @deprecated Renamed to {@link #actionSelectAWTPopupMenuItem(Component, String)}.
   */
  @Deprecated
  public void actionSelectAWTPopupMenuItemByLabel(Component invoker, String path) {
    actionSelectAWTPopupMenuItem(invoker, path);
  }

  public void actionSelectAWTPopupMenuItem(Component invoker, String path) {
    AWTMenuListener listener = new AWTMenuListener();
    new WeakAWTEventListener(listener, AWTEvent.ACTION_EVENT_MASK);
    selectAWTPopupMenuItem(invoker, path);
    long start = System.currentTimeMillis();
    while (listener.isNotEventFired()) {
      if (System.currentTimeMillis() - start > defaultDelay) {
        throw new ActionFailedException("Menu item '" + path + "' failed to fire");
      }
      sleep();
    }
    waitForIdle();
  }

  public void actionSelectMenuItem(Component item) {
    Log.debug("Attempting to select menu item " + toString(item));
    selectMenuItem(item);
    waitForIdle();
  }

  public void actionSelectMenuItem(Component sameWindow, String path) {
    Log.debug("Attempting to select menu item '" + path + "'");
    selectMenuItem(sameWindow, path);
    waitForIdle();
  }

  public void actionSelectPopupMenuItem(Component invoker, String path) {
    actionSelectPopupMenuItem(invoker, invoker.getWidth() / 2, invoker.getHeight() / 2, path);
  }

  public void actionSelectPopupMenuItem(
      Component invoker, ComponentLocation location, String path) {
    selectPopupMenuItem(invoker, location, path);
    waitForIdle();
  }

  public void actionSelectPopupMenuItem(Component invoker, int x, int y, String path) {
    actionSelectPopupMenuItem(invoker, new ComponentLocation(new Point(x, y)), path);
  }

  public void actionShowPopupMenu(Component invoker) {
    actionShowPopupMenu(invoker, new ComponentLocation());
  }

  public void actionShowPopupMenu(Component invoker, ComponentLocation location) {
    Point where = location.getPoint(invoker);
    showPopupMenu(invoker, where.x, where.y);
  }

  public void actionShowPopupMenu(Component invoker, int x, int y) {
    showPopupMenu(invoker, x, y);
  }

  public void actionClick(Component component) {
    actionClick(component, new ComponentLocation());
  }

  public void actionClick(Component component, ComponentLocation location) {
    actionClick(component, location, InputEvent.BUTTON1_DOWN_MASK);
  }

  public void actionClick(Component component, ComponentLocation location, int buttons) {
    actionClick(component, location, buttons, 1);
  }

  public void actionClick(Component component, ComponentLocation location, int buttons, int count) {
    Point where = location.getPoint(component);
    click(component, where.x, where.y, buttons, count);
    waitForIdle();
  }

  public void actionClick(Component component, int x, int y) {
    actionClick(component, new ComponentLocation(new Point(x, y)));
  }

  public void actionClick(Component component, int x, int y, int buttons) {
    actionClick(component, x, y, buttons, 1);
  }

  public void actionClick(Component component, int x, int y, int buttons, int count) {
    actionClick(component, new ComponentLocation(new Point(x, y)), buttons, count);
  }

  public void actionKeyPress(Component component, int keyCode) {
    actionFocus(component);
    actionKeyPress(keyCode);
  }

  public void actionKeyPress(int keyCode) {
    keyPress(keyCode);
    waitForIdle();
  }

  public void actionKeyRelease(Component component, int keyCode) {
    actionFocus(component);
    actionKeyRelease(keyCode);
  }

  public void actionKeyRelease(int keyCode) {
    keyRelease(keyCode);
    waitForIdle();
  }

  public void actionKeyStroke(Component component, int keyCode) {
    actionFocus(component);
    actionKeyStroke(keyCode, 0);
  }

  public void actionKeyStroke(int keyCode) {
    actionKeyStroke(keyCode, 0);
  }

  public void actionKeyStroke(Component component, int keyCode, int modifiers) {
    actionFocus(component);
    actionKeyStroke(keyCode, modifiers);
  }

  public void actionKeyStroke(int keyCode, int modifiers) {
    if (Bugs.hasKeyStrokeGenerationBug()) {
      int oldDelay = getAutoDelay();
      setAutoDelay(50);
      key(keyCode, modifiers);
      setAutoDelay(oldDelay);
      delay(100);
    } else {
      key(keyCode, modifiers);
    }
    waitForIdle();
  }

  public void actionKeyString(Component component, String value) {
    actionFocus(component);
    actionKeyString(value);
  }

  public void actionKeyString(String value) {
    keyString(value);
    // FIXME waitForIdle isn't always sufficient on OSX with key events
    if (Bugs.hasKeyStrokeGenerationBug()) {
      delay(100);
    }
    waitForIdle();
  }

  public void actionFocus(Component component) {
    focus(component, true);
  }

  public void actionMouseMove(Component component, ComponentLocation location) {
    Point where = location.getPoint(component);
    mouseMove(component, where.x, where.y);
    waitForIdle();
  }

  public void actionMousePress(Component component, ComponentLocation location) {
    actionMousePress(component, location, InputEvent.BUTTON1_DOWN_MASK);
  }

  public void actionMousePress(Component component, ComponentLocation location, int mask) {
    Point where = location.getPoint(component);
    mousePress(component, where.x, where.y, mask);
    waitForIdle();
  }

  public void actionMouseRelease() {
    mouseRelease();
    waitForIdle();
  }

  public void actionDrag(Component dragSource, ComponentLocation location) {
    actionDrag(dragSource, location, "BUTTON1_DOWN_MASK");
  }

  public void actionDrag(Component dragSource) {
    actionDrag(dragSource, new ComponentLocation());
  }

  /**
   * Perform a drag action with the given modifiers.
   *
   * @param dragSource source of the drag
   * @param location   identifies location on the given {@link Component} to begin the drag.
   * @param modifiers  a <code>String</code> representation of key modifiers, event.g. "ALT|SHIFT",
   *                   based on the {@link InputEvent#ALT_MASK InputEvent fields}.
   * @deprecated Use the
   * {@link #actionDrag(Component, ComponentLocation, int) integer modifier mask} version instead.
   */
  @Deprecated
  public void actionDrag(Component dragSource, ComponentLocation location, String modifiers) {
    actionDrag(dragSource, location, AWT.getModifiers(modifiers));
  }

  /**
   * Perform a drag action with the given modifiers.
   *
   * @param dragSource source of the drag
   * @param location   identifies location on the given {@link Component} to begin the drag.
   * @param modifiers  one or more of the {@link InputEvent#ALT_DOWN_MASK InputEvent fields}.
   */
  public void actionDrag(Component dragSource, ComponentLocation location, int modifiers) {
    Point where = location.getPoint(dragSource);
    drag(dragSource, where.x, where.y, modifiers);
    waitForIdle();
  }

  public void actionDrag(Component dragSource, int sx, int sy) {
    actionDrag(dragSource, new ComponentLocation(new Point(sx, sy)));
  }

  /**
   * Perform a drag action.  Grabs at the given location with the given modifiers.
   *
   * @param dragSource source of the drag
   * @param sx         X coordinate
   * @param sy         Y coordinate
   * @param modifiers  a <code>String</code> representation of key modifiers, event.g. "ALT|SHIFT",
   *                   based on the {@link InputEvent#ALT_DOWN_MASK InputEvent fields}.
   * @deprecated Use the
   * {@link #actionDrag(Component, ComponentLocation, int) ComponentLocation/ integer modifier mask}
   * version instead.
   */
  @Deprecated
  public void actionDrag(Component dragSource, int sx, int sy, String modifiers) {
    actionDrag(dragSource, new ComponentLocation(new Point(sx, sy)), modifiers);
  }

  public void actionDragOver(Component target, ComponentLocation location) {
    Point point = location.getPoint(target);
    dragOver(target, point.x, point.y);
    waitForIdle();
  }

  public void actionDrop(Component dropTarget) {
    actionDrop(dropTarget, new ComponentLocation());
  }

  public void actionDrop(Component dropTarget, ComponentLocation location) {
    Point where = location.getPoint(dropTarget);
    drop(dropTarget, where.x, where.y);
    waitForIdle();
  }

  public void actionDrop(Component dropTarget, int x, int y) {
    drop(dropTarget, x, y);
    waitForIdle();
  }

  public boolean assertImage(Component component, java.io.File fileImage, boolean ignoreBorder) {
    BufferedImage img = capture(component, ignoreBorder);
    return new ImageComparator().compare(img, fileImage) == 0;
  }

  /**
   * Returns whether a Window corresponding to the given String is showing.  The value may be a
   * plain String or regular expression and may match either the window title (for Frames or
   * Dialogs) or its Component name.
   *
   * @param id id
   * @return true if frame is showing
   * @see junit.extensions.abbot.ComponentTestFixture#isShowing(String)
   * @deprecated This method does not specify the proper context for the lookup.
   */
  @Deprecated
  public boolean assertFrameShowing(String id) {
    try {
      Hierarchy hierarchy = AWTHierarchy.getDefault();
      abbot.finder.ComponentFinder finder = new BasicFinder(hierarchy);
      return finder.find(new WindowMatcher(id, true)) != null;
    } catch (ComponentNotFoundException e) {
      return false;
    } catch (MultipleComponentsFoundException e) {
      // Might not be the one you want, but that's what the docs say
      return true;
    }
  }

  /**
   * Convenience wait for a window to be displayed.  The given value may be a plain String or
   * regular expression and may match either the window title (for Frames and Dialogs) or its
   * Component name.  This method is provided as a convenience for hand-coded tests, since scripts
   * will use a wait step instead.<p> The property abbot.robot.component_delay affects the default
   * timeout.
   *
   * @param identifier id
   * @see junit.extensions.abbot.ComponentTestFixture#isShowing(String)
   * @deprecated This method does not provide sufficient context to reliably find a component.
   */
  @Deprecated
  public void waitForFrameShowing(final String identifier) {
    wait(
        new Condition() {
          public boolean test() {
            return assertFrameShowing(identifier);
          }

          public String toString() {
            return Strings.get("tester.Component.show_wait", new Object[] {identifier});
          }
        },
        componentDelay);
  }

  public boolean assertComponentShowing(ComponentReference ref) {
    try {
      Component component = ref.getComponent();
      return isReadyForInput(component);
    } catch (ComponentSearchException e) {
      return false;
    }
  }

  public void waitForComponentShowing(final ComponentReference ref) {
    wait(
        new Condition() {
          public boolean test() {
            return assertComponentShowing(ref);
          }

          public String toString() {
            return Strings.get("tester.Component.show_wait", new Object[] {ref});
          }
        },
        componentDelay);
  }

  private Method[] cachedMethods = null;

  /**
   * Look up methods with the given prefix.  Facilitates auto-scanning by scripts and the script
   * editor.
   */
  private Method[] getMethods(String prefix, Class<?> returnType, boolean componentArgument) {
    if (cachedMethods == null) {
      cachedMethods = getClass().getMethods();
    }
    ArrayList<Method> methods = new ArrayList<>();
    // Only save one Method for each unique name
    HashSet<String> names = new HashSet<>(IGNORED_METHODS);

    Method[] methodList = cachedMethods;
    for (Method method : methodList) {
      String name = method.getName();
      if (names.contains(name) || !name.startsWith(prefix)) {
        continue;
      }
      Class<?>[] params = method.getParameterTypes();
      if ((returnType == null || returnType.equals(method.getReturnType()))
          && ((params.length == 0 && !componentArgument)
              || (params.length > 0
                  && (Component.class.isAssignableFrom(params[0]) == componentArgument)))) {
        methods.add(method);
        names.add(name);
      }
    }
    return methods.toArray(new Method[0]);
  }

  private Method[] cachedActions = null;

  public Method[] getActions() {
    if (cachedActions == null) {
      cachedActions = getMethods("action", void.class, false);
    }
    return cachedActions;
  }

  private Method[] cachedComponentActions = null;

  public Method[] getComponentActions() {
    if (cachedComponentActions == null) {
      cachedComponentActions = getMethods("action", void.class, true);
    }
    return cachedComponentActions;
  }

  private Method[] cachedPropertyMethods = null;

  public Method[] getPropertyMethods() {
    if (cachedPropertyMethods == null) {
      ArrayList<Method> all = new ArrayList<>();
      all.addAll(Arrays.asList(getMethods("is", boolean.class, true)));
      all.addAll(Arrays.asList(getMethods("has", boolean.class, true)));
      all.addAll(Arrays.asList(getMethods("get", null, true)));
      cachedPropertyMethods = all.toArray(new Method[0]);
    }
    return cachedPropertyMethods;
  }

  private Method[] cachedAssertMethods = null;

  public Method[] getAssertMethods() {
    if (cachedAssertMethods == null) {
      cachedAssertMethods = getMethods("assert", boolean.class, false);
    }
    return cachedAssertMethods;
  }

  private Method[] cachedComponentAssertMethods = null;

  public Method[] getComponentAssertMethods() {
    if (cachedComponentAssertMethods == null) {
      cachedComponentAssertMethods = getMethods("assert", boolean.class, true);
    }
    return cachedComponentAssertMethods;
  }

  public static String stripHTML(String value) {
    if (value != null && (value.startsWith("<html>") || value.startsWith("<HTML>"))) {
      while (value.startsWith("<")) {
        int right = value.indexOf(">");
        if (right == -1) {
          break;
        }
        value = value.substring(right + 1);
      }
      while (value.endsWith(">")) {
        int right = value.lastIndexOf("<");
        if (right == -1) {
          break;
        }
        value = value.substring(0, right);
      }
    }
    return value;
  }

  protected void waitAction(String description, Condition condition) throws ActionFailedException {
    try {
      wait(condition);
    } catch (WaitTimedOutError wto) {
      throw new ActionFailedException(description);
    }
  }

  public Class<?> getTestedClass(Class<?> cls) {
    while (getTester(cls.getSuperclass()) == this) {
      cls = cls.getSuperclass();
    }
    return cls;
  }

  public ComponentLocation parseLocation(String encoded) {
    return new ComponentLocation().parse(encoded);
  }

  public ComponentLocation getLocation(Component component, Point where) {
    return new ComponentLocation(where);
  }

  private static class AWTMenuListener implements AWTEventListener {

    private volatile boolean eventFired;

    @Override
    public void eventDispatched(AWTEvent event) {
      if (event.getID() == ActionEvent.ACTION_PERFORMED) {
        eventFired = true;
      }
    }

    public boolean isNotEventFired() {
      return !eventFired;
    }
  }
}
