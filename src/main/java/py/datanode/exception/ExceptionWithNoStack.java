

package py.datanode.exception;

/**
 * It does not fill in a stack trace for performance reasons.
 */
@SuppressWarnings("serial")
public final class ExceptionWithNoStack extends Exception {
  /**
   * Pre-allocated exception to avoid garbage generation.
   */
  public static final ExceptionWithNoStack INSTANCE = new ExceptionWithNoStack();

  /**
   * Private constructor so only a single instance exists.
   */
  private ExceptionWithNoStack() {
  }

  /**
   * Overridden so the stack trace is not filled in for this exception for performance reasons.
   */
  @Override
  public Throwable fillInStackTrace() {
    return this;
  }
}
