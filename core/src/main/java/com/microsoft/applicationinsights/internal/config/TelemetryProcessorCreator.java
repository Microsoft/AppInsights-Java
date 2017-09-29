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

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * The class will try to create the {@link TelemetryProcessor}
 * and will activate any 'setXXX' method based on the configuration
 *
 * Any exception thrown, or any setter method returns false will cause the processor to be ignored.
 *
 * Created by gupele on 8/7/2016.
 */
public final class TelemetryProcessorCreator {

    public TelemetryProcessor Create(TelemetryProcessorXmlElement confClass) {
        if (confClass == null) {
            return null;
        }

        TelemetryProcessor processor = null;

        processor = ReflectionUtils.createInstance(confClass.getType(), TelemetryProcessor.class);
        if (processor == null) {
            return null;
        }

        try {
            for (ParamXmlElement param : confClass.getAdds()){
                String methodName = "set" + param.getName();
                try {
                    if (!ReflectionUtils.activateMethod(processor, methodName, param.getValue(), String.class)) {
                        InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, confClass.getType() + ": method " + methodName + "failed, the class will not be used.");
                        return null;
                    }
                } catch (ThreadDeath td) {
                	throw td;
                } catch (Throwable t) {
                    InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, confClass.getType() + ": failed to activate method " + methodName + ", exception: " + t.getMessage() + ", the class will not be used.");
                    return null;
                }
            }
        } catch (Throwable throwable) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, confClass.getType() + ": unexpected exception while creating processor " + confClass.getType() + ", exception: " + throwable.getMessage() + ", the class will not be used.");
            return null;
        }

        return processor;
    }
}
