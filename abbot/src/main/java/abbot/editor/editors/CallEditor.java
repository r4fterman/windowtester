package abbot.editor.editors;

import abbot.editor.widgets.ArrayEditor;
import abbot.i18n.Strings;
import abbot.script.Call;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Provide an editor for call steps.
 *
 * @author Blake Christensen bchristen@users.sourceforge.net
 * @author twall@users.sourceforge.net
 */
// TODO: add a help button for the selected method
public class CallEditor extends StepEditor {

  private final Call call;
  protected JTextField target;
  protected JComboBox method;
  protected ArrayEditor arguments;
  private String[] names = new String[0];
  private boolean fieldChanging;

  public CallEditor(Call call) {
    super(call);
    this.call = call;

    target = addTextField(Strings.get("TargetClass"), call.getTargetClassName());
    target.setName(TAG_CLASS);

    method = addComboBox(Strings.get("Method"), call.getMethodName(), getMethodNames());
    method.setName(TAG_METHOD);

    arguments = addArrayEditor(Strings.get("Arguments"), call.getArguments());
    arguments.setName(TAG_ARGS);
  }

  protected Call getCall() {
    return call;
  }

  protected String[] getMethodNames() {
    try {
      Class<?> cls = call.getTargetClass();
      String[] names = getMethodNames(getMethods(cls, Modifier.PUBLIC));
      Arrays.sort(names);
      return names;
    } catch (ClassNotFoundException e) {
      return new String[0];
    }
  }

  protected Class<?> getTargetClass() throws ClassNotFoundException {
    try {
      return call.getTargetClass();
    } catch (NoClassDefFoundError e) {
      throw new ClassNotFoundException(e.getMessage());
    }
  }

  protected Map<String, Method> getMethods(Class<?> cls, int mask) {
    Map<String, Method> processed = new HashMap<>();
    while (cls != null) {
      Method[] methods = cls.getDeclaredMethods();
      for (Method value : methods) {
        if ((value.getModifiers() & mask) == mask) {
          processed.put(value.getName(), value);
        }
      }
      cls = cls.getSuperclass();
    }
    return processed;
  }

  protected String[] getMethodNames(Map<String, Method> methods) {
    return methods.keySet().toArray(new String[0]);
  }

  protected void validateTargetClass() {
    try {
      call.getTargetClass();
      target.setForeground(DEFAULT_FOREGROUND);
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      target.setForeground(ERROR_FOREGROUND);
    }
  }

  protected void validateMethod() {
    try {
      call.getMethod();
      method.setForeground(DEFAULT_FOREGROUND);
    } catch (IllegalArgumentException | NoSuchMethodException | ClassNotFoundException e) {
      method.setForeground(ERROR_FOREGROUND);
    } catch (NoClassDefFoundError e) {
      target.setForeground(ERROR_FOREGROUND);
    }
  }

  /**
   * Sychronize the UI with the Call data.
   */
  private void availableMethodsChanged() {
    fieldChanging = true;
    String[] newNames = getMethodNames();
    boolean changed = newNames.length != names.length;
    for (int i = 0; i < newNames.length && !changed; i++) {
      changed = !newNames[i].equals(names[i]);
    }
    if (changed) {
      method.setModel(new DefaultComboBoxModel(newNames));
      String name = call.getMethodName();
      if (!name.equals(method.getSelectedItem())) {
        method.setSelectedItem(name);
      }
      names = newNames;
    }
    fieldChanging = false;
  }

  protected void targetClassChanged() {
    fieldChanging = true;
    target.setText(call.getTargetClassName());
    fieldChanging = false;
    availableMethodsChanged();
    validateTargetClass();
    validateMethod();
  }

  protected void methodChanged() {
    if (!call.getMethodName().equals(method.getSelectedItem())) {
      method.setSelectedItem(call.getMethodName());
    }
    validateMethod();
  }

  protected void argumentsChanged() {
    arguments.setValues(call.getArguments());
    validateMethod();
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    if (fieldChanging) {
      return;
    }

    Object src = ev.getSource();
    if (src == target) {
      String cname = target.getText().trim();
      if (!cname.equals(call.getTargetClassName())) {
        call.setTargetClassName(cname);
        availableMethodsChanged();
        validateTargetClass();
        validateMethod();
        fireStepChanged();
      }
    } else if (src == method) {
      String name = (String) method.getSelectedItem();
      if (!call.getMethodName().equals(name)) {
        call.setMethodName(name);
        validateMethod();
        fireStepChanged();
      }
    } else if (src == arguments) {
      // FIXME check method signature and do component field if the
      // first arg is a component, do popup from available refs
      // FIXME check arguments against method signature
      Object[] values = arguments.getValues();
      String[] sValues = new String[values.length];
      System.arraycopy(values, 0, sValues, 0, values.length);
      call.setArguments(sValues);
      validateMethod();
      fireStepChanged();
    } else {
      super.actionPerformed(ev);
    }
  }
}
