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
package com.windowtester.internal.debug;

import abbot.util.AbbotTimerTask;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.Timer;

/**
 * Thread debugging utility.
 */
public class ThreadUtil {

  private static final Object LOCK = new Object();
  private static Timer timer;

  /**
   * Periodically dump the stack to System.err. This automatically cancels any previously scheduled
   * stack dumps.
   */
  public static void startPrintStackTraces(long period) {
    Thread callingThread = Thread.currentThread();
    synchronized (LOCK) {
      stopPrintStackTraces();
      timer = new Timer("ThreadUtil Stack Dump");
      timer.schedule(
          new AbbotTimerTask() {
            @Override
            public void run() {
              System.err.println(
                  "**********************************************************************************");
              System.err.println("Periodic Thread Dump: " + System.currentTimeMillis());
              System.err.println("Calling Thread: " + callingThread);
              printStackTraces();
            }
          },
          period,
          period);
    }
  }

  /**
   * Stop any scheduled stack dumps
   */
  public static void stopPrintStackTraces() {
    synchronized (LOCK) {
      if (timer != null) {
        timer.cancel();
        timer = null;
      }
    }
  }

  /**
   * Get a string representation of the current stack state of all the active threads.
   */
  public static String getStackTraces() {
    var stringWriter = new StringWriter(5000);
    printStackTraces(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  /**
   * Print a string representation of the current stack state of all the active threads.
   */
  public static void printStackTraces() {
    printStackTraces(new PrintWriter(new OutputStreamWriter(System.err)));
  }

  /**
   * Print a string representation of the current stack state of all the active threads.
   */
  public static void printStackTraces(PrintWriter writer) {
    try {
      var map = Thread.getAllStackTraces();
      for (Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
        printStackTrace(writer, entry.getKey(), entry.getValue());
      }
      writer.flush();
    } catch (Throwable e) {
      writer.println("Failed to obtain stack traces: " + e);
    }
  }

  private static void printStackTrace(
      PrintWriter writer, Thread thread, StackTraceElement[] trace) {
    try {
      writer.println(thread.toString() + ":");
      for (StackTraceElement stackTraceElement : trace) {
        writer.println("\tat " + stackTraceElement);
      }
    } catch (Exception e) {
      writer.println("\t*** Exception printing stack trace: " + e);
    }
    writer.flush();
  }
}
