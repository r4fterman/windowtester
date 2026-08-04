package abbot.editor.recorder;

import abbot.BugReport;
import abbot.Log;
import abbot.Platform;
import abbot.i18n.Strings;
import abbot.script.Action;
import abbot.script.Assert;
import abbot.script.ComponentReference;
import abbot.script.Resolver;
import abbot.script.Step;
import abbot.tester.AWTConstants;
import abbot.tester.ComponentLocation;
import abbot.tester.ComponentTester;
import abbot.tester.Robot;
import abbot.util.AWT;
import java.awt.AWTEvent;
import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.Container;
import java.awt.Menu;
import java.awt.MenuComponent;
import java.awt.MenuContainer;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolTip;
import javax.swing.JWindow;
import javax.swing.text.JTextComponent;

/**
 * Record basic semantic events you might find on any component. This class handles the following actions:
 * <ul>
 * <li>window actions
 * <li>popup menus
 * <li>click
 * <li>typed keys
 * <li>basic drag and drop
 * <li>InputMethod events (extended character input)
 * </ul>
 * Clicks, popup menus, and drag/drop actions may be based on coordinates or
 * component substructure (cell, row, tab, etc) locations.
 *
 * <h3>Window Actions</h3>
 * While these nominally might be handled in a WindowRecorder, they are so
 * common that it's easier to handle here instead.  Currently supports
 * tracking show/hide/activate.  TODO: move/resize/iconfify/deiconify.
 * <h3>Popup Menus</h3>
 * Currently, only the click/select/click sequence is supported.  The
 * press/drag/release version shouldn't be hard to implement, though.
 * <h3>Click</h3>
 * Simple press/release on a component, storing the exact coordinate of the
 * click.  Most things with selectability will want to override this.  Culling
 * accidental intervening drags would be nice but probably not worth the
 * effort or complexity (better just to be less sloppy with your mouse).
 * <h3>Key Type</h3>
 * Capture only events that result in actual output.  No plain modifiers,
 * shortcuts, or mnemonics.
 * <h3>Drag/Drop</h3>
 * Basic drag from one component and drop on another, storing exact
 * coordinates of the press/release actions.  Should definitely override this
 * to represent your component's internal objects (e.g. cells in a table).
 * Note that these are two distinct actions, even though they always appear
 * together.  The source is responsible for identifying the drag, and the
 * target is responsible for identifying the drop.
 * <h3>InputMethod</h3>
 * Catch extended character input.
 */
// NOTE: Mac OSX robot will actually generate key modifiers prior
// to  button2/3
// NOTE: Mac OSX CTRL/ALT+MB1 invokes MB2
// CTRL+MB1->CTRL+MB2
// ALT+MB1->MB2
// TODO: test recorders by sending an event stream; test platform stream
// by generating robot events and verifying stream seen; this splits the
// tests into separate concerns.
public class ComponentRecorder extends SemanticRecorder {

  private static final String[] TYPES = {
    "any", "window", "menu", "click", "key", "drag", "drop", "text", "input method"
  };

  /**
   * Mappings for special keys.
   */
  private static final Map<String, String> specialMap;

  static {
    // Make explicit some special key mappings which we DON'T want to save
    // as the resulting characters (b/c they may not actually be
    // characters, or they're not particularly good to save as
    // characters.
    int[][] mappings = {
      {'\t', KeyEvent.VK_TAB},
      {'', KeyEvent.VK_ESCAPE}, // No escape sequence exists
      {'\b', KeyEvent.VK_BACK_SPACE},
      {'', KeyEvent.VK_DELETE}, // No escape sequence exists
      {'\n', KeyEvent.VK_ENTER},
      {'\r', KeyEvent.VK_ENTER},
    };
    specialMap = new HashMap<>();
    for (int i = 0; i < mappings.length; i++) {
      specialMap.put(String.valueOf((char) mappings[i][0]), AWT.getKeyCode(mappings[i][1]));
    }
  }

  // For windows
  private Window window = null;
  private boolean isClose = false;
  // For key events
  private char keychar = KeyEvent.CHAR_UNDEFINED;
  private int modifiers;
  // For clicks
  private Component target;
  private Component forwardedTarget;
  private int x, y;
  private boolean released;
  private int clickCount;
  // added button 10/3/06
  private int button;
  // For menu events
  private Component invoker;
  private int menux, menuy;
  private MenuItem awtMenuTarget;
  private Component menuTarget;
  private boolean isPopup;
  private boolean hasAWTPopup;
  private MenuListener menuListener;
  private boolean menuCanceled;
  // For drag events
  // This class is responsible for handling drag/drop once the action has
  // been recognized by a derived class
  private Component dragSource;
  private int dragx, dragy;
  // For drop events
  private Component dropTarget;
  private int dropx, dropy;
  private boolean nativeDrag;
  // InputMethod
  private final List<Integer> imKeyCodes = new ArrayList<>();
  private final StringBuffer imText = new StringBuffer();

