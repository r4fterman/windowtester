package example;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import abbot.script.Script;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

/**
 * Simple example of a ScriptTestSuite.  Selects all scripts of the form MyCode-[0-9]*.xml.
 */
public class MyCodeTest {

  @Test
  void run_script_0() throws Throwable {
    Script script = new Script(getFile("MyCode-0.xml"));
    script.run();
  }

  @Test
  void run_script_1() throws Throwable {
    Script script = new Script(getFile("MyCode-1.xml"));
    script.run();
  }

  @Test
  void run_script_2() throws Throwable {
    Script script = new Script(getFile("MyCode-2.xml"));
    script.run();
  }

  private File getFile(String filename) throws URISyntaxException {
    var url = MyCodeTest.class.getResource(filename);
    assertNotNull(url, () -> "File not found: " + filename);
    return new File(url.toURI());
  }
}
