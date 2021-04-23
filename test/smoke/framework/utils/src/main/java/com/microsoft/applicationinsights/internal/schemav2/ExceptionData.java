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
/*
 * Generated from ExceptionData.bond (https://github.com/Microsoft/bond)
*/
package com.microsoft.applicationinsights.internal.schemav2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Data contract class ExceptionData.
 */
public class ExceptionData extends Domain
{
    /**
     * Backing field for property Ver.
     */
    private static final int ver = 2;

    /**
     * Backing field for property Exceptions.
     */
    private List<ExceptionDetails> exceptions;

    /**
     * Backing field for property SeverityLevel.
     */
    private SeverityLevel severityLevel;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;

    /**
     * Initializes a new instance of the ExceptionData class.
     */
    public ExceptionData()
    {
        this.InitializeFields();
    }

    /**
     * Gets the Exceptions property.
     */
    public List<ExceptionDetails> getExceptions() {
        if (this.exceptions == null) {
            this.exceptions = new ArrayList<>();
        }
        return this.exceptions;
    }

    /**
     * Sets the Exceptions property.
     */
    public void setExceptions(List<ExceptionDetails> value) {
        this.exceptions = value;
    }

    /**
     * Gets the SeverityLevel property.
     */
    public SeverityLevel getSeverityLevel() {
        return this.severityLevel;
    }

    /**
     * Sets the SeverityLevel property.
     */
    public void setSeverityLevel(SeverityLevel value) {
        this.severityLevel = value;
    }

    /**
     * Gets the Properties property.
     */
    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<>();
        }
        return this.properties;
    }

    /**
     * Gets the Measurements property.
     */
    public ConcurrentMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new ConcurrentHashMap<>();
        }
        return this.measurements;
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {

    }
}
