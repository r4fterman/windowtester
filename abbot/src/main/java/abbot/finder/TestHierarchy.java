package abbot.finder;

import abbot.util.AWT;
import java.awt.Component;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;
import java.util.WeakHashMap;

/**
 * Provide isolation of a Component hierarchy to limit consideration to only those Components
 * created during the lifetime of this Hierarchy instance. Extant Components (and any subsequently
 * generated subwindows) are ignored by default.<p> Implicitly auto-filters windows which are
 * disposed (i.event. generate a WINDOW_CLOSED event), but also implicitly un-filters them if they
 * should be shown again.  Any Window explicitly disposed with {@link #dispose(Window)} will be
 * ignored permanently.
 */
public class TestHierarchy extends AWTHierarchy {

  // Map of components to ignore
  private final Map<Component, Boolean> filtered = new WeakHashMap<>();

  /**
   * Create a new TestHierarchy which does not contain any UI Components which might already exist.
   */
  public TestHierarchy() {
    this(true);
  }

  public TestHierarchy(boolean ignoreExisting) {
    if (ignoreExisting) {
      ignoreExisting();
    }
  }

  @Override
  public boolean contains(Component component) {
    return super.contains(component) && !isFiltered(component);
  }

  @Override
  public void dispose(Window window) {
    if (contains(window)) {
      super.dispose(window);
      setFiltered(window, true);
    }
  }

  /**
   * Make all currently extant components invisible to this Hierarchy, without affecting their
   * current state.
   */
  public void ignoreExisting() {
    for (Component component : getRoots()) {
      setFiltered(component, true);
    }
  }

  @Override
  public Collection<Component> getRoots() {
    var components = super.getRoots();
    return components.stream().filter(key -> !filtered.containsKey(key)).toList();
  }

  @Override
  public Collection<Component> getComponents(Component component) {
    if (!isFiltered(component)) {
      var components = super.getComponents(component);
      // NOTE: this only removes those components which are directly
      // filtered, not necessarily those which have a filtered ancestor.
      return components.stream().filter(key -> !filtered.containsKey(key)).toList();
    }
    return EMPTY;
  }

  private boolean isWindowFiltered(Component component) {
    var window = AWT.getWindow(component);
    return window != null && isFiltered(window);
  }

  public boolean isFiltered(Component component) {
    if (component == null) {
      return false;
    }

    return filtered.containsKey(component)
        || ((component instanceof Window) && isFiltered(component.getParent()))
        || (!(component instanceof Window) && isWindowFiltered(component));
  }

  private void setFiltered(Component component, boolean filter) {
    if (AWT.isSharedInvisibleFrame(component)) {
      getComponents(component).forEach(c -> setFiltered(c, filter));
      return;
    }

    if (filter) {
      filtered.put(component, Boolean.TRUE);
    } else {
      filtered.remove(component);
    }

    if (component instanceof Window window) {
      var windows = window.getOwnedWindows();
      Arrays.stream(windows).forEach(w -> setFiltered(w, filter));
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TestHierarchy.class.getSimpleName() + "[", "]")
        .add("filtered=" + filtered)
        .toString();
  }
}
