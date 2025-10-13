package com.windowtester.runtime.swing.locator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JMenuItemLocatorTest {

  @Test
  void path_should_be_returned() {
    var locator = new JMenuItemLocator("File/Exit");

    assertEquals("File/Exit", locator.getPath());
  }

  @Test
  void path_should_be_parsed_into_name_and_label() {
    var locator = new JMenuItemLocator("File/Exit");

    assertEquals("Exit", locator.getNameOrLabel());
  }
}
