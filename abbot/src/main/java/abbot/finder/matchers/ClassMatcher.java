package abbot.finder.matchers;

import java.awt.Component;

/**
 * Provides matching of components by class.
 */
public class ClassMatcher extends AbstractMatcher {

  private final Class<?> cls;
  private final boolean mustBeShowing;

  public ClassMatcher(Class<?> cls) {
    this(cls, false);
  }

  public ClassMatcher(Class<?> cls, boolean mustBeShowing) {
    this.cls = cls;
    this.mustBeShowing = mustBeShowing;
  }

  @Override
  public boolean matches(Component component) {
    return cls.isAssignableFrom(component.getClass()) && (!mustBeShowing || component.isShowing());
  }

  @Override
  public String toString() {
    return "Class matcher (" + cls.getName() + ")";
  }
}
