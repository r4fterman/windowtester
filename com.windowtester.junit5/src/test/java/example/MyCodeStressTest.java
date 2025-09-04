package example;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import abbot.finder.AWTHierarchy;
import abbot.script.Script;
import abbot.script.StepRunner;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Simple example of a stress test on an app.
 */
class MyCodeStressTest {

  @RepeatedTest(10)
  void run_script() throws Throwable {
    Script script = new Script(getFile(), new AWTHierarchy());
    new StepRunner().run(script);
  }

  private File getFile() throws URISyntaxException {
    var filename = "StressMyCode.xml";
    var url = MyCodeTest.class.getResource(filename);
    assertNotNull(url, () -> "File not found: " + filename);
    return new File(url.toURI());
  }
}
