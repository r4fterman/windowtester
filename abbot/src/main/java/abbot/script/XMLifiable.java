package abbot.script;

import org.dom4j.Element;

public interface XMLifiable {
  /**
   * Provide an XML representation of the object.
   * @return xml element
   */
  Element toXML();
}
