package abbot.script;

import abbot.tester.ComponentLocation;
import abbot.tester.ComponentTester;
import abbot.util.AWT;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulate an action. Usage:<br>
 * <pre>
 * &lt;action method="..." args="..."&gt;<br> &lt;action method="..." args="component_id[,...]" class="..."&gt;<br>
 * </pre>
 * An Action reproduces a user semantic action (such as a mouse click, menu selection, or drag/drop action) on a
 * particular component.  The id of the component being operated on must be the first argument, and the class of that
 * component must be identified by the class tag if the action is not provided by the base {@link
 * abbot.tester.ComponentTester} class Note that the method name is the name of the actionXXX method, e.g. to click a
 * button (actionClick on AbstractButtonTester), the XML would appear thus:
 * <pre>
 * &lt;action method="actionClick" args="My Button" class=javax.swing.AbstractButton&gt;<br>
 * </pre>
 * Note that if the first argument is a Component, the class tag is required. Note also that the specified class is the
 * <i>tested</i> class, not the target class for the method invocation.
 * The target class for the method invocation is always a ComponentTester-derived class.
 */
// Any reason for the tested class to be saved and not the target class?  the
// tester class gets looked up dynamically, but is there any reason that would
// be required?
// The component reference class should be used; that way the component can
// change class w/o affecting the action; also the action class should *not*
// be saved, and the component tester looked up dynamically.
public class Action extends Call {

  private static final String USAGE = "<action method=\"...\" args=\"...\" [class=\"...\"]/>";

  // Account for deprecated methods which use String representations of
  // modifier masks.
  private static final Set<String> stringModifierMethods =
      new HashSet<>(Arrays.asList("actionKeyStroke", "actionKeyPress", "actionKeyRelease"));

  private static final Set<String> optionalFocusMethods =
      new HashSet<>(
          Arrays.asList(
              "actionKeyStroke", "actionKeyPress", "actionKeyRelease", "actionKeyString"));

  private static final String DEFAULT_CLASS_NAME = "java.awt.Component";

  /**
   * Provide a default value for the target class name, so that the Call parent class won't choke.
   */
  private static Map<String, String> patchAttributes(Map<String, String> map) {
    map.putIfAbsent(TAG_CLASS, DEFAULT_CLASS_NAME);
    return map;
  }

  public Action(Resolver resolver, Map<String, String> attributes) {
    super(resolver, patchAttributes(attributes));
    patchMethodName();
  }

  public Action(Resolver resolver, String description, String methodName, String[] args) {
    super(resolver, description, DEFAULT_CLASS_NAME, methodName, args);
    patchMethodName();
  }

  public Action(
      Resolver resolver,
      String description,
      String methodName,
      String[] args,
      Class<?> targetClass) {
    super(resolver, description, targetClass.getName(), methodName, args);
    patchMethodName();
  }

  private void patchMethodName() {
    // account for deprecated usage
    String mn = getMethodName();
    if (!mn.startsWith("action")) {
      setMethodName("action" + mn);
    }
  }

  @Override
  public void setTargetClassName(String cn) {
    if (cn == null || cn.isEmpty()) {
      cn = DEFAULT_CLASS_NAME;
    }
    super.setTargetClassName(cn);
  }

  @Override
  public String getXMLTag() {
    return TAG_ACTION;
  }

  @Override
  public Map<String, String> getAttributes() {
    Map<String, String> map = super.getAttributes();
    // Only save the class attribute if it's not the default
    map.remove(TAG_CLASS);
    if (!DEFAULT_CLASS_NAME.equals(getTargetClassName())) {
      map.put(TAG_CLASS, getTargetClassName());
    }
    return map;
  }

  @Override
  public String getUsage() {
    return USAGE;
  }

  @Override
  public String getDefaultDescription() {
    // strip off "action", if it's there
    String name = getMethodName();
    if (name.startsWith("action")) {
      name = name.substring(6);
    }
    return name + getArgumentsDescription();
  }

  @Override
  public Class<?> getTargetClass() throws ClassNotFoundException {
    return resolveTester(getTargetClassName()).getClass();
  }

  @Override
  protected Object evaluateParameter(Method m, String param, Class<?> type) throws Exception {
    // Convert ComponentLocation arguments
    if (ComponentLocation.class.isAssignableFrom(type)) {
      ComponentTester tester = (ComponentTester) getTarget(m);
      return tester.parseLocation(param);
    }
    // Convert virtual key codes and modifier masks into integers
    else if ((type == int.class || type == Integer.class)
        && (param.startsWith("VK_") || param.contains("_MASK"))) {
      if (param.startsWith("VK_")) {
        return AWT.getKeyCode(param);
      }
      return AWT.getModifiers(param);
    } else {
      return super.evaluateParameter(m, param, type);
    }
  }

  @Override
  protected Object getTarget(Method m) throws ClassNotFoundException {
    return resolveTester(getTargetClassName());
  }

  @Override
  protected Method[] resolveMethods(String name, Class<?> cls, Class<?> returnType)
      throws NoSuchMethodException {
    Method[] methods = super.resolveMethods(name, cls, returnType);
    if (stringModifierMethods.contains(name)) {
      // still have some key methods hanging around which expect a
      // string representation of the VK_ code and/or modifiers.
      // ignore them here.
      List<Method> list = new ArrayList<>(Arrays.asList(methods));
      for (Method method : methods) {
        Class<?>[] ptypes = method.getParameterTypes();
        for (Class<?> ptype : ptypes) {
          if (ptype == String.class) {
            list.remove(method);
            break;
          }
        }
      }
      methods = list.toArray(new Method[0]);
    }
    return methods;
  }

  @Override
  public Method getMethod() throws ClassNotFoundException, NoSuchMethodException {
    return resolveMethod(getMethodName(), getTargetClass(), void.class);
  }

  @Override
  protected Method disambiguateMethod(Method[] methods) {
    // Try to find the right one by examining some of the parameters
    // Nothing fancy, just explicitly picks between the variants.
    if (methods.length == 2) {
      // pick between key action variants
      if (optionalFocusMethods.contains(methods[0].getName())) {
        Class<?>[] params = methods[0].getParameterTypes();
        Method kcMethod, crefMethod;
        if (params[0] == int.class) {
          kcMethod = methods[0];
          crefMethod = methods[1];
        } else {
          kcMethod = methods[1];
          crefMethod = methods[0];
        }
        String[] args = getArguments();
        if (args.length > 0 && args[0].startsWith("VK_")) {
          return kcMethod;
        } else {
          return crefMethod;
        }
      }
    }
    return super.disambiguateMethod(methods);
  }
}
