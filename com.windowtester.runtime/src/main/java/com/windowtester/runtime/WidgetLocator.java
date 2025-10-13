/*******************************************************************************
 *  Copyright (c) 2012 Google, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Google, Inc. - initial API and implementation
 *******************************************************************************/
package com.windowtester.runtime;

import com.windowtester.internal.runtime.ClassReference;
import com.windowtester.internal.runtime.IWidgetIdentifier;
import com.windowtester.internal.runtime.finder.FinderFactory;
import com.windowtester.runtime.locator.IWidgetLocator;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for Widget locators.  Widget locators capture hierarchy (containment) relationships
 * between widgets for use in widget identification.
 * <p>
 * For example, a widget identified by class and position relative to its parent composite could be
 * described so:
 * <pre>
 * 		new WidgetLocator(Text.class, 2,
 * 			   new WidgetLocator(Group.class, "addressGroup"));
 * </pre>
 * <p>
 * Effectively, this WidgetLocator instance describes the third Text widget in the Group labeled
 * "addressGroup".
 */
public class WidgetLocator implements Serializable, IWidgetIdentifier, IAdaptable, IWidgetLocator {

  /*
   * NOTE: this class is serializable and uses the default serialization scheme.
   * This should _not_ be a problem (hierarchies are not too deep); still, we
   * could consider a custom serialization scheme.
   */
  @Serial private static final long serialVersionUID = 7772528976750829834L;

  /**
   * A sentinel value, indicating an unassigned index
   */
  public static final int UNASSIGNED = -1;

  /**
   * the reference to the target class
   */
  private final ClassReference classRef;

  /**
   * The target widget's name or label
   */
  private final String nameOrLabel;

  /**
   * The target's index relative to its parent
   */
  private int index;

  /**
   * The target's parent info
   */
  private WidgetLocator parentInfo;

  /**
   * The target's ancestor info
   */
  private WidgetLocator ancestorInfo;

  /**
   * A map for associated data key-value pairs
   */
  private Map<String, String> map;

  // required for subclass convenience
  protected WidgetLocator() {
    this(null);
  }

  /**
   * @since 3.8.1
   */
  protected WidgetLocator(
      ClassReference classRef, String nameOrLabel, int index, WidgetLocator parentInfo) {
    this.classRef = classRef;
    this.nameOrLabel = nameOrLabel;
    this.index = index;
    this.parentInfo = parentInfo;
  }

  /**
   * Create an instance.
   *
   * @param cls         - the target class
   * @param nameOrLabel - the target's name or label
   * @param index       - the target's index relative to its parent
   * @param parentInfo  - the target's parent info
   */
  public WidgetLocator(Class<?> cls, String nameOrLabel, int index, WidgetLocator parentInfo) {
    this(ClassReference.forClass(cls), nameOrLabel, index, parentInfo);
  }

  /**
   * Create an instance.
   *
   * @param cls        - the target class
   * @param index      - the target's index relative to its parent
   * @param parentInfo - the target's parent info
   */
  public WidgetLocator(Class<?> cls, int index, WidgetLocator parentInfo) {
    this(cls, null, index, parentInfo);
  }

  /**
   * Create an instance.
   *
   * @param cls         - the target class
   * @param nameOrLabel - the target's name or label
   * @param parentInfo  - the target's parent info
   */
  public WidgetLocator(Class<?> cls, String nameOrLabel, WidgetLocator parentInfo) {
    this(cls, nameOrLabel, UNASSIGNED, parentInfo);
  }

  /**
   * Create an instance.
   *
   * @param cls        - the target class
   * @param parentInfo - the target's parent info
   */
  public WidgetLocator(Class<?> cls, WidgetLocator parentInfo) {
    this(cls, null, UNASSIGNED, parentInfo);
  }

  /**
   * Create an instance.
   *
   * @param cls - the target class
   */
  public WidgetLocator(Class<?> cls) {
    this(cls, (WidgetLocator) null);
  }

  /**
   * Create an instance.
   *
   * @param cls         - the target class
   * @param nameOrLabel - the target's name or label
   */
  public WidgetLocator(Class<?> cls, String nameOrLabel) {
    this(cls, nameOrLabel, null);
  }

  /**
   * Create an instance.
   *
   * @param cls         - the target class
   * @param nameOrLabel - the target's name or label
   * @param index       - the target's index relative to its parent
   */
  public WidgetLocator(Class<?> cls, String nameOrLabel, int index) {
    this(cls, nameOrLabel, index, null);
  }

  /**
   * Create an instance.
   *
   * @param cls   - the target class
   * @param index - the target's index relative to its parent
   */
  public WidgetLocator(Class<?> cls, int index) {
    this(cls, null, index, null);
  }

