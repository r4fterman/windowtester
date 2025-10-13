package com.windowtester.junit5.resolver;

import com.windowtester.internal.swing.UIContextSwingFactory;
import com.windowtester.junit5.SwingUIContext;
import com.windowtester.junit5.WindowtesterExecutionStorage;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class SwingUIContextParameterResolver implements ParameterResolver {

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().isAnnotationPresent(SwingUIContext.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getStorage(extensionContext).saveUIContext(UIContextSwingFactory.createContext());
  }

  private WindowtesterExecutionStorage getStorage(ExtensionContext extensionContext) {
    return new WindowtesterExecutionStorage(extensionContext);
  }
}
