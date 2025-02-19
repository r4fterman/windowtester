package com.windowtester.internal.debug;

public class MultiStatus implements IStatus {
  private final String productId;
  private final int ok;
  private final IStatus[] children;
  private final String message;
  private final Throwable throwable;

  public MultiStatus(
      String productId, int ok, IStatus[] children, String message, Throwable throwable) {
    this.productId = productId;
    this.ok = ok;
    this.children = children;
    this.message = message;
    this.throwable = throwable;
  }

  public String getMessage() {
    return message;
  }

  public Throwable getException() {
    return throwable;
  }

  public IStatus[] getChildren() {
    return children;
  }
}
