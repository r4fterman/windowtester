package abbot.script;

import org.dom4j.Element;

public interface XMLifiable {
  /**
   * Provide an XML representation of the object.
   * @return xml element
   */
  Element toXML();

  /**
   * Provide an editable string representation of the object.  Usually will be a String form of the XML, but may be
   * something simpler if it doesn't make sense to provide the full XML.
   * @return editable string
   */
  String toEditableString();
}
