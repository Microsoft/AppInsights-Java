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

package com.microsoft.applicationinsights.agent.internal.agent;

import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An 'instrumented' class data
 *
 * Created by gupele on 5/19/2015.
 */
public final class ClassInstrumentationData {

    private final static ClassVisitorFactory s_defaultClassVisitorFactory = new ClassVisitorFactory() {
        @Override
        public DefaultClassVisitor create(ClassInstrumentationData classInstrumentationData, ClassWriter classWriter) {
            return new DefaultClassVisitor(classInstrumentationData, classWriter);
        }
    };

    private final static MethodVisitorFactory s_defaultMethodVisitorFactory = new MethodVisitorFactory() {
        @Override
        public MethodVisitor create(MethodInstrumentationDecision decision,
                                    int access,
                                    String desc,
                                    String owner,
                                    String methodName,
                                    MethodVisitor methodVisitor,
                                    ClassToMethodTransformationData additionalData) {
            return new DefaultMethodVisitor(decision, access, desc, owner, methodName, methodVisitor, additionalData);
        }
    };

    private final String className;

    // The type of class
    private final String classType;

    // Methods that will be instrumented
    private MethodInstrumentationInfo methodInstrumentationInfo;

    private long thresholdInMS;
    private boolean reportExecutionTime;
    private boolean reportCaughtExceptions;

    private ClassVisitorFactory classVisitorFactory;

    private Pattern classNamePattern;
    private String onlyPackageName;

    public ClassInstrumentationData(String className, InstrumentedClassType classType) {
        this(className, classType.toString());
    }

    public ClassInstrumentationData(String className, String classType) {
        this(className, classType, null);
    }

    public ClassInstrumentationData(String className, InstrumentedClassType classType, ClassVisitorFactory classVisitorFactory) {
        this(className, classType.toString(), classVisitorFactory);
    }

    public ClassInstrumentationData(String className, String classType, ClassVisitorFactory classVisitorFactory) {
        this.classType = classType;
        this.methodInstrumentationInfo = new MethodInstrumentationInfo();
        if (classVisitorFactory == null) {
            this.classVisitorFactory = s_defaultClassVisitorFactory;
        } else {
            this.classVisitorFactory = classVisitorFactory;
        }

        int index = className.lastIndexOf("/");
        if (index != -1) {
            String onlyClassName = className.substring(index + 1);
            if (className.contains("*")) {
                onlyPackageName = className.substring(0, index + 1);
                onlyClassName = onlyClassName.replace("*", ".*");
                classNamePattern = Pattern.compile(onlyClassName);
                this.className = null;
                return;
            }
        }

        onlyPackageName = null;
        classNamePattern = null;
        this.className = className;
    }

    public boolean addMethod(String methodName, String signature, boolean reportCaughtExceptions, boolean reportExecutionTime, long thresholdInMS) {
        return addMethod(methodName, signature, reportCaughtExceptions, reportExecutionTime, thresholdInMS, null);
    }

    public boolean addMethod(String methodName, String signature, boolean reportCaughtExceptions, boolean reportExecutionTime, long thresholdInMS, MethodVisitorFactory methodVisitorFactory) {
        if (StringUtils.isNullOrEmpty(methodName)) {
            return false;
        }

        MethodInstrumentationRequest request =
                new MethodInstrumentationRequestBuilder()
                .withMethodName(methodName)
                .withMethodSignature(signature)
                .withThresholdInMS(thresholdInMS)
                .withReportCaughtExceptions(reportCaughtExceptions)
                .withReportExecutionTime(reportExecutionTime).create();
        methodInstrumentationInfo.addMethod(request, methodVisitorFactory == null ? s_defaultMethodVisitorFactory : methodVisitorFactory);
        return true;
    }

    public ClassInstrumentationData setReportExecutionTime(boolean reportExecutionTime) {
        this.reportExecutionTime = reportExecutionTime;
        return this;
    }

    public ClassInstrumentationData setReportCaughtExceptions(boolean reportCaughtExceptions) {
        this.reportCaughtExceptions = reportCaughtExceptions;
        return this;
    }

    public ClassInstrumentationData setClassVisitorFactory(ClassVisitorFactory classVisitorFactory) {
        if (classVisitorFactory != null) {
            this.classVisitorFactory = classVisitorFactory;
        }
        return this;
    }

    public ClassVisitorFactory getClassVisitorFactory() {
        return this.classVisitorFactory;
    }

    public void addAllMethods(boolean reportCaughtExceptions, boolean reportExecutionTime, MethodVisitorFactory methodVisitorFactory) {
        methodInstrumentationInfo.addAllMethods(reportCaughtExceptions, reportExecutionTime, methodVisitorFactory == null ? s_defaultMethodVisitorFactory : methodVisitorFactory);
    }

    public void addAllMethods(boolean reportCaughtExceptions, boolean reportExecutionTime) {
        addAllMethods(reportCaughtExceptions, reportExecutionTime, null);
    }

    public String getClassName() {
        return className;
    }

    public String getClassType() {
        return classType;
    }

    public MethodInstrumentationInfo getMethodInstrumentationInfo() {
        return methodInstrumentationInfo;
    }

    public void setMethodInstrumentationInfo(MethodInstrumentationInfo methodInstrumentationInfo) {
        this.methodInstrumentationInfo = methodInstrumentationInfo;
    }

    public boolean isReportExecutionTime() {
        return reportExecutionTime;
    }

    public boolean isReportCaughtExceptions() {
        return reportCaughtExceptions;
    }

    public long getThresholdInMS() {
        return thresholdInMS;
    }

    public ClassVisitor getDefaultClassInstrumentor(ClassWriter classWriter) {
        return classVisitorFactory.create(this, classWriter);
    }

    public MethodVisitor getMethodVisitor(int access,
                                          String methodName,
                                          String methodSignature,
                                          MethodVisitor originalMV) {
        return getMethodVisitor(access, methodName, methodSignature, originalMV, null);
    }

    public MethodVisitor getMethodVisitor(int access,
                                          String methodName,
                                          String methodSignature,
                                          MethodVisitor originalMV,
                                          ClassToMethodTransformationData additionalData) {
        MethodInstrumentationDecision decision = methodInstrumentationInfo.getDecision(methodName, methodSignature);
        if (decision == null || decision.getMethodVisitorFactory() == null) {
            return originalMV;
        }

        return decision.getMethodVisitorFactory().create(decision, access, methodSignature, getClassName(), methodName, originalMV, additionalData);
    }

    public ClassInstrumentationData setThresholdInMS(long thresholdInMS) {
        this.thresholdInMS = thresholdInMS;
        return this;
    }

    public boolean isRegExp() {
        return classNamePattern != null;
    }

    public String getFullPackageName() {
        return onlyPackageName;
    }

    public boolean isClassNameMatches(String onlycClassName) {
        Matcher matcher = classNamePattern.matcher(onlycClassName);
        return matcher.matches();
    }
}

