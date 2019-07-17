/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weidi.usefragments.tool;

import java.util.regex.Pattern;

/**
 * Thrown when a non-recoverable playback failure occurs.
 * <p>
 * Where possible, the cause returned by {@link #getCause()} will indicate the reason for failure.
 */
public final class ExoPlaybackException extends Exception {

  // Extraction pattern for code and message.
  public static final Pattern ERROR_PATTERN = Pattern.compile("^CODE=([-0-9]*) ,MESSAGE=" +
          "([-_.!~*\\\\'()a-zA-Z0-9;\\\\/?:\\\\@&=+\\\\$,%#\\s]+)$");

  // Error code.
  public static final int EXOPLAYER_ERROR_SERVER_DIED = 100;
  public static final int EXOPLAYER_ERROR_UNKNOWN = 1;
  public static final int EXOPLAYER_ERROR_IO = -1004;
  public static final int EXOPLAYER_ERROR_OPEN_FILE_FAILED = -5004;

  /**
   * True if the cause (i.e. the {@link Throwable} returned by {@link #getCause()}) was only caught
   * by a fail-safe at the top level of the player. False otherwise.
   */
  public final boolean caughtAtTopLevel;
  public final int what;
  public final int extra;

  public ExoPlaybackException(String message) {
    super(message);
    caughtAtTopLevel = false;
    this.what = 1;
    this.extra = 0;
  }

  public ExoPlaybackException(Throwable cause) {
    super(cause);
    caughtAtTopLevel = false;
    this.what = 1;
    this.extra = 0;
  }

  public ExoPlaybackException(String message, Throwable cause) {
    super(message, cause);
    caughtAtTopLevel = false;
    this.what = 1;
    this.extra = 0;
  }

  /* package */ ExoPlaybackException(Throwable cause, boolean caughtAtTopLevel) {
    super(cause);
    this.caughtAtTopLevel = caughtAtTopLevel;
    this.what = 1;
    this.extra = 0;
  }

  public ExoPlaybackException(String message, int what, int extra) {
    super(message);
    caughtAtTopLevel = false;
    this.what = what;
    this.extra = extra;
  }

  ExoPlaybackException(String message, boolean caughtAtTopLevel, int what, int extra) {
    super(message);
    this.caughtAtTopLevel = caughtAtTopLevel;
    this.what = what;
    this.extra = extra;
  }

  public static String createErrorMessage(int code, String message) {
    return new String("CODE=" + code + " ,MESSAGE=" + message);
  }

}
