/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.release;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author Edwin Punzalan
 */
public class ReleaseResult {
    /** The result of the release. */
    public static final int UNDEFINED = -1;
    /** The release was successful. */
    public static final int SUCCESS = 0;
    /** The release failed. */
    public static final int ERROR = 1;

    private StringBuilder stdOut = new StringBuilder();

    private int resultCode = UNDEFINED;

    private long startTime;

    private long endTime;

    private static final String LS = System.getProperty("line.separator");

    /**
     * Append Info message to the output.
     * @param message the message to append
     */
    public void appendInfo(String message) {
        stdOut.append("[INFO] ").append(message).append(LS);
    }
    /**
     * Append warning message to the output.
     * @param message the message to append
     */
    public void appendWarn(String message) {
        stdOut.append("[WARN] ").append(message).append(LS);
    }

    /**
     * Append debug message to the output.
     * @param message the message to append
     */
    public void appendDebug(String message) {
        stdOut.append("[DEBUG] ").append(message).append(LS);
    }

    /**
     * Append error message to the output.
     * @param message the message to append
     * @param e the exception to append
     */
    public void appendDebug(String message, Exception e) {
        appendDebug(message);

        stdOut.append(getStackTrace(e)).append(LS);
    }

    /**
     * Append error message to the output.
     *
     * @param message the message to append
     */
    public void appendError(String message) {
        stdOut.append("[ERROR] ").append(message).append(LS);

        setResultCode(ERROR);
    }

    /**
     * Append error exception to the output
     *
     * @param e the exception to append
     */
    public void appendError(Exception e) {
        appendError(getStackTrace(e));
    }

    /**
     * Append stack trace to the output
     *
     * @param message the message to append
     * @param e the exception to append
     */
    public void appendError(String message, Exception e) {
        appendError(message);

        stdOut.append(getStackTrace(e)).append(LS);
    }

    /**
     * Append message to the output.
     *
     * @param message the message to append
     */
    public void appendOutput(String message) {
        stdOut.append(message);
    }

    public String getOutput() {
        return stdOut.toString();
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    private String getStackTrace(Exception e) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        PrintStream stream = new PrintStream(byteStream);

        e.printStackTrace(stream);

        stream.flush();

        return byteStream.toString();
    }
}