  /**
   * Keep a short-term memory of windows we've seen open/close already.
   */
  private static final WeakHashMap<Object, Boolean> closeEventWindows = new WeakHashMap<>();

  private static final WeakHashMap<Object, Boolean> openEventWindows = new WeakHashMap<>();

  /**
   * Create a ComponentRecorder for use in capturing the semantics of a GUI action.
   * @param resolver resolver
   */
  public ComponentRecorder(Resolver resolver) {
    super(resolver);
  }

  /**
   * Does the given event indicate a window was shown?
   * @param event event
   * @return true if open
   */
  protected boolean isOpen(AWTEvent event) {
    int id = event.getID();
    // 1.3 VMs may generate a WINDOW_OPEN without a COMPONENT_SHOWN
    // (see EventRecorderTest.testClickWithDialog)
    // NOTE: COMPONENT_SHOWN precedes WINDOW_OPENED, but we don't really
    // care in this case, since we're just recording the event, not
    // watching for the component's validity.
    return (id == WindowEvent.WINDOW_OPENED && !openEventWindows.containsKey(event.getSource()))
        || id == ComponentEvent.COMPONENT_SHOWN;
  }

  protected boolean isClose(AWTEvent event) {
    int id = event.getID();
    // Window.dispose doesn't generate a HIDDEN event, but it does
    // generate a WINDOW_CLOSED event (1.3/1.4)
    return (id == WindowEvent.WINDOW_CLOSED && !closeEventWindows.containsKey(event.getSource()))
        || id == ComponentEvent.COMPONENT_HIDDEN;
  }

  /**
   * Returns whether this ComponentRecorder wishes to accept the given event.  If the event is accepted, the recorder
   * must invoke init() with the appropriate semantic event type.
   * @param event event
   * @return true if event has been accepted
   */
  @Override
  public boolean accept(AWTEvent event) {
    int rtype = SE_NONE;
    if (isWindowEvent(event)) {
      rtype = SE_WINDOW;
    } else if (isMenuEvent(event)) {
      rtype = SE_MENU;
    } else if (isKeyTyped(event)) {
      rtype = SE_KEY;
    } else if (isClick(event)) {
      rtype = SE_CLICK;
    } else if (isDragDrop(event)) {
      rtype = SE_DROP;
    } else if (isInputMethod(event)) {
      rtype = SE_IM;
    } else {
      if (Log.isClassDebugEnabled(ComponentRecorder.class)) {
        Log.debug("Ignoring " + Robot.toString(event));
      }
    }

    init(rtype);
    boolean accepted = rtype != SE_NONE;
    if (accepted && Log.isClassDebugEnabled(ComponentRecorder.class)) {
      Log.debug("Accepted " + ComponentTester.toString(event));
    }

    return accepted;
  }

  protected boolean isWindowEvent(AWTEvent event) {
    // Ignore activate and deactivate.  They are unreliable.
    // We only want open/close events on non-tooltip and non-popup windows
    return (event.getSource() instanceof Window)
        && !AWT.isHeavyweightPopup((Window) event.getSource())
        && !isToolTip(event.getSource())
        && (isClose(event) || isOpen(event));
  }

  /**
   * Return true if the given event source is a tooltip. Such events look like window events, but we check for them
   * before other kinds of window events so as to be able to filter them out.
   *
   * TODO: emit steps to confirm value of tooltip?
   *
   * @param source the object to examine
   * @return true if this event source is a tooltip
   */
  protected boolean isToolTip(Object source) {
    // Tooltips appear to be a direct subclass of JWindow and
    // have a single component of class JToolTip
    if (source instanceof JWindow) {
      Container pane = ((JWindow) source).getContentPane();
      while (pane.getComponentCount() == 1) {
        Component child = pane.getComponent(0);
        if (child instanceof JToolTip) {
          return true;
        }
        if (!(child instanceof Container)) {
          break;
        }
        pane = (Container) child;
      }
    }
    return false;
  }

