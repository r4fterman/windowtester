package abbot.script;

import abbot.Log;
import abbot.finder.ComponentNotFoundException;
import abbot.finder.ComponentSearchException;
import abbot.finder.MultipleComponentsFoundException;
import abbot.tester.ComponentTester;
import abbot.util.AWT;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Script step to generate a single AWT event to a component.   Currently used for key down/up and mouse motion.
 */
// TODO: Save mouse motion/enter/leave as relative to containing window/frame,
// in case frame moves
public class Event extends Step {
  private static final String USAGE = "<event type=\"...\" kind=\"...\" [...]/>";

  private String componentID = null;
  private String type = null;
  private String kind = null;
  private final Map<String, String> eventAttributes = new HashMap<>();

  public Event(Resolver resolver, Map<String, String> attributes) {
    super(resolver, attributes);
    componentID = attributes.get(TAG_COMPONENT);
    // can't create events without a component, so creation of the event
    // is deferred.  we do check for validity, though.
    parseEvent(attributes);
  }

  public Event(Resolver resolver, String desc, AWTEvent event) {
    super(resolver, desc);
    int id = event.getID();
    type = simpleClassName(event.getClass());
    kind = ComponentTester.getEventID(event);
    Component comp = ((ComponentEvent) event).getComponent();
    if (event instanceof MouseEvent me) {
      ComponentReference ref = resolver.addComponent(comp);
      componentID = ref.getID();
      eventAttributes.put(TAG_X, String.valueOf(me.getX()));
      eventAttributes.put(TAG_Y, String.valueOf(me.getY()));
      // Convert enter/exit to mouse moved
      if (id == MouseEvent.MOUSE_ENTERED
          || id == MouseEvent.MOUSE_EXITED
          || id == MouseEvent.MOUSE_DRAGGED) {
        kind = "MOUSE_MOVED";
      }
      // No need to include modifiers in a captured event; it is assumed
      // that the modifiers are captured separately
      if (me.isPopupTrigger()) {
        eventAttributes.put(TAG_TRIGGER, "true");
      }
    } else if (event instanceof KeyEvent ke) {
      ComponentReference ref = resolver.addComponent(comp);
      componentID = ref.getID();
      if (ke.getModifiersEx() != 0) {
        eventAttributes.put(TAG_MODIFIERS, AWT.getModifiers(ke));
      }
      if (id == KeyEvent.KEY_TYPED) {
        // Must encode keychars (e.g. '<')
        eventAttributes.put(TAG_KEYCHAR, String.valueOf(ke.getKeyChar()));
      } else {
        eventAttributes.put(TAG_KEYCODE, AWT.getKeyCode(ke.getKeyCode()));
      }
    } else {
      throw new IllegalArgumentException("Unimplemented event type " + event);
    }
  }

  @Override
  public String getDefaultDescription() {
    String desc = type + "." + kind;
    if (type.equals("KeyEvent")) {
      desc += " (" + eventAttributes.get(TAG_KEYCODE) + ")";
    }
    if (componentID != null) {
      desc += " on ${" + componentID + "}";
    }
    return desc;
  }

  @Override
  public String getXMLTag() {
    return TAG_EVENT;
  }

  @Override
  public String getUsage() {
    return USAGE;
  }

  @Override
  public Map<String, String> getAttributes() {
    Map<String, String> map = super.getAttributes();
    map.put(TAG_COMPONENT, componentID);
    map.put(TAG_TYPE, type);
    if (kind != null) {
      map.put(TAG_KIND, kind);
    }
    map.putAll(eventAttributes);
    return map;
  }

  @Override
  public void runStep() throws Throwable {
    ComponentTester.getTester(java.awt.Component.class)
        .sendEvent(createEvent(System.currentTimeMillis()));
  }

