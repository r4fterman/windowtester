/*******************************************************************************
 *  Copyright (c) 2012 Google, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Google, Inc. - initial API and implementation
 *******************************************************************************/
package com.windowtester.runtime.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * The class <code>URLUtilities</code> defines utility methods for working with URL's.
 * <p>
 *
 * @author Brian Wilkerson
 * @version $Revision$
 */
public class URLUtilities {

  /**
   * Prevent the creation of instances of this class.
   */
  private URLUtilities() {
    // hide public constructor
  }

  /**
   * Return a URL which is the local equivalent of the give URL. This method is expected to be used
   * with the plug-in-relative URLs returned by IPluginDescriptor, Bundle.getEntry() and
   * Platform.find(). If the specified URL is not a plug-in-relative URL, it is returned as-is. If
   * the specified URL is a plug-in-relative URL of a file (including a .jar archive), it is
   * returned as a locally-accessible URL using "file:" or "jar:file:" protocol (caching the file
   * locally, if required). If the specified URL is a plug-in-relative URL of a directory, an
   * exception is thrown.
   *
   * @param url original plug-in-relative URL
   * @return the resolved URL
   * @throws IOException if it is unable to resolve URL
   */
  public static URL toFileURL(URL url) throws IOException {
    try {
      return new File(url.toURI()).toURI().toURL();
    } catch (URISyntaxException e) {
      return url;
    }
  }

  /**
   * Encode the given URL so that it is appropriate for use as a URI.
   *
   * @param url the URL to be encoded
   * @return the encoded form of the URL
   */
  public static String encodeURL(String url) {
    var length = url.length();
    var builder = new StringBuilder(length + 20);
    for (int i = 0; i < length; i++) {
      var currentChar = url.charAt(i);
      if (currentChar == ' ') {
        builder.append("%20");
      } else {
        builder.append(currentChar);
      }
    }
    return builder.toString();
  }
}