  /**
   * Accept this visitor.
   */
  public void accept(IWidgetLocatorVisitor visitor) {
    visitor.visit(this);
    if (parentInfo != null) {
      parentInfo.accept(visitor);
    }
  }

  /**
   * Get the <code>WidgetLocator</code> that describes this widget's parent.
   *
   * @return the parent's <code>WidgetLocator</code> object.
   */
  public WidgetLocator getParentInfo() {
    return parentInfo;
  }

  /**
   * Get the <code>WidgetLocator</code> that describes one of the widget's ancestors.
   *
   * @return the ancestor's <code>WidgetLocator</code> object.
   */
  public WidgetLocator getAncestorInfo() {
    return ancestorInfo;
  }

  @Override
  public String getNameOrLabel() {
    return nameOrLabel;
  }

  @Override
  public Class<?> getTargetClass() {
    return classRef.getClassForName();
  }

  /**
   * Get the subject widget's class, as a class reference.
   *
   * @return the subject's class reference
   */
  protected ClassReference getTargetClassRef() {
    return classRef;
  }

  @Override
  public String getTargetClassName() {
    if (classRef != null) {
      return classRef.getName();
    }
    return null;
  }

  /**
   * Set the parent <code>WidgetLocator</code>.
   *
   * @param parentInfo - the new parent <code>WidgetLocator</code>
   */
  public void setParentInfo(WidgetLocator parentInfo) {
    this.parentInfo = parentInfo;
  }

  /**
   * Set the ancestor <code>WidgetLocator</code>.
   *
   * @param ancestorInfo - the new ancestor <code>WidgetLocator</code>
   */
  public void setAncestorInfo(WidgetLocator ancestorInfo) {
    this.ancestorInfo = ancestorInfo;
  }

  /**
   * Get this widget's index relative to its parent widget.
   *
   * @return a 0-based relative index or UNASSIGNED if it is not indexed.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Set this widget's index relative to it's parent.
   *
   * @param index - the index.
   */
  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    var name = getWidgetLocatorStringName();
    if (name != null) {
      builder.append(name).append("(");
    } else {
      builder.append("WidgetLocator(").append(classRef.getName());
      if (getNameOrLabel() != null) {
        builder.append(", ");
      }
    }

    if (getNameOrLabel() != null) {
      builder.append('\"').append(getNameOrLabel()).append('\"');
      if (getIndex() != UNASSIGNED || getParentInfo() != null) {
        builder.append(", ");
      }
    }
    if (index != UNASSIGNED) {
      builder.append(index).append(", ");
    }
    if (parentInfo != null) {
      builder.append(parentInfo);
    }
    builder.append(")");

    return builder.toString();
  }

  /**
   * Override to customize Locator name used in toString(). Default is null.
   */
  protected String getWidgetLocatorStringName() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof WidgetLocator that) {
      return index == that.index
          && Objects.equals(classRef, that.classRef)
          && Objects.equals(nameOrLabel, that.nameOrLabel)
          && Objects.equals(parentInfo, that.parentInfo)
          && Objects.equals(map, that.map);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(classRef, nameOrLabel, index, parentInfo, map);
  }

  /**
   * Sets the programmer defined property key to the specified value.
   * <p>
   * Data mappings allow programmers to associate arbitrary key-value pairs with locator instances.
   * </p>
   *
   * @param key   the name of the property
   * @param value the value of the property or null if it has not been set
   * @throws IllegalArgumentException if the key is null
   */
  public void setData(String key, String value) {
    /*
     * NOTE: keys and values are NOT objects (as in widgets)
     * this is because locators need to be serializable.
     * An alternative is to have the be ISerializables...
     * we could widen the interface if need be.
     */
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    getDataMap().put(key, value);
  }

  /**
   * Returns the programmer defined property of the receiver with the specified name, or null if it
   * has not been set.
   * <p>
   * Data mappings allow programmers to associate arbitrary key-value pairs with locator instances.
   * </p>
   *
   * @param key the name of the property
   * @return the value of the property or null if it has not been set
   * @throws IllegalArgumentException if the key is null
   */
  public String getData(String key) {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    return getDataMap().get(key);
  }

  /**
   * Copies all programmer defined properties of the receiver into the specified locator.
   *
   * @param locator the locator
   */
  public void copyDataTo(WidgetLocator locator) {
    if (map != null && locator != null) {
      map.putAll(locator.getDataMap());
    }
  }

  private Map<String, String> getDataMap() {
    if (map == null) {
      map = new HashMap<>();
    }
    return map;
  }

  @Override
  public Object getAdapter(Class<?> adapter) {
    return null;
  }

  @Override
  public IWidgetLocator[] findAll(IUIContext ui) {
    // get the appropriate finder for this ui context
    return FinderFactory.getFinder(ui).findAll(this);
  }

  @Override
  public boolean matches(Object widget) {
    return false;
  }
}