  /**
   * Validate the attributes are sufficient to construct an event.
   */
  private void parseEvent(Map<String, String> map) {
    type = map.get(TAG_TYPE);
    componentID = map.get(TAG_COMPONENT);
    kind = map.get(TAG_KIND);
    if (type == null) {
      usage("AWT event type missing");
    }
    if (type.endsWith("MouseEvent")) {
      String modifiers = map.get(TAG_MODIFIERS);
      String x = map.get(TAG_X);
      String y = map.get(TAG_Y);
      String count = map.get(TAG_COUNT);
      String trigger = map.get(TAG_TRIGGER);
      if (kind == null) {
        usage("MouseEvent must specify a kind");
      }
      if (modifiers != null) {
        eventAttributes.put(TAG_MODIFIERS, modifiers);
      }
      if (x != null) {
        eventAttributes.put(TAG_X, x);
      }
      if (y != null) {
        eventAttributes.put(TAG_Y, y);
      }
      if (count != null) {
        eventAttributes.put(TAG_COUNT, count);
      }
      if (trigger != null) {
        eventAttributes.put(TAG_TRIGGER, trigger);
      }
      if (type.equals("MenuDragMouseEvent")) {
        // FIXME
      }
    } else if (type.equals("KeyEvent")) {
      if (kind == null) {
        usage("KeyEvent must specify a kind");
      }
      String keyCode = map.get(TAG_KEYCODE);
      String modifiers = map.get(TAG_MODIFIERS);
      // Saved characters might be XML-encoded
      String keyChar = map.get(TAG_KEYCHAR);
      if (keyCode == null) {
        if (!kind.equals("KEY_TYPED")) {
          usage("KeyPress/Release require a keyCode");
        }
      } else if (!kind.equals("KEY_TYPED")) {
        eventAttributes.put(TAG_KEYCODE, keyCode);
      }
      if (keyChar == null) {
        if (kind.equals("KEY_TYPED")) {
          usage("KeyTyped requires a keyChar");
        }
      } else if (kind.equals("KEY_TYPED")) {
        eventAttributes.put(TAG_KEYCHAR, keyChar);
      }
      if (modifiers != null && !modifiers.isEmpty()) {
        eventAttributes.put(TAG_MODIFIERS, modifiers);
      }
    }
    // FIXME what others are important? window events?
    else {
      Log.warn("Unimplemented event type '" + type + "', placeholder");
      // usage("Unimplemented event type '" + type + "'");
    }
  }

  protected java.awt.Component resolve(String name)
      throws NoSuchReferenceException,
          ComponentNotFoundException,
          MultipleComponentsFoundException {
    ComponentReference ref = getResolver().getComponentReference(name);
    if (ref != null) {
      return ref.getComponent();
    }
    throw new NoSuchReferenceException(name);
  }

  /**
   * Create an event based on the parameters we've collected
   */
  private AWTEvent createEvent(long timestamp)
      throws ComponentSearchException, NoSuchReferenceException {
    Component comp = null;
    if (componentID != null) {
      comp = resolve(componentID);
    }
    if (type.endsWith("MouseEvent")) {
      int x = (comp.getSize().width + 1) / 2;
      int y = (comp.getSize().height + 1) / 2;
      int count = 1;
      boolean trigger = false;
      String modifiers = eventAttributes.get(TAG_MODIFIERS);
      int mods = modifiers != null ? AWT.getModifiers(modifiers) : 0;
      try {
        x = Integer.parseInt(eventAttributes.get(TAG_X));
      } catch (Exception exc) {
        // ignore
      }
      try {
        y = Integer.parseInt(eventAttributes.get(TAG_Y));
      } catch (Exception exc) {
        // ignore
      }
      try {
        count = Integer.parseInt(eventAttributes.get(TAG_COUNT));
      } catch (Exception exc) {
        // ignore
      }
      try {
        trigger = Boolean.getBoolean(eventAttributes.get(TAG_TRIGGER));
      } catch (Exception exc) {
        // ignore
      }
      int id = ComponentTester.getEventID(MouseEvent.class, kind);
      return new MouseEvent(comp, id, timestamp, mods, x, y, count, trigger);
    } else if (type.equals("KeyEvent")) {
      String modifiers = eventAttributes.get(TAG_MODIFIERS);
      int mods = modifiers != null ? AWT.getModifiers(modifiers) : 0;
      int code = AWT.getKeyCode(eventAttributes.get(TAG_KEYCODE));
      String ch = eventAttributes.get(TAG_KEYCHAR);
      char keyChar = ch != null ? ch.charAt(0) : (char) code;
      int id = ComponentTester.getEventID(KeyEvent.class, kind);
      return new KeyEvent(comp, id, timestamp, mods, code, keyChar);
    }
    throw new IllegalArgumentException("Bad event type " + type);
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getComponentID() {
    return componentID;
  }

  public void setComponentID(String id) {
    componentID = id;
  }

  public String getAttribute(String tag) {
    return eventAttributes.get(tag);
  }

  public void setAttribute(String tag, String value) {
    eventAttributes.put(tag, value);
  }
}
