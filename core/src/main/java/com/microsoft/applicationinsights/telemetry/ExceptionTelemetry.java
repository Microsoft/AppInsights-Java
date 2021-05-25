/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.telemetry;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.StackFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Telemetry type used to track exceptions sent to Azure Application Insights.
 */
public final class ExceptionTelemetry extends BaseSampleSourceTelemetry<ExceptionData> {

    private static final int MAX_PARSED_STACK_LENGTH = 32768; // Breeze will reject parsedStack exceeding 65536 bytes. Each char is 2 bytes long.
    private static final Logger logger = LoggerFactory.getLogger(ExceptionTelemetry.class);
    private Double samplingPercentage;
    private final ExceptionData data;
    private Throwable throwable;

    /**
     * Envelope Name for this telemetry.
     */
    public static final String ENVELOPE_NAME = "Exception";


    /**
     * Base Type for this telemetry.
     */
    public static final String BASE_TYPE = "ExceptionData";


    public ExceptionTelemetry() {
        super();
        data = new ExceptionData();
        initialize(data.getProperties());
    }

    /**
     * Initializes a new instance.
     * @param stackSize The max stack size to report.
     * @param exception The exception to track.
     */
    public ExceptionTelemetry(Throwable exception, int stackSize) {
        this();
        setException(exception, stackSize);
    }

    /**
     * Initializes a new instance.
     * @param exception The exception to track.
     */
    public ExceptionTelemetry(Throwable exception) {
        this(exception, Integer.MAX_VALUE);
    }

    public Exception getException() {
        return throwable instanceof Exception ? (Exception)throwable : null;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setException(Throwable throwable) {
        setException(throwable, Integer.MAX_VALUE);
    }

    public void setException(Throwable throwable, int stackSize) {
        this.throwable = throwable;
        updateException(throwable, stackSize);
    }

    /**
     * Gets a map of application-defined exception metrics.
     * The metrics appear along with the exception in Analytics, but under Custom Metrics in Metrics Explorer.
     * @return The map of metrics
     */
    public ConcurrentMap<String,Double> getMetrics() {
        return data.getMeasurements();
    }

    public void setSeverityLevel(SeverityLevel severityLevel) {
        data.setSeverityLevel(severityLevel == null ? null : com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.values()[severityLevel.getValue()]);
    }

    public SeverityLevel getSeverityLevel() {
        return data.getSeverityLevel() == null ? null : SeverityLevel.values()[data.getSeverityLevel().getValue()];
    }

    @Override
    public Double getSamplingPercentage() {
        return samplingPercentage;
    }

    @Override
    public void setSamplingPercentage(Double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
    }

    @Override
    public ExceptionData getData() {
        return data;
    }

    public List<ExceptionDetails> getExceptions() {
        return data.getExceptions();
    }

    private void updateException(Throwable throwable, int stackSize) {
        ArrayList<ExceptionDetails> exceptions = new ArrayList<>();
        convertExceptionTree(throwable, null, exceptions, stackSize);

        data.setExceptions(exceptions);
    }

    private static void convertExceptionTree(Throwable exception, ExceptionDetails parentExceptionDetails, List<ExceptionDetails> exceptions, int stackSize) {
        if (exception == null) {
            exception = new Exception("");
        }

        if (stackSize == 0) {
            return;
        }

        ExceptionDetails exceptionDetails = createWithStackInfo(exception, parentExceptionDetails);
        exceptions.add(exceptionDetails);

        if (exception.getCause() != null) {
            convertExceptionTree(exception.getCause(), exceptionDetails, exceptions, stackSize - 1);
        }
    }

    private static ExceptionDetails createWithStackInfo(Throwable exception, ExceptionDetails parentExceptionDetails) {
        if (exception == null) {
            throw new IllegalArgumentException("exception cannot be null");
        }

        ExceptionDetails exceptionDetails = new ExceptionDetails();
        exceptionDetails.setId(exception.hashCode());
        exceptionDetails.setTypeName(exception.getClass().getName());

        String exceptionMessage = exception.getMessage();
        if (Strings.isNullOrEmpty(exceptionMessage)) {
            exceptionMessage = exception.getClass().getName();
        }
        exceptionDetails.setMessage(exceptionMessage);

        if (parentExceptionDetails != null) {
            exceptionDetails.setOuterId(parentExceptionDetails.getId());
        }

        StackTraceElement[] trace = exception.getStackTrace();
        exceptionDetails.setHasFullStack(true);
        if (trace != null && trace.length > 0) {
            List<StackFrame> stack = exceptionDetails.getParsedStack();

            // We need to present the stack trace in reverse order.
            int stackLength = 0;
            for (int idx = 0; idx < trace.length; idx++) {
                StackTraceElement elem = trace[idx];

                if (elem.isNativeMethod()) {
                    continue;
                }

                String className = elem.getClassName();

                StackFrame frame = new StackFrame();
                frame.setLevel(idx);
                frame.setFileName(elem.getFileName());
                frame.setLine(elem.getLineNumber());

                if (!Strings.isNullOrEmpty(className)) {
                    frame.setMethod(elem.getClassName() + "." + elem.getMethodName());
                }
                else {
                    frame.setMethod(elem.getMethodName());
                }

                stackLength += getStackFrameLength(frame);
                if (stackLength > MAX_PARSED_STACK_LENGTH) {
                    exceptionDetails.setHasFullStack(false);
                    logger.debug("parsedStack is exceeding 65536 bytes capacity. It is truncated from full {} frames to partial {} frames.", trace.length, stack.size());
                    break;
                }

                stack.add(frame);
            }
        }

        return exceptionDetails;
    }
    @Override
    public String getEnvelopName() {
        return ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return BASE_TYPE;
    }

    /***
     * @param stackFrame
     * @return the stack frame length for only the strings in the stack frame.
     */
     // this is the same logic used to limit length on the Breeze side
    private static int getStackFrameLength(StackFrame stackFrame)
    {
        int stackFrameLength = (stackFrame.getMethod() == null ? 0 : stackFrame.getMethod().length())
                + (stackFrame.getAssembly() == null ? 0 : stackFrame.getAssembly().length())
                + (stackFrame.getFileName() == null ? 0 : stackFrame.getFileName().length());
        return stackFrameLength;
    }
}
