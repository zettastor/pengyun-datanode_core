/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
