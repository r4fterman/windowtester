package abbot.finder;

import abbot.i18n.Strings;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 * Provides basic comp lookup, examining each comp in turn. Searches all components of interest in a
 * given hierarchy.
 */
public class BasicFinder implements ComponentFinder {

  private static final ComponentFinder DEFAULT = new BasicFinder(new AWTHierarchy());

  public static ComponentFinder getDefault() {
    return DEFAULT;
  }

  private final Hierarchy hierarchy;

  private class SingleComponentHierarchy implements Hierarchy {

    private final List<Component> list = new ArrayList<>();
    private final Component root;

    public SingleComponentHierarchy(Container root) {
      this.root = root;
      list.add(root);
    }

    @Override
    public Collection<Component> getRoots() {
      return list;
    }

    @Override
    public Collection<Component> getComponents(Component component) {
      return getHierarchy().getComponents(component);
    }

    @Override
    public Container getParent(Component component) {
      return getHierarchy().getParent(component);
    }

    @Override
    public boolean contains(Component component) {
      return getHierarchy().contains(component) && SwingUtilities.isDescendingFrom(component, root);
    }

    @Override
    public void dispose(Window window) {
      getHierarchy().dispose(window);
    }
  }

  public BasicFinder() {
    this(AWTHierarchy.getDefault());
  }

  public BasicFinder(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  protected Hierarchy getHierarchy() {
    return hierarchy;
  }

  @Override
  public Component find(Container root, Matcher matcher)
      throws ComponentNotFoundException, MultipleComponentsFoundException {
    return find(toHierarchy(root), matcher);
  }

  private Hierarchy toHierarchy(Container container) {
    if (container != null) {
      return new SingleComponentHierarchy(container);
    }
    return getHierarchy();
  }

  @Override
  public Component find(Matcher matcher)
      throws ComponentNotFoundException, MultipleComponentsFoundException {
    return find((Container) null, matcher);
  }

  protected Component find(Hierarchy hierarchy, Matcher matcher)
      throws ComponentNotFoundException, MultipleComponentsFoundException {
    var found = new HashSet<Component>();
    for (Component component : hierarchy.getRoots()) {
      findMatches(hierarchy, matcher, component, found);
    }

    if (found.isEmpty()) {
      var msg = Strings.get("finder.not_found", new Object[] {matcher.toString()});
      throw new ComponentNotFoundException(msg);
    } else if (found.size() > 1) {
      var list = found.toArray(new Component[0]);
      if (!(matcher instanceof MultiMatcher)) {
        var msg = Strings.get("finder.multiple_found", new Object[] {matcher.toString()});
        throw new MultipleComponentsFoundException(msg, list);
      }
      return ((MultiMatcher) matcher).bestMatch(list);
    }
    return found.iterator().next();
  }

  protected void findMatches(
      Hierarchy hierarchy, Matcher matcher, Component component, Set<Component> found) {
    if (found.size() == 1 && !(matcher instanceof MultiMatcher)) {
      return;
    }

    for (Component value : hierarchy.getComponents(component)) {
      findMatches(hierarchy, matcher, value, found);
    }

    if (matcher.matches(component)) {
      found.add(component);
    }
  }
}
