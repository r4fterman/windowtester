package com.windowtester.internal.swing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PathStringTokenizerUtilTest {

  @Test
  void tokenize_should_split_path_at_slash_character() {
    var tokenized = PathStringTokenizerUtil.tokenize("/a/b/c/d/E");

    assertEquals(5, tokenized.length);
    assertEquals("a", tokenized[0]);
    assertEquals("b", tokenized[1]);
    assertEquals("c", tokenized[2]);
    assertEquals("d", tokenized[3]);
    assertEquals("E", tokenized[4]);
  }
}
