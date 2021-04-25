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

package com.microsoft.applicationinsights.internal.config;

/**
 * Created by gupele on 3/13/2015.
 */
public class ApplicationInsightsXmlConfiguration {

    private String connectionString;

    private String roleName;

    private String roleInstance;

    private TelemetryModulesXmlElement modules;

    private final PerformanceCountersXmlElement performance = new PerformanceCountersXmlElement();

    private QuickPulseXmlElement quickPulse;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleInstance() {
        return roleInstance;
    }

    public void setRoleInstance(String roleInstance) {
        this.roleInstance = roleInstance;
    }

    public QuickPulseXmlElement getQuickPulse() {
        if (quickPulse == null) {
            quickPulse = new QuickPulseXmlElement();
        }
        return quickPulse;
    }

    public TelemetryModulesXmlElement getModules() {
        return modules;
    }

    public void setModules(TelemetryModulesXmlElement modules) {
        this.modules = modules;
    }

    public PerformanceCountersXmlElement getPerformance() {
        return performance;
    }
}
