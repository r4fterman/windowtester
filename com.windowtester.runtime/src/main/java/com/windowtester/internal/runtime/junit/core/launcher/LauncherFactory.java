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
package com.windowtester.internal.runtime.junit.core.launcher;

import com.windowtester.internal.debug.LogHandler;
import com.windowtester.internal.runtime.junit.core.ISequenceRunner.IRunnable;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory for Application launchers.
 */
public class LauncherFactory {

  /**
   * Abstract base launcher implementation.
   */
  abstract static class AbstractLauncher implements IApplicationLauncher {

    private final List<ILaunchListener> listeners = new ArrayList<>();

    @Override
    public void addListener(ILaunchListener listener) {
      listeners.add(listener);
    }

    /**
     * Notify listeners post launch.
     */
    protected void notifyPostLaunch() {
      for (ILaunchListener listener : listeners) {
        listener.postFlight();
      }
    }

    /**
     * Notify listeners pre launch.
     */
    protected void notifyPreLaunch() {
      for (ILaunchListener listener : listeners) {
        listener.preFlight();
      }
    }
  }

  /**
   * A special launcher that does nothing (presumably because the application is bootstrapped
   * elsewhere).
   */
  static class NoOpLauncher extends AbstractLauncher {

    @Override
    public void launch() {
      // do nothing
    }
  }

  /**
   * A launcher that spawns another thread in which to execute. Note that pre and post launch
   * notifications occur before and after the thread is started respectively.
   */
  static class SeparateThreadLauncher extends AbstractLauncher {

    private final IRunnable runnable;

    public SeparateThreadLauncher(IRunnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void launch() {
      notifyPreLaunch();
      Thread t =
          new Thread(
              () -> {
                try {
                  runnable.run();
                } catch (Throwable e) {
                  e.printStackTrace();
                  LogHandler.log(e);
                }
              });
      t.start();
      notifyPostLaunch();
    }
  }

  /**
   * A launcher that invokes the main method of a class in a separate thread.
   */
  static class MainRunner extends SeparateThreadLauncher {

    public MainRunner(Class<?> launchClass, String[] launchArgs) {
      super(
          () -> {
            var nullArgs = new String[0];
            var main = launchClass.getMethod("main", String[].class);

            Object[] realArgs;
            // instead pass null string
            if (launchArgs == null) {
              realArgs = new Object[] {nullArgs};
            } else {
              realArgs = new Object[] {launchArgs};
            }

            main.invoke(null, realArgs);
          });
    }
  }

  /**
   * Launcher factory method; creates a launcher appropriate for the given arguments.
   *
   * @param launchClass the class to launch (may be <code>null</code>)
   * @param launchArgs  the program arguments to pass to the launched class (may be
   *                    <code>null</code>)
   * @return a launcher appropriate to the given arguments
   */
  public static IApplicationLauncher create(Class<?> launchClass, String[] launchArgs) {
    if (launchClass == null) {
      return new NoOpLauncher();
    }
    return new MainRunner(launchClass, launchArgs);
  }
}