  protected boolean isMenuEvent(AWTEvent event) {
    if (event.getID() == ActionEvent.ACTION_PERFORMED
        && event.getSource() instanceof java.awt.MenuItem) {
      return true;
    } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
      MouseEvent me = (MouseEvent) event;
      return me.isPopupTrigger()
          || ((me.getModifiersEx() & AWTConstants.POPUP_MASK) != 0)
          || me.getSource() instanceof JMenu;
    }
    return false;
  }

  protected boolean isKeyTyped(AWTEvent event) {
    return event.getID() == KeyEvent.KEY_TYPED;
  }

  protected boolean isClick(AWTEvent event) {
    if (event.getID() == MouseEvent.MOUSE_PRESSED) {
      MouseEvent me = (MouseEvent) event;
      return (me.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
    }
    return false;
  }

  protected boolean isDragDrop(AWTEvent event) {
    return event.getID() == MouseEvent.MOUSE_DRAGGED;
  }

  // FIXME may be some better detection, like checking for DND interfaces. */
  protected boolean canDrag() {
    return true;
  }

  protected boolean canMultipleClick() {
    return true;
  }

  /**
   * Is this the start of an input method event?
   */
  private boolean isInputMethod(AWTEvent event) {
    // NOTE: HALF_WIDTH signals start of kanji input
    // NOTE: Mac uses input method for some dual-keystroke chars (option-e)
    return (event.getID() == KeyEvent.KEY_RELEASED
            && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_HALF_WIDTH)
        || event.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED;
  }

  protected boolean parseClick(AWTEvent event) {
    boolean consumed = true;
    int id = event.getID();
    if (id == MouseEvent.MOUSE_PRESSED) {
      Log.debug("Parsing mouse down");
      MouseEvent me = (MouseEvent) event;
      if (clickCount == 0) {
        target = me.getComponent();
        x = me.getX();
        y = me.getY();
        modifiers = me.getModifiersEx();
        clickCount = 1;
        button = me.getButton();
        // Add the component immediately, just in case it gets removed
        // from the hierarchy as a result of the click.
        getResolver().addComponent(target);
      } else {
        if (target == me.getComponent()) {
          clickCount = me.getClickCount();
          button = me.getButton();
        } else if (!released) {
          // It's possible to get two consecutive MOUSE_PRESSED
          // events for different targets (e.g. double click on a
          // table cell to get the default editor) (OSX 1.3.1, XP
          // 1.4.1_01). Ignore the second click, since it is
          // artificial, and wait for the original click to finish.
          // i.e. w32 1.3.1
          // MOUSE_PRESSED  JTable
          // MOUSE_PRESSED  JTextField
          // FOCUS_LOST     JTable
          // FOCUS_GAINED   JTextField
          // MOUSE_EXITED   JTable
          // MOUSE_ENTERED  JTextField
          // MOUSE_RELEASED JTable
          // MOUSE_RELEASED JTextField
          forwardedTarget = me.getComponent();
        }
      }
      released = false;
    } else if (id == MouseEvent.MOUSE_RELEASED) {
      Log.debug("Parsing mouse up");
      released = true;
      // Optionally disallow multiple clicks
      if (!canMultipleClick()) {
        setFinished(true);
      }
    } else if (id == MouseEvent.MOUSE_CLICKED) {
      // optionally wait for multiple clicks
      if (!canMultipleClick()) {
        setFinished(true);
      }
    } else if (id == MouseEvent.MOUSE_EXITED) {
      Log.debug("exit event, released=" + released);
      if (event.getSource() != target || released) {
        consumed = false;
        setFinished(true);
      } else if (!released) {
        // May not see any DRAGGED events if it's a native drag;
        // 1.3 posts MOUSE_EXITED after MOUSE_PRESSED, no drag events
        if (clickCount == 1) {
          setRecordingType(SE_DRAG);
          consumed = dragStarted(target, x, y, modifiers, (MouseEvent) event);
        }
      }
    } else if (id == MouseEvent.MOUSE_ENTERED) {
      if (event.getSource() == target && !released) {
        // nothing
      } else if (event.getSource() != forwardedTarget) {
        consumed = false;
        setFinished(true);
      }
    } else if (id == MouseEvent.MOUSE_DRAGGED && canDrag()) {
      Log.debug("Changing click to drag start");
      MouseEvent me = (MouseEvent) event;
      if (Math.abs(me.getX() - x) >= AWTConstants.DRAG_THRESHOLD
          || Math.abs(me.getY() - y) >= AWTConstants.DRAG_THRESHOLD) {
        // Was actually a drag; pass off to drag handler
        setRecordingType(SE_DRAG);
        consumed = dragStarted(target, x, y, modifiers, me);
      } else {
        Log.debug("Drag too small");
      }
    }
    // These events will not prevent a multi-click from being registered.
    else if ((id >= ComponentEvent.COMPONENT_FIRST && id <= ComponentEvent.COMPONENT_LAST)
        || (event instanceof ContainerEvent)
        || (event instanceof FocusEvent)
        || (id == HierarchyEvent.HIERARCHY_CHANGED
            && (((HierarchyEvent) event).getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0)) {
      // Ignore most hierarchy change and component events between
      // clicks.
      // The focus event is sporadic on w32 1.4.1_02
    } else {
      // All other events should cause the click to finish,
      // but don't register a click unless we've received the release
      // event.
      if (released) {
        consumed = false;
        setFinished(true);
      }
    }
    return consumed;
  }

  protected boolean parseWindowEvent(AWTEvent event) {
    final boolean consumed = true;
    isClose = isClose(event);
    // Keep track of window open/close state so we don't parse the same
    // semantic event twice (e.g. COMPONENT_SHOWN + WINDOW_OPENED or
    // multiple WINDOW_CLOSED events).
    if (isClose) {
      closeEventWindows.put(event.getSource(), Boolean.TRUE);
      openEventWindows.remove(event.getSource());
    } else {
      openEventWindows.put(event.getSource(), Boolean.TRUE);
      closeEventWindows.remove(event.getSource());
    }
    Log.log("close=" + isClose + " (" + Robot.toString(event) + ")");
    window = (Window) event.getSource();
    setFinished(true);
    return consumed;
  }

  protected boolean parseKeyEvent(AWTEvent e) {
    int id = e.getID();
    boolean consumed = true;
    if (id == KeyEvent.KEY_TYPED) {
      KeyEvent typed = (KeyEvent) e;
      target = typed.getComponent();
      keychar = typed.getKeyChar();
      modifiers = typed.getModifiersEx();
      if ((modifiers & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK) {
        Log.debug("Waiting for potential focus accelerator on '" + keychar + "'");
      } else {
        // Ignore KEY_TYPED input for control and alt modifiers, since
        // the generated characters are not accepted as text input.
        // Add others if you encounter them, but err on the side of
        // accepting input that can later be removed.
        if ((modifiers & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())
                == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
            || (modifiers & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK) {
          Log.debug("Ignoring modifiers: " + modifiers);
          setRecordingType(SE_NONE);
        }
        setFinished(true);
      }
    } else if (id == FocusEvent.FOCUS_LOST) {
      // Ignore and wait for FOCUS_GAINED
    } else if (id == FocusEvent.FOCUS_GAINED) {
      // Looks like a focus accelerator focus change.  Ignore the
      // KEY_TYPED event.
      Object o = e.getSource();
      char ch = KeyEvent.CHAR_UNDEFINED;
      if (o instanceof JTextComponent) {
        ch = ((JTextComponent) o).getFocusAccelerator();
        Log.debug("focus accelerator is '" + ch + "'");
      }
      if (Character.toUpperCase(ch) == Character.toUpperCase(keychar)) {
        setRecordingType(SE_NONE);
        setFinished(true);
      } else {
        setFinished(true);
        consumed = false;
      }
    } else {
      setRecordingType(SE_NONE);
      setFinished(true);
      consumed = false;
    }
    return consumed;
  }

  protected boolean parseMenuSelection(AWTEvent event) {
    int id = event.getID();
    final boolean consumed = true;
    // press, release, show, [move, show,] press, release
    // press, [drag, show,] release (FIXME not done)
    // ACTION_PERFORMED and ITEM_STATE_CHANGED are only
    // produced by AWT components (wxp/1.4.2)
    if (id == ActionEvent.ACTION_PERFORMED || id == ItemEvent.ITEM_STATE_CHANGED) {
      awtMenuTarget = (MenuItem) event.getSource();
      invoker = AWT.getInvoker(awtMenuTarget);
      // If there is no invoker, the selection came from the MenuBar
      if (invoker != null) {
        isPopup = true;
      }
      Log.debug("AWT menu selection, invoker=" + Robot.toString(invoker));
      if (event instanceof ActionEvent) {
        modifiers = ((ActionEvent) event).getModifiers();
      } else {
        // ItemEvent doesn't report modifiers, so ask use internal
        // tracking to see if any modifiers are active.
        modifiers = Robot.getState().getModifiers();
      }
      setFinished(true);
    } else if (id == MouseEvent.MOUSE_PRESSED) {
      MouseEvent me = (MouseEvent) event;
      // On the first press, we haven't yet set the invoker, which
      // is either a JMenu or the component holding the popup.
      if (invoker == null) {
        invoker = me.getComponent();
        menux = me.getX();
        menuy = me.getY();
        modifiers = me.getModifiersEx();
        isPopup = me.isPopupTrigger();
        // Must add the listener now, b/c on w32 release/click events
        // are not generated until *after* the awt popup selection.
        if (isPopup || (modifiers & AWTConstants.POPUP_MASK) != 0) {
          hasAWTPopup = addMenuListener(invoker);
        }
        // It's possible for a popup menu to be triggered by some
        // other event (e.g. a button click).  Assume that action is
        // already recorded and simply make note of the appropriate
        // menu selection.
        if (invoker instanceof JMenuItem && !(invoker instanceof JMenu)) {
          menuTarget = invoker;
          invoker = null;
          menux = menuy = -1;
          modifiers = 0;
          isPopup = true;
        }
      } else if (event.getSource() instanceof JMenu) {
        // ignore
      } else if (event.getSource() instanceof JMenuItem) {
        // Click to select the menu item; this will be the second
        // press event received
        menuTarget = (Component) event.getSource();
      } else {
        // Mouse press in something other than the menu, assume it was
        // canceled.
        // Popup was canceled.  Discard subsequent release/click.
        menuCanceled = true;
        setStatus("Popup menu selection canceled");
      }
      Log.log("Menu mouse press");
    } else if (id == MouseEvent.MOUSE_RELEASED) {
      MouseEvent me = (MouseEvent) event;
      // The menu target won't be set until the second mouse press
      if (menuCanceled) {
        setRecordingType(SE_NONE);
        setFinished(true);
      } else if (menuTarget == null) {
        // This is the first mouse release
        if (!isPopup) {
          isPopup = me.isPopupTrigger();
        }
      } else {
        setFinished(true);
      }
      Log.log("Menu mouse release");
    } else if (id == MouseEvent.MOUSE_CLICKED && isPopup) {
      // If it was a popup trigger, make sure there was a popup,
      // otherwise record it as a click.
      // Note that we won't likely get any events with an AWT popup, so
      // just assume it was invoked if there is one.
      if (!hasAWTPopup && AWT.findActivePopupMenu() == null) {
        setRecordingType(SE_CLICK);
        target = invoker;
        x = menux;
        y = menuy;
        setFinished(true);
      }
    } else {
      Log.debug("Ignoring " + ComponentTester.toString(event));
    }
    return consumed;
  }

  // TODO: set up test to generate several different drag types, then sample
  // the event streams on different platforms:
  // drag from one component to another
  // drag within a component
  protected boolean parseDrop(AWTEvent event) {
    int id = event.getID();
    final boolean consumed = true;

    // Use enter/exit events to determine what the final destination
    // is, since drag events always use the drag source for the component.
    if (id == MouseEvent.MOUSE_DRAGGED) {
      // If we don't have a target yet, default to the drag source
      MouseEvent me = (MouseEvent) event;
      if (dropTarget == null) {
        Log.debug("No target yet, using drag source as target");
        dropTarget = me.getComponent();
        dropx = me.getX();
        dropy = me.getY();
      }
      // origin is always the drag source
      Point p = Robot.getState().getDragOrigin();
      if (Math.abs(p.x - me.getX()) > AWTConstants.DRAG_THRESHOLD
          || Math.abs(p.y - me.getY()) > AWTConstants.DRAG_THRESHOLD) {
        setNativeDrag(false);
        Log.debug("Not a native drag");
      }
    } else if (id == MouseEvent.MOUSE_MOVED) {
      // This seems to be the canonical exit from a drag/drop
      // Observed on: w32 1.4.2 JTree, JTable, JLabel drops
      // I'd much rather see a MOUSE_RELEASED when the drag completes.
      dropTarget = ((MouseEvent) event).getComponent();
      dropx = ((MouseEvent) event).getX();
      dropy = ((MouseEvent) event).getY();
      setFinished(true);
    } else if (id == MouseEvent.MOUSE_ENTERED) {
      Log.debug("Drag enter");
      dropTarget = ((MouseEvent) event).getComponent();
      dropx = ((MouseEvent) event).getX();
      dropy = ((MouseEvent) event).getY();
      Log.debug("Not a native drag");
      setNativeDrag(false);
    } else if (id == MouseEvent.MOUSE_EXITED) {
      // If a true drag is in effect (as of 1.4+), we will see no java
      // event queue events until this one (on w32, anyway).
      // If it's 1.3, we won't get any events, even after the drag
      // completes.
      MouseEvent me = (MouseEvent) event;
      if (nativeDrag && Platform.JAVA_VERSION >= Platform.JAVA_1_4) {
        Log.debug("Inferring drop");
        dropTarget = me.getComponent();
        dropx = me.getX();
        dropy = me.getY();
        setFinished(true);
      }
    } else if (id == MouseEvent.MOUSE_RELEASED) {
      MouseEvent me = (MouseEvent) event;
      Log.debug("Dropped");
      // FIXME verify that the component is always the original press
      // source
      dropTarget = me.getComponent();
      dropx = me.getX();
      dropy = me.getY();
      setFinished(true);
    } else {
      if (Log.isClassDebugEnabled(ComponentRecorder.class)) {
        Log.debug("Ignoring " + ComponentTester.toString(event));
      }
    }
    return consumed;
  }

  protected boolean parseInputMethod(AWTEvent event) {
    boolean consumed = true;
    int id = event.getID();
    if (id == KeyEvent.KEY_RELEASED) {
      KeyEvent ke = (KeyEvent) event;
      int code = ke.getKeyCode();
      switch (code) {
        case KeyEvent.VK_HALF_WIDTH:
          // This indicates the input method start (for kanji, anyway)
          break;
        case KeyEvent.VK_FULL_WIDTH:
          // This indicates the input method end (for kanji, anyway)
          Log.log("Captured " + imText);
          setFinished(true);
          break;
        case KeyEvent.VK_ALT_GRAPH:
        case KeyEvent.VK_CONTROL:
        case KeyEvent.VK_SHIFT:
        case KeyEvent.VK_META:
        case KeyEvent.VK_ALT:
          Log.debug("Modifier indicates end of InputMethod");
          consumed = false;
          setFinished(true);
          break;
        default:
          // Consume other key release events, assuming there was no
          // corresponding key press event.
          imKeyCodes.add(code);
          break;
      }
    } else if (event instanceof InputMethodEvent ime) {
      if (id == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) {
        if (ime.getCommittedCharacterCount() > 0) {
          AttributedCharacterIterator iter = ime.getText();
          StringBuilder sb = new StringBuilder();
          for (char ch = iter.first(); ch != CharacterIterator.DONE; ch = iter.next()) {
            sb.append(ch);
          }
          imText.append(sb);
          Log.debug("Partial capture " + sb);
        }
        if (!Platform.isOSX()) {
          setFinished(true);
        }
      }
    } else {
      consumed = false;
      setFinished(true);
    }
    return consumed;
  }

  @Override
  public boolean parse(AWTEvent event) {
    if (Log.isClassDebugEnabled(ComponentRecorder.class)) {
      Log.debug("Parsing " + ComponentTester.toString(event) + " as " + TYPES[getRecordingType()]);
    }

    // Default handling is an event consumed and assume not finished
    boolean consumed = true;

    switch (getRecordingType()) {
      case SE_WINDOW:
        consumed = parseWindowEvent(event);
        break;
      case SE_KEY:
        consumed = parseKeyEvent(event);
        break;
      case SE_CLICK:
        consumed = parseClick(event);
        break;
      case SE_MENU:
        consumed = parseMenuSelection(event);
        break;
      case SE_DROP:
        consumed = parseDrop(event);
        break;
      case SE_IM:
        consumed = parseInputMethod(event);
        break;
      default:
        Log.warn("Unknown input type: " + getRecordingType());
        // error
        break;
    }
    if (isFinished()) {
      try {
        Step step = createStep();
        setStep(step);
        Log.log("Semantic event recorded: " + step);
      } catch (Throwable thr) {
        final String msg = "recording.error";
        BugReport br = new BugReport(msg, thr);
        Log.log("Semantic recorder error: " + br);
        setStatus(Strings.get("editor.see_console"));
        setRecordingType(SE_NONE);
        throw br;
      }
    }

    return consumed;
  }

  protected void setNativeDrag(boolean n) {
    nativeDrag = n;
  }

  protected boolean dragStarted(
      Component target, int x, int y, int modifiers, MouseEvent dragEvent) {
    dragSource = target;
    dragx = x;
    dragy = y;
    setFinished(true);
    return false;
  }

  @Override
  protected Step createStep() {
    Step step = null;
    int type = getRecordingType();
    Log.debug(
        "Creating step for semantic recorder, type: "
            + (type >= 0 && type < TYPES.length
                ? TYPES[getRecordingType()]
                : String.valueOf(type)));
    switch (type) {
      case SE_WINDOW:
        step = createWindowEvent(window, isClose);
        break;
      case SE_MENU:
        if (awtMenuTarget != null) {
          if (invoker == null) {
            MenuContainer mc = awtMenuTarget.getParent();
            while (mc instanceof MenuComponent) {
              mc = ((MenuComponent) mc).getParent();
            }
            if (mc == null) {
              throw new Error("AWT MenuItem " + awtMenuTarget + " has no Component ancestor");
            }
            invoker = (Component) mc;
          }
          step = createAWTMenuSelection(invoker, awtMenuTarget, isPopup);
        } else if (isPopup) {
          step = createPopupMenuSelection(invoker, menux, menuy, menuTarget);
        } else if (menuTarget != null) {
          step = createMenuSelection(menuTarget);
        }
        break;
      case SE_KEY:
        {
          if (keychar != KeyEvent.CHAR_UNDEFINED) {
            step = createKey(target, keychar, modifiers);
          } else {
            step = null;
          }
          break;
        }
      case SE_CLICK:
        {
          step = createClick(target, x, y, modifiers, canMultipleClick() ? clickCount : 1);
          break;
        }
      case SE_DRAG:
        {
          step = createDrag(dragSource, dragx, dragy);
          break;
        }
      case SE_DROP:
        step = createDrop(dropTarget, dropx, dropy);
        break;
      case SE_IM:
        if (!imText.isEmpty()) {
          step = createInputMethod(imKeyCodes, imText.toString());
        } else {
          Log.debug("Input method resulted in no text");
          step = null;
        }
        break;
      default:
        step = null;
        break;
    }

    return step;
  }

  protected Step createWindowEvent(Window window, boolean isClose) {
    ComponentReference ref = getResolver().addComponent(window);
    final String method = "assertComponentShowing";
    Assert step =
        new Assert(
            getResolver(),
            null,
            ComponentTester.class.getName(),
            method,
            new String[] {ref.getID()},
            "true",
            isClose);
    step.setWait(true);
    return step;
  }

  protected Step createMenuSelection(Component menuItem) {
    ComponentReference cr = getResolver().addComponent(menuItem);
    return new Action(getResolver(), null, "actionSelectMenuItem", new String[] {cr.getID()});
  }

  protected Step createAWTMenuSelection(Component parent, MenuItem menuItem, boolean isPopup) {
    ComponentReference ref = getResolver().addComponent(parent);
    String method = "actionSelectAWTMenuItem";
    if (isPopup) {
      method = "actionSelectAWTPopupMenuItem";
    }
    // Get a unique path for the MenuItem
    String path = AWT.getPath(menuItem);
    // Do a quick search on the invoker for other popups.  If there are
    // duplicates, include the menu item name
    return new Action(getResolver(), null, method, new String[] {ref.getID(), path});
  }

  protected Step createPopupMenuSelection(Component invoker, int x, int y, Component menuItem) {
    Step step;
    if (invoker != null) {
      ComponentReference inv = getResolver().addComponent(invoker);
      JMenuItem mi = (JMenuItem) menuItem;
      String where = getLocationArgument(invoker, x, y);
      step =
          new Action(
              getResolver(),
              null,
              "actionSelectPopupMenuItem",
              new String[] {inv.getID(), where, mi.getText()},
              invoker.getClass());
    } else {
      ComponentReference ref = getResolver().addComponent(menuItem);
      step = new Action(getResolver(), null, "actionSelectMenuItem", new String[] {ref.getID()});
    }
    return step;
  }

  protected Step createKey(Component comp, char keychar, int mods) {
    ComponentReference cr = getResolver().addComponent(comp);
    // NOTE: Any keys which might have effects as key press/release should
    // be encoded as a keystroke, rather than a keystring.
    // NOTE: We encode strings rather than integer values, since the
    // names are more useful.
    String code = (String) specialMap.get(String.valueOf(keychar));
    if (code != null) {
      String[] args =
          mods != 0
              ? new String[] {cr.getID(), code, AWT.getKeyModifiers(mods)}
              : new String[] {cr.getID(), code};
      return new Action(getResolver(), null, "actionKeyStroke", args);
    }
    return new Action(
        getResolver(), null, "actionKeyString", new String[] {cr.getID(), String.valueOf(keychar)});
  }

  protected Step createDrop(Component comp, int x, int y) {
    Step step = null;
    if (comp != null) {
      ComponentReference cr = getResolver().addComponent(comp);
      String where = getLocationArgument(comp, x, y);
      step =
          new Action(
              getResolver(), null, "actionDrop", new String[] {cr.getID(), where}, comp.getClass());
    }
    return step;
  }

  protected Step createDrag(Component comp, int x, int y) {
    ComponentReference ref = getResolver().addComponent(comp);
    String where = getLocationArgument(comp, x, y);
    return new Action(
        getResolver(),
        null,
        "actionDrag",
        new String[] {
          ref.getID(), where,
        },
        comp.getClass());
  }

  protected Step createClick(Component target, int x, int y, int mods, int count) {
    Log.debug("creating click");
    ComponentReference cr = getResolver().addComponent(target);
    List<String> args = new ArrayList<>();
    args.add(cr.getID());
    args.add(getLocationArgument(target, x, y));
    if ((mods != 0 && mods != MouseEvent.BUTTON1_DOWN_MASK) || count > 1) {
      // NOTE: this currently saves POPUP or TERTIARY, rather than
      // an explicit button 2 or 3.  I figure that makes more sense
      // than a hard coded button number.
      args.add(AWT.getMouseModifiers(mods));
      if (count > 1) {
        args.add(String.valueOf(count));
      }
    }
    return new Action(
        getResolver(), null, "actionClick", args.toArray(new String[0]), target.getClass());
  }

  protected Step createInputMethod(List<Integer> codes, String text) {
    Log.debug("Text length is " + text.length());
    return new Action(getResolver(), null, "actionKeyString", new String[] {text});
  }

  @Override
  protected void init(int recordingType) {
    super.init(recordingType);
    target = null;
    forwardedTarget = null;
    released = false;
    clickCount = 0;
    keychar = KeyEvent.CHAR_UNDEFINED;
    invoker = null;
    awtMenuTarget = null;
    isPopup = false;
    hasAWTPopup = false;
    menuListener = null;
    menuTarget = null;
    menuCanceled = false;
    dragSource = dropTarget = null;
    nativeDrag = true;
    window = null;
    isClose = false;
    imKeyCodes.clear();
    imText.delete(0, imText.length());
  }

  @Override
  protected void setFinished(boolean state) {
    MenuListener listener;
    synchronized (this) {
      super.setFinished(state);
      listener = menuListener;
      menuListener = null;
    }
    if (listener != null) {
      listener.dispose();
    }
  }

  private boolean addMenuListener(Component invoker) {
    PopupMenu[] popups = AWT.getPopupMenus(invoker);
    if (popups.length > 0) {
      menuListener = new MenuListener(popups);
      return true;
    }
    return false;
  }

  private class MenuListener implements ItemListener {
    private final List<MenuItem> items = new ArrayList<>();

    public MenuListener(PopupMenu[] popups) {
      for (PopupMenu popup : popups) {
        addRecursive(popup);
      }
    }

    private void addRecursive(Menu menu) {
      for (int i = 0; i < menu.getItemCount(); i++) {
        MenuItem item = menu.getItem(i);
        if (item instanceof Menu) {
          addRecursive((Menu) item);
        } else if (item instanceof CheckboxMenuItem) {
          ((CheckboxMenuItem) item).addItemListener(this);
          items.add(item);
        }
      }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      dispose();
      parse(e);
    }

    public void dispose() {
      while (!items.isEmpty()) {
        ((CheckboxMenuItem) items.getFirst()).removeItemListener(this);
        items.removeFirst();
      }
    }
  }

  protected String getLocationArgument(Component c, int x, int y) {
    return getLocation(c, x, y).toString();
  }

  protected ComponentLocation getLocation(Component c, int x, int y) {
    ComponentTester tester = ComponentTester.getTester(c);
    return tester.getLocation(c, new Point(x, y));
  }

  public Component getTarget() {
    return target;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getClickCount() {
    return clickCount;
  }

  public int getModifiers() {
    return modifiers;
  }

  public int getButton() {
    return button;
  }
}
