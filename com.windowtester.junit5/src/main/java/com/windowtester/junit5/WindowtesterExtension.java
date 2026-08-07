package com.windowtester.junit5;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

import com.windowtester.junit5.resolver.AnnotationResolver;
import com.windowtester.junit5.resolver.FieldInfo;
import com.windowtester.junit5.resolver.SwingUIContextParameterResolver;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class WindowtesterExtension
    implements ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

  /**
   * Upper bound for {@link #waitUntilReadyForInput(Window)}. Generous on purpose: WindowTracker's
   * own fallback normally ends the wait after {@code abbot.window_ready_delay}, so reaching this
   * means readiness detection is broken rather than slow.
   */
  private static final long READY_TIMEOUT_MILLIS = 10_000;

  private static final long READY_POLL_MILLIS = 20;

  private final SwingUIContextParameterResolver swingUIContextResolver;

  public WindowtesterExtension() {
    swingUIContextResolver = new SwingUIContextParameterResolver();
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return isSwingUIContextParameter(parameterContext, extensionContext);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return swingUIContextResolver.resolveParameter(parameterContext, extensionContext);
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    new AnnotationResolver(context)
        .tryToFindAnnotatedField(UIUnderTest.class)
        .filter(fieldInfo -> fieldInfo.result() instanceof Component)
        .ifPresent(fieldInfo -> showUI(fieldInfo, context));
  }

  private void showUI(FieldInfo fieldInfo, ExtensionContext context) {
    var uiUnderTest = fieldInfo.result();
    var title = getUIUnderTestTitle(fieldInfo);
    var dimension = getUIUnderTestDimension(fieldInfo);
    if (uiUnderTest instanceof Frame frame) {
      title.ifPresent(frame::setTitle);
      showWindow(frame, dimension, context);
    } else if (uiUnderTest instanceof Dialog dialog) {
      title.ifPresent(dialog::setTitle);
      showWindow(dialog, dimension, context);
    } else {
      var window = createWindow((Component) uiUnderTest);
      title.ifPresent(window::setTitle);
      showWindow(window, dimension, context);
    }
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    getStorage(context).wipe();
  }

  private JFrame createWindow(Component component) {
    var baseFrame = createBaseFrame();
    attachComponentToFrame(baseFrame, component);
    return baseFrame;
  }

  private JFrame createBaseFrame() {
    var frame = new JFrame("Test");
    frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    return frame;
  }

  private Optional<String> getUIUnderTestTitle(FieldInfo fieldInfo) {
    var title = fieldInfo.title();
    if (title.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(title);
  }

  private Dimension getUIUnderTestDimension(FieldInfo fieldInfo) {
    return fieldInfo.dimension();
  }

  private void attachComponentToFrame(JFrame jFrame, Component component) {
    var pane = (JPanel) jFrame.getContentPane();
    pane.setBorder(new EmptyBorder(10, 10, 10, 10));
    pane.setLayout(new BorderLayout(5, 5));

    if (component instanceof JComponent comp) {
      comp.setOpaque(true);
    }

    pane.add(component, BorderLayout.CENTER);
  }

  private void showWindow(Window window, Dimension dimension, ExtensionContext context) {
    getStorage(context).saveUIComponent(window);

    try {
      EventQueue.invokeAndWait(
          () -> {
            // Make sure the window is positioned away from
            // any toolbars around the display borders
            // Display the window.
            window.setLocation(100, 100);
            window.pack();
            window.pack();
            window.setSize(dimension);

            window.setAutoRequestFocus(true);
            window.setAlwaysOnTop(true);
            window.toFront();

            window.setVisible(true);
          });
    } catch (InvocationTargetException | InterruptedException e) {
      throw new RuntimeException("Fail to close window.", e);
    }
    waitUntilReadyForInput(window);
  }

  /**
   * Blocks until the window can actually receive OS level input, then lets the event queue settle.
   *
   * @see WindowReadiness
   */
  private static void waitUntilReadyForInput(Window window) {
    var observed =
        WindowReadiness.awaitReadyForInput(
            window, WindowReadiness.windowTracker(), READY_TIMEOUT_MILLIS, READY_POLL_MILLIS);
    if (!observed) {
      return;
    }

    // Showing the window leaves layout and paint work behind. Let it finish, so the first
    // interaction is computed against settled geometry.
    drainEventQueue();
  }

  private static void drainEventQueue() {
    try {
      EventQueue.invokeAndWait(() -> {});
    } catch (InvocationTargetException e) {
      // Nothing was submitted that could fail.
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean isSwingUIContextParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return swingUIContextResolver.supportsParameter(parameterContext, extensionContext);
  }

  private WindowtesterExecutionStorage getStorage(ExtensionContext extensionContext) {
    return new WindowtesterExecutionStorage(extensionContext);
  }
}
