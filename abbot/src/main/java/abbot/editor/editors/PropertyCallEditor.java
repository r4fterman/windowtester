package abbot.editor.editors;

import abbot.i18n.Strings;
import abbot.script.ComponentReference;
import abbot.script.PropertyCall;
import abbot.tester.ComponentTester;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;

/**
 * Provide convenient editing of a PropertyCall step.
 */
public abstract class PropertyCallEditor extends CallEditor {

  private final JComboBox component;
  private final PropertyCall call;

  public PropertyCallEditor(PropertyCall call) {
    super(call);
    this.call = call;
    component =
        addComponentSelector(
            Strings.get("ComponentID"), call.getComponentID(), call.getResolver(), true);
    component.setName(TAG_COMPONENT);
  }

  protected Class<?> getComponentTargetClass(Class<?> cls) {
    String[] args = getCall().getArguments();
    if (args.length == 1) {
      String id = args[0];
      ComponentReference ref = getCall().getResolver().getComponentReference(id);
      if (ref != null) {
        try {
          return getCall().resolveClass(ref.getRefClassName());
        } catch (ClassNotFoundException e) {
          // ignore
        }
      }
    }
    return null;
  }

  @Override
  protected Map<String, Method> getMethods(Class<?> cls, int mask) {
    boolean isTester = ComponentTester.class.isAssignableFrom(cls);
    boolean isComponent = Component.class.isAssignableFrom(cls);
    Class<?> componentClass = isTester ? getComponentTargetClass(cls) : (isComponent ? cls : null);
    Class<?> testerClass =
        isTester
            ? cls
            : (isComponent ? ComponentTester.getTester(componentClass).getClass() : null);
    if (!isTester && !isComponent) {
      return super.getMethods(cls, mask);
    }

    Map<String, Method> map = new HashMap<>();
    if (componentClass != null) {
      for (Method m : super.getMethods(componentClass, mask).values()) {
        if (PropertyCall.isPropertyMethod(m)) {
          map.put(m.getName(), m);
        }
      }
    }
    // Scan the corresponding component tester for additional property
    // methods
    try {
      ComponentTester tester =
          componentClass != null
              ? ComponentTester.getTester(componentClass)
              : (ComponentTester) testerClass.getDeclaredConstructor().newInstance();
      for (Method m : getComponentTesterMethods(tester)) {
        map.put(m.getName(), m);
      }
    } catch (Exception e) {
      // ignore
    }
    return map;
  }

  protected boolean includeMethod(Class<?> cls, Method m) {
    return true;
  }

  protected Collection<Method> getComponentTesterMethods(ComponentTester tester) {
    return Arrays.asList(tester.getPropertyMethods());
  }

  protected void componentChanged() {
    component.setSelectedItem(call.getComponentID());
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    Object src = ev.getSource();
    if (src == component) {
      String id = (String) component.getSelectedItem();
      if (id != null) {
        id = id.trim();
      }
      if ("".equals(id)) {
        id = null;
      }
      ComponentReference ref = call.getResolver().getComponentReference(id);
      String tcn = ref != null ? ref.getRefClassName() : Component.class.getName();
      call.setComponentID(id);

      call.setTargetClassName(tcn);
      call.setArguments(new String[0]);
      targetClassChanged();
      argumentsChanged();

      fireStepChanged();
    } else if (src == method) {
      super.actionPerformed(ev);
      // When the method changes to or from a ComponentTester
      // pseudo-property method, we need to change the target class.
      try {
        Class<?> cls = call.getTargetClass();
        String methodName = (String) method.getSelectedItem();
        Map<String, Method> methods = getMethods(cls, Modifier.PUBLIC);
        Method m = methods.get(methodName);
        if (m != null) {
          Class<?> newClass = m.getDeclaringClass();
          if (ComponentTester.class.isAssignableFrom(newClass)
              && Component.class.isAssignableFrom(cls)) {
            String id = call.getComponentID();
            if (id != null) {
              call.setArguments(new String[] {id});
              argumentsChanged();
            }
            call.setComponentID(null);
            call.setTargetClassName(newClass.getName());
            componentChanged();
            targetClassChanged();

            fireStepChanged();
          } else if (Component.class.isAssignableFrom(newClass)
              && ComponentTester.class.isAssignableFrom(cls)
              && call.getArguments().length == 1) {
            newClass = getComponentTargetClass(cls);
            String id = call.getArguments()[0];

            call.setArguments(new String[0]);
            call.setComponentID(id);
            call.setTargetClassName(newClass.getName());
            argumentsChanged();
            componentChanged();
            targetClassChanged();

            fireStepChanged();
          }
        }
      } catch (ClassNotFoundException e) {
        // don't care
      }
    } else {
      super.actionPerformed(ev);
    }
  }
}
