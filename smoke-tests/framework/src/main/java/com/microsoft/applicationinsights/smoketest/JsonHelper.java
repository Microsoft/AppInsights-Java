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

package com.microsoft.applicationinsights.smoketest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.applicationinsights.smoketest.schemav2.Base;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Domain;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.PageViewData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.telemetry.Duration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JsonHelper {
  public static final Gson GSON =
      new GsonBuilder()
          .serializeNulls()
          .registerTypeHierarchyAdapter(Base.class, new BaseDataContractDeserializer())
          .registerTypeAdapter(TypeToken.get(Duration.class).getType(), new DurationDeserializer())
          .create();

  private static class BaseDataContractDeserializer implements JsonDeserializer<Base> {
    private static final String discriminatorField = "baseType";

    private final Map<String, Class<? extends Domain>> classMap;

    public BaseDataContractDeserializer() {
      classMap = new HashMap<>();
      Class<? extends Domain>[] classes =
          new Class[] {
            RequestData.class,
            EventData.class,
            ExceptionData.class,
            MessageData.class,
            MetricData.class,
            RemoteDependencyData.class,
            PageViewData.class
          };

      for (Class<? extends Domain> clazz : classes) {
        classMap.put(clazz.getSimpleName(), clazz);
      }
    }

    @Override
    @SuppressWarnings("SystemOut")
    public Base deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      JsonObject jo = json.getAsJsonObject();
      String baseType = jo.get(discriminatorField).getAsString();
      try {
        Data<Domain> rval = Data.class.getDeclaredConstructor().newInstance();
        JsonObject baseData = jo.get("baseData").getAsJsonObject();
        Class<? extends Domain> domainClass = classMap.get(baseType);
        if (domainClass == null) {
          throw new JsonParseException("Unknown Domain type: " + baseType);
        }
        rval.setBaseType(baseType);
        Domain deserialize = context.deserialize(baseData, TypeToken.get(domainClass).getType());
        rval.setBaseData(deserialize);
        return rval;
      } catch (InstantiationException
          | IllegalAccessException
          | NoSuchMethodException
          | InvocationTargetException e) {
        System.err.println("Error deserializing data");
        e.printStackTrace();
        throw new JsonParseException(e);
      }
    }
  }

  private static class DurationDeserializer implements JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      String value = json.getAsString();
      int firstDot = value.indexOf('.');
      boolean hasDays = firstDot > -1 && firstDot < value.indexOf(':');

      String[] parts = value.split("[:.]"); // [days.]hours:minutes:seconds[.milliseconds]

      long[] conversionFactor = new long[] {86400000, 3600000, 60000, 1000, 1};
      int conversionIndex = hasDays ? 0 : 1;
      long duration = 0;
      for (int i = 0; i < parts.length; i++) {
        String part =
            (conversionIndex == conversionFactor.length - 1) ? parts[i].substring(0, 3) : parts[i];
        duration += Long.parseLong(part) * conversionFactor[conversionIndex++];
      }

      return new Duration(duration);
    }
  }

  private JsonHelper() {}
}
