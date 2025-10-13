package com.windowtester.internal.debug;

public class Status implements IStatus {

  public static final int OK = 0;
  public static final int ERROR = 1;
  public static final int INFO = 2;

  private final int status;
  private final String productId;
  private final int ok;
  private final String message;
  private final Throwable throwable;

  public Status(int status, String productId, int ok, String message, Throwable throwable) {
    this.status = status;
    this.productId = productId;
    this.ok = ok;
    this.message = message;
    this.throwable = throwable;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Throwable getException() {
    return throwable;
  }

  @Override
  public IStatus[] getChildren() {
    return new IStatus[0];
  }
}
