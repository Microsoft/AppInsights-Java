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

package com.microsoft.applicationinsights.web.internal.cookies;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.Cookie;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.DateTimeUtils;
import com.microsoft.applicationinsights.web.utils.HttpHelper;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by yonisha on 2/5/2015.
 */
public class SessionCookieTests {

    // region Members

    private static Cookie defaultCookie;
    private static String sessionId;
    private static Date sessionAcquisitionTime;
    private static Date sessionRenewalTime;
    private static SessionContext sessionContext;
    private static RequestTelemetryContext requestTelemetryContextMock;

    // endregion Members

    @BeforeClass
    public static void initialize() throws Exception {
        sessionId = LocalStringsUtils.generateRandomId(true);
        sessionAcquisitionTime = new Date();
        sessionRenewalTime = new Date(sessionAcquisitionTime.getTime() + 1000);

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionId,
                String.valueOf(sessionAcquisitionTime.getTime()),
                String.valueOf(sessionRenewalTime.getTime())
        });

        defaultCookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

        sessionContext = new SessionContext(new ConcurrentHashMap<String, String>());
        sessionContext.setId(sessionId);

        SessionCookie sessionCookie = new SessionCookie(defaultCookie);
        requestTelemetryContextMock = mock(RequestTelemetryContext.class);
        when(requestTelemetryContextMock.getSessionCookie()).thenReturn(sessionCookie);
    }

    // region Tests
    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void testCookieParsedSuccessfully() throws Exception {
        SessionCookie sessionCookie = new SessionCookie(defaultCookie);

        Assert.assertEquals("Wrong session ID", sessionId, sessionCookie.getSessionId());
        Assert.assertEquals("Wrong session acquisition time", sessionAcquisitionTime, sessionCookie.getSessionAcquisitionDate());
        Assert.assertEquals("Wrong session renewal time", sessionRenewalTime, sessionCookie.getSessionRenewalDate());
    }

    @Test
    public void testCookieExpiration() throws Exception {
        String expiredFormattedCookie = HttpHelper.getFormattedSessionCookie(true);
        Cookie expiredCookie = new Cookie(SessionCookie.COOKIE_NAME, expiredFormattedCookie);
        SessionCookie sessionCookie = new SessionCookie(expiredCookie);

        Assert.assertTrue("Expired session expected.", sessionCookie.isSessionExpired(SessionCookie.SESSION_DEFAULT_EXPIRATION_TIMEOUT_IN_MINUTES));
    }

    @Test
    public void testCorruptedSessionAcquisitionTimeValueThrowsExceptionOnCookieParsing() throws Exception {
        thrown.expect(Exception.class);

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionId,
                "corruptedAcquisitionTime",
                String.valueOf(sessionRenewalTime.getTime())
        });

        createSessionCookie(formattedCookie);
    }

    @Test
    public void testCorruptedSessionRenewalTimeValueThrowsExceptionOnCookieParsing() throws Exception {
        thrown.expect(Exception.class);

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionId,
                String.valueOf(sessionAcquisitionTime.getTime()),
                "corruptedValue"
        });

        createSessionCookie(formattedCookie);
    }

    @Test
    public void testUnexpectedCookieValuesCountThrowsException() throws Exception {
        thrown.expect(Exception.class);

        String formattedCookie = SessionCookie.formatCookie(new String[]{
                "singleValueCookie"
        });

        createSessionCookie(formattedCookie);
    }

    @Test
    public void testSessionHttpCookiePathSetForAllPages() {
        Cookie cookie = HttpCookieFactory.generateSessionHttpCookie(requestTelemetryContextMock, sessionContext, 10);

        Assert.assertEquals("Path should catch all urls", HttpCookieFactory.COOKIE_PATH_ALL_URL, cookie.getPath());
    }

    @Test
    public void testSessionHttpCookieSetMaxAge() {
        final int sessionTimeoutInMinutes = 10;

        Cookie cookie = HttpCookieFactory.generateSessionHttpCookie(requestTelemetryContextMock, sessionContext, sessionTimeoutInMinutes);

        Assert.assertEquals(sessionTimeoutInMinutes * 60, cookie.getMaxAge());
    }

    @Test
    public void testCookieParsedSuccessfullyWithBackwardCompatibilityForTimeFormats() throws Exception {
        String expectedAcquisitionTime = DateTimeUtils.formatAsRoundTripDate(sessionAcquisitionTime);
        String expectedRenewalTime = DateTimeUtils.formatAsRoundTripDate(sessionRenewalTime);

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionId,
                expectedAcquisitionTime,
                expectedRenewalTime
        });

        Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

        SessionCookie sessionCookie = new SessionCookie(cookie);

        Assert.assertEquals("Wrong session acquisition time", expectedAcquisitionTime, DateTimeUtils.formatAsRoundTripDate(sessionCookie.getSessionAcquisitionDate()));
        Assert.assertEquals("Wrong session renewal time", expectedRenewalTime, DateTimeUtils.formatAsRoundTripDate(sessionCookie.getSessionRenewalDate()));
    }


    @Test
    public void testCookieParsedSuccessfullyWithMiliseconds() throws Exception {
        Date expectedAcquisitionTime = new Date();
        Date expectedRenewalTime = new Date();

        String formattedCookie = SessionCookie.formatCookie(new String[] {
                sessionId,
                expectedAcquisitionTime.getTime() + ".10",
                expectedRenewalTime.getTime() + ".99"
        });

        Cookie cookie = new Cookie(SessionCookie.COOKIE_NAME, formattedCookie);

        SessionCookie sessionCookie = new SessionCookie(cookie);

        Assert.assertEquals("Wrong session acquisition time", expectedAcquisitionTime, sessionCookie.getSessionAcquisitionDate());
        Assert.assertEquals("Wrong session renewal time", expectedRenewalTime, sessionCookie.getSessionRenewalDate());
    }

    // endregion Tests

    // region Private

    private void createSessionCookie(String cookieValue) throws Exception {
        Cookie corruptedCookie = new Cookie(SessionCookie.COOKIE_NAME, cookieValue);
        new SessionCookie(corruptedCookie);
    }

    // endregion Private
}
