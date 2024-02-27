package abbot.script;

import abbot.i18n.Strings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jdom.Element;

/**
 * Script step which groups a sequence of other Steps.  The sub-Steps have a fixed order and are executed in the order
 * contained in the sequence. Events sent by sub-Steps are propagated by this one.
 */
public class Sequence extends Step {

  private static final String USAGE = "<sequence ...>...</sequence>";
  private final ArrayList<Step> sequence = new ArrayList<>();

  public Sequence(Resolver resolver, Element el, Map atts) {
    super(resolver, atts);
    try {
      parseChildren(el);
    } catch (InvalidScriptException ise) {
      setScriptError(ise);
    }
  }

  public Sequence(Resolver resolver, Map atts) {
    super(resolver, atts);
  }

  public Sequence(Resolver resolver, String desc) {
    this(resolver, desc, null);
  }

  public Sequence(Resolver resolver, String desc, List steps) {
    super(resolver, desc);
    if (steps != null) {
      Iterator iter = steps.iterator();
      synchronized (sequence) {
        while (iter.hasNext()) {
          addStep((Step) iter.next());
        }
      }
    }
  }

  protected void parseChild(Element el) throws InvalidScriptException {
    Step step = createStep(getResolver(), el);
    addStep(step);
  }

  protected void parseChildren(Element el) throws InvalidScriptException {
    synchronized (sequence) {
      Iterator iter = el.getContent().iterator();
      while (iter.hasNext()) {
        Object obj = iter.next();
        if (obj instanceof Element) {
          parseChild((Element) obj);
        } else if (obj instanceof org.jdom.Comment) {
          String text = ((org.jdom.Comment) obj).getText();
          addStep(new abbot.script.Comment(getResolver(), text));
        }
      }
    }
  }

  public String getDefaultDescription() {
    return Strings.get("sequence.desc", new Object[] {String.valueOf(size())});
  }

  public String getXMLTag() {
    return TAG_SEQUENCE;
  }

  protected Element addContent(Element el) {
    ArrayList seq;
    synchronized (sequence) {
      seq = (ArrayList) sequence.clone();
    }
    Iterator iter = seq.iterator();
    while (iter.hasNext()) {
      Step step = (Step) iter.next();
      if (step instanceof abbot.script.Comment) {
        el.addContent(new org.jdom.Comment(step.getDescription()));
      } else {
        el.addContent(step.toXML());
      }
    }
    return el;
  }

  public String getUsage() {
    return USAGE;
  }

  public String toEditableString() {
    return getDescription();
  }

  protected void runStep() throws Throwable {
    runStep(null);
  }

  protected void runStep(StepRunner runner) throws Throwable {
    Iterator iter;
    synchronized (sequence) {
      iter = ((ArrayList) sequence.clone()).iterator();
    }
    if (runner != null) {
      while (iter.hasNext() && !runner.stopped()) {
        runner.runStep((Step) iter.next());
      }
    } else {
      while (iter.hasNext()) {
        ((Step) iter.next()).run();
      }
    }
  }

  public int size() {
    synchronized (sequence) {
      return sequence.size();
    }
  }

  /**
   * Remove all stepchildren. More effective than Cinderella's stepmother.
   */
  public void clear() {
    synchronized (sequence) {
      sequence.clear();
    }
  }

  public List<Step> steps() {
    return sequence;
  }

  public int indexOf(Step step) {
    synchronized (sequence) {
      return sequence.indexOf(step);
    }
  }

  public Step getStep(int index) {
    synchronized (sequence) {
      return (Step) sequence.get(index);
    }
  }

  public void addStep(int index, Step step) {
    synchronized (sequence) {
      sequence.add(index, step);
    }
  }

  public void addStep(Step step) {
    synchronized (sequence) {
      sequence.add(step);
    }
  }

  public void setStep(int index, Step step) {
    synchronized (sequence) {
      sequence.set(index, step);
    }
  }

  public void removeStep(Step step) {
    synchronized (sequence) {
      sequence.remove(step);
    }
  }

  public void removeStep(int index) {
    synchronized (sequence) {
      sequence.remove(index);
    }
  }
}
