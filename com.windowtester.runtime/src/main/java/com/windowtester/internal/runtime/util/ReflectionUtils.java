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
package com.windowtester.internal.runtime.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Simple reflection utilities.
 */
public class ReflectionUtils {

  @SuppressWarnings("unchecked")
  public static <T> T newInstance(T obj)
      throws IllegalArgumentException,
          SecurityException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException,
          NoSuchMethodException {
    var newObj = obj.getClass().getConstructor().newInstance();
    return (T) newObj;
  }

  @SuppressWarnings("unchecked")
  public static <T> T newInstance(T obj, Class<?> argType, Object arg)
      throws IllegalArgumentException,
          SecurityException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException,
          NoSuchMethodException {
    var newObj = obj.getClass().getConstructor(argType).newInstance(arg);
    return (T) newObj;
  }

  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<?> instanceClass, Class<?> argType, Object arg)
      throws IllegalArgumentException,
          SecurityException,
          InstantiationException,
          IllegalAccessException,
          InvocationTargetException,
          NoSuchMethodException {
    var newObj = instanceClass.getConstructor(argType).newInstance(arg);
    return (T) newObj;
  }

  public static <T> T[] newArray(T[] a, int size) {
    return newArray(getComponentType(a), size);
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<? extends T> getComponentType(T[] a) {
    var cls = a.getClass().getComponentType();
    return (Class<? extends T>) cls;
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(Class<? extends T> k, int size) {
    if (k.isPrimitive()) {
      throw new IllegalArgumentException("Argument cannot be primitive: " + k);
    }
    var a = Array.newInstance(k, size);
    return (T[]) a;
  }

  @SuppressWarnings("unchecked")
  public static <T> T castTo(Object o, Class<T> cls) {
    Class<? extends Object> oc = o.getClass();
    if (cls.isAssignableFrom(oc)) {
      return (T) o;
    }
    return null;
  }

  public static boolean instanceOf(Object o, Class<?> cls) {
    Invariants.notNull(o, cls);
    return cls.isInstance(o);
  }

  public static Object invoke(Object object, String methodName)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Class<?> objectClass = object.getClass();
    Method method;
    try {
      method = objectClass.getMethod(methodName);
    } catch (NoSuchMethodException e) {
      method = objectClass.getDeclaredMethod(methodName);
      method.setAccessible(true);
    }
    return method.invoke(object);
  }
}
