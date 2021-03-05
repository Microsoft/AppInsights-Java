/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.instrumentation.api.aisdk.AiConnectionString;
import io.opentelemetry.instrumentation.api.aisdk.AiWebsiteSiteName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AzureFunctionsInstrumentationHelperTest {

  /*
   * Lazily Set Connection String For Linux Consumption Plan:
   *
   *    Term      LazySetOptIn   ConnectionString      EnableAgent        LazySet
   *    Preview   FALSE          VALID                 TRUE               Enabled
   *                             VALID                 FALSE              Disabled
   *                             VALID                 NULL               Disabled
   *                             NULL                  TRUE/FALSE/NULL    Disabled
   *    GA        TRUE           VALID                 TRUE               Enabled
   *                             VALID                 FALSE              Disabled
   *                             VALID                 NULL               Enabled
   *                             NULL                  TRUE/FALSE/NULL    Disabled
   */
  private static final String CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint=http://fakeingestion:60606/";

  private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";
  private static final String WEBSITE_SITE_NAME = "fake_site_name";

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOffEnableAgentOn() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "true"));
  }

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is FALSE")
  public void disableLazySetWithLazySetOptInOffEnableAgentOff() {
    assertFalse(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "false"));
  }

  @Test
  @DisplayName("LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is NULL")
  public void disableLazySetWithLazySetOptInOffEnableAgentNull() {
    assertFalse(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, null));
  }

  @Test
  @DisplayName(
      "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is TRUE")
  public void disableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, null);
    assertNull(accessor.connectionString);
  }

  @Test
  @DisplayName(
      "LazySetOptIn is FALSE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent is TRUE")
  public void disableLazySetWithLazySetOptInOffConnectionStringNotNullInstrumentationKeyNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(CONNECTION_STRING, null);
    assertEquals(accessor.connectionString, CONNECTION_STRING);

    MockedWebsiteSiteNameAccessor websiteSiteNameAccessor = new MockedWebsiteSiteNameAccessor();
    AiWebsiteSiteName.setAccessor(websiteSiteNameAccessor);
    AzureFunctionsInstrumentationHelper.setWebsiteSiteName(WEBSITE_SITE_NAME);
    assertEquals(websiteSiteNameAccessor.websiteSiteName, WEBSITE_SITE_NAME);
  }

  @Test
  @DisplayName(
      "LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNotNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, INSTRUMENTATION_KEY);
    assertEquals(accessor.connectionString, "InstrumentationKey=" + INSTRUMENTATION_KEY);
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOnEnableAgentOn() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(true, "true"));
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is FALSE")
  public void disableLazySetWithLazySetOptInOnEnableAgentOff() {
    assertFalse(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(true, "false"));
  }

  @Test
  @DisplayName("LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is NULL")
  public void enableLazySetWithLazySetOptInOnEnableAgentNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(true, null));
  }

  @Test
  @DisplayName(
      "LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is TRUE")
  public void disableLazySetWithLazySetOptInOnConnectionStringNullAndInstrumentationKeyNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(true, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, null);
    assertNull(accessor.connectionString);
  }

  @Test
  @DisplayName(
      "LazySetOptIn is TRUE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOnConnectionStringNotNullInstrumentationKeyNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(CONNECTION_STRING, null);
    assertEquals(accessor.connectionString, CONNECTION_STRING);
  }

  @Test
  @DisplayName(
      "LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent is TRUE")
  public void enableLazySetWithLazySetOptInOnConnectionStringNullInstrumentationKeyNotNull() {
    assertTrue(AzureFunctionsInstrumentationHelper.shouldUpdateConnectionString(false, "true"));
    MockedAccessor accessor = new MockedAccessor();
    AiConnectionString.setAccessor(accessor);
    AzureFunctionsInstrumentationHelper.setConnectionString(null, INSTRUMENTATION_KEY);
    assertEquals(accessor.connectionString, "InstrumentationKey=" + INSTRUMENTATION_KEY);
  }

  private class MockedAccessor implements AiConnectionString.Accessor {
    private String connectionString;

    @Override
    public boolean hasValue() {
      return connectionString != null;
    }

    @Override
    public void setValue(String value) {
      connectionString = value;
    }
  }

  private class MockedWebsiteSiteNameAccessor implements AiWebsiteSiteName.Accessor {
    private String websiteSiteName;

    @Override
    public boolean hasValue() {
      return websiteSiteName != null && !websiteSiteName.isEmpty();
    }

    @Override
    public void setValue(String value) {
      websiteSiteName = value;
    }
  }
}
