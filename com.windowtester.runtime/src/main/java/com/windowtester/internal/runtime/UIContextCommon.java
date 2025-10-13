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

import static com.windowtester.internal.runtime.util.StringUtils.NEW_LINE;

import com.windowtester.internal.debug.IRuntimePluginTraceOptions;
import com.windowtester.internal.debug.LogHandler;
import com.windowtester.internal.debug.TraceHandler;
import com.windowtester.internal.runtime.condition.ConditionMonitor;
import com.windowtester.internal.runtime.locator.IContextMenuItemLocator;
import com.windowtester.internal.runtime.selector.ClickHelper;
import com.windowtester.internal.runtime.selector.IClickDriver;
import com.windowtester.runtime.IUIContext;
import com.windowtester.runtime.WaitTimedOutException;
import com.windowtester.runtime.WidgetSearchException;
import com.windowtester.runtime.condition.ICondition;
import com.windowtester.runtime.condition.IConditionHandler;
import com.windowtester.runtime.condition.IConditionMonitor;
import com.windowtester.runtime.internal.AssertionHandler;
import com.windowtester.runtime.internal.IAssertionHandler;
import com.windowtester.runtime.locator.ILocator;
import com.windowtester.runtime.locator.IMenuItemLocator;
import com.windowtester.runtime.locator.IWidgetLocator;
import com.windowtester.runtime.locator.MenuItemLocator;
import com.windowtester.runtime.util.ScreenCapture;
import com.windowtester.runtime.util.TestMonitor;

/**
 * Abstract superclass of {@link UIContextSwing}.
 */
public abstract class UIContextCommon implements IUIContext {

  // dump system information
  static {
    StringBuilder sb = new StringBuilder();
    sb.append(NEW_LINE)
        .append("*************************************************")
        .append(NEW_LINE)
        .append("WindowTester Runtime " + ProductInfo.build)
        .append(NEW_LINE);
    echoSystemProperties("OS:", new String[] {"os.name", "os.arch", "os.version"}, sb);
    echoSystemProperties("Java:", new String[] {"java.vendor", "java.version"}, sb);
    echoSystemProperties(
        "Spec:",
        new String[] {
          "java.specification.name", "java.specification.vendor", "java.specification.version"
        },
        sb);
    echoSystemProperties(
        "VM:",
        new String[] {
          "java.vm.specification.name",
          "java.vm.specification.vendor",
          "java.vm.specification.version"
        },
        sb);
    sb.append("*************************************************").append(NEW_LINE);

    LogHandler.log(sb.toString());
  }

  private static void echoSystemProperties(String tag, String[] keys, StringBuilder sb) {
    sb.append("   ");
    sb.append(tag);
    sb.append(" ".repeat(Math.max(0, 6 - tag.length())));
    sb.append(System.getProperty(keys[0]));
    for (int i = 1; i < keys.length; i++) {
      sb.append(", ");
      sb.append(System.getProperty(keys[i]));
    }
    sb.append(NEW_LINE);
  }

  private IClickDriver clickHelper;
  private IConditionMonitor conditionMonitor;
  private IAssertionHandler assertionHandler;

  @Override
  public Object getAdapter(Class<?> adapter) {
    // TODO[author=pq]: (how) can clients add adapters?
    if (adapter == IConditionMonitor.class) {
      return getConditionMonitor();
    }
    return null;
  }

  @Override
  public IWidgetLocator click(ILocator locator) throws WidgetSearchException {
    // unifying context and standard clicks
    if (locator instanceof IContextMenuItemLocator context) {
      return contextClick(context.getOwner(), context.getMenuPath());
    }

    return click(1, locator);
  }

  @Override
  public IWidgetLocator click(int clickCount, ILocator locator) throws WidgetSearchException {
    return click(clickCount, locator, getDefaultButtonMask());
  }

  @Override
  public IWidgetLocator click(int clickCount, ILocator locator, int buttonMask)
      throws WidgetSearchException {
    return getClickDriver().click(clickCount, locator, buttonMask);
  }

  @Override
  public IWidgetLocator contextClick(ILocator locator, IMenuItemLocator menuItem)
      throws WidgetSearchException {
    return getClickDriver().contextClick(locator, menuItem);
  }

  @Override
  public IWidgetLocator contextClick(ILocator locator, IMenuItemLocator menuItem, int modifierMask)
      throws WidgetSearchException {
    throw new UnsupportedOperationException();
  }

  @Override
  public IWidgetLocator contextClick(ILocator locator, String menuItem)
      throws WidgetSearchException {
    return contextClick(locator, new MenuItemLocator(menuItem));
  }

  @Override
  public IWidgetLocator contextClick(ILocator locator, String menuItem, int modifierMask)
      throws WidgetSearchException {
    return contextClick(locator, new MenuItemLocator(menuItem), modifierMask);
  }

  /**
   * Get the click driver (responsible for performing clicks)
   */
  protected IClickDriver getClickDriver() {
    if (clickHelper == null) {
      clickHelper = new ClickHelper(this);
    }
    return clickHelper;
  }

  /**
   * Get the default mouse button mask in case it is unspecified.
   */
  protected abstract int getDefaultButtonMask();

  @Override
  public IConditionMonitor getConditionMonitor() {
    if (conditionMonitor == null) {
      conditionMonitor = new ConditionMonitor(ConditionMonitor.getInstance());
    }
    return conditionMonitor;
  }

  /**
   * Answer the invariant handler local to this particular UI context.
   *
   * @return the local invariant handler (not <code>null</code>).
   * @since 3.7.1
   */
  protected IAssertionHandler getAssertionHandler() {
    if (assertionHandler == null) {
      assertionHandler = new AssertionHandler(this);
    }
    return assertionHandler;
  }

  @Override
  public void assertThat(ICondition condition) throws WaitTimedOutException {
    getAssertionHandler().assertThat(condition);
  }

  @Override
  public void assertThat(String message, ICondition condition) throws WaitTimedOutException {
    getAssertionHandler().assertThat(message, condition);
  }

  @Override
  public void ensureThat(IConditionHandler conditionHandler) throws Exception {
    getAssertionHandler().ensureThat(conditionHandler);
  }

  @Override
  public int handleConditions() {
    return getConditionMonitor().process(this);
  }

  /**
   * Capture the current screen as a file in a standard location with a name based upon the current
   * test and test case.
   *
   * @param desc the description for logging purposes
   */
  public void doScreenCapture(String desc) {
    var testcaseID = TestMonitor.getInstance().getCurrentTestCaseID();
    TraceHandler.trace(
        IRuntimePluginTraceOptions.CONDITIONS,
        "Creating screenshot (" + desc + ") for testcase: " + testcaseID);
    ScreenCapture.createScreenCapture(testcaseID);
  }
}
