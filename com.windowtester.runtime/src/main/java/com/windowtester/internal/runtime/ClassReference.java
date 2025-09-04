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
package com.windowtester.internal.runtime;

import com.windowtester.internal.debug.Logger;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A class that stores the class information for a widget in the form of a string.
 */
public class ClassReference implements Serializable {

  public static ClassReference forClass(Class<?> cls) {
    return new ClassReference(cls);
  }

  /**
   * @since 3.8.1
   */
  public static ClassReference forName(String className) {
    return new ClassReference(className);
  }

  /**
   * A class that is used as a sentinel when class resolution in
   * {@link ClassReference#getClassForName()} fails.
   */
  public static final class UnresolvableClass {}

  @Serial private static final long serialVersionUID = -5522094418456665521L;

  /**
   * name of the class
   **/
  private final String name;

  /**
   * the class  -- transient to avoid serialization
   **/
  private transient Class<?> cls;

  /**
   * Constructor
   *
   * @param cls the class
   */
  public ClassReference(Class<?> cls) {
    if (cls == null) {
      throw new IllegalArgumentException("class must not be null");
    }
    this.cls = cls;
    name = cls.getName();
  }

  /**
   * Constructor
   *
   * @param className the class name
   */
  public ClassReference(String className) {
    if (className == null) {
      throw new IllegalArgumentException("class name must not be null");
    }
    name = className;
  }

  /**
   * @return the class corresponding to the name
   */
  public Class<?> getClassForName() {
    if (!isResolved()) {
      cls = resolveClass();
    }
    return cls;
  }

  private Class<?> resolveClass() {
    try {
      return Class.forName(getName());
    } catch (ClassNotFoundException e) {
      Logger.log(e);
      /*
       * I'd like to throw an exception here but there are lots
       * of clients that call this code and don't expect an exception...
       * As a an interim solution, we return a sentinel value that indicates
       * that the class could not be resolved.
       */
      return UnresolvableClass.class;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassReference other) {
      // just test by string names
      return other.name.equals(name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  /**
   * @return the name of the class
   */
  public String getName() {
    return name;
  }

  /**
   * Test whether this class reference refers to the given class.
   * <p>
   * Note: this test <b>will not</b> force the class to be resolved.
   */
  public boolean refersTo(Class<?> cls) {
    if (cls == null) {
      throw new IllegalArgumentException("class must not be null");
    }
    if (isResolved()) {
      return getClassForName().equals(cls);
    }
    return cls.getName().equals(getName());
  }

  private boolean isResolved() {
    return cls != null;
  }

  @Override
  public String toString() {
    return "ClassReference(" + getName() + ")";
  }
}
