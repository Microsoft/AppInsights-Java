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

package com.microsoft.applicationinsights.extensibility.context;

/** ContextTagKeys */
@SuppressWarnings({"MemberName", "AbbreviationAsWordInName"})
public class ContextTagKeys {
  //
  // Fields
  //
  // 1: Optional string ApplicationId
  private String ApplicationId;

  // 2: Optional string ApplicationVersion
  private String ApplicationVersion;

  // 3: Optional string ApplicationTypeId
  private String ApplicationTypeId;

  // 11: Optional string DeviceId
  private String DeviceId;

  // 12: Optional string DeviceOS
  private String DeviceOS;

  // 13: Optional string DeviceOSVersion
  private String DeviceOSVersion;

  // 14: Optional string DeviceLocale
  private String DeviceLocale;

  // 15: Optional string DeviceType
  private String DeviceType;

  // 16: Optional string DeviceVMName
  private String DeviceVMName;

  // 17: Optional string DeviceRoleName
  @Deprecated private String DeviceRoleName;

  // 18: Optional string DeviceRoleInstance
  @Deprecated private String DeviceRoleInstance;

  // 19: Optional string DeviceOEMName
  private String DeviceOEMName;

  // 20: Optional string DeviceModel
  private String DeviceModel;

  // 21: Optional string DeviceNetwork
  private String DeviceNetwork;

  // 22: Optional string DeviceScreenResolution
  private String DeviceScreenResolution;

  // 23: Optional string DeviceLanguage
  private String DeviceLanguage;

  // 24: Optional string DeviceIp
  private String DeviceIp;

  // 31: Optional string LocationLatitude
  private String LocationLatitude;

  // 32: Optional string LocationLongitude
  private String LocationLongitude;

  // 33: Optional string LocationIP
  private String LocationIP;

  // 34: Optional string LocationContinent
  private String LocationContinent;

  // 35: Optional string LocationCountry
  private String LocationCountry;

  // 36: Optional string LocationProvince
  private String LocationProvince;

  // 37: Optional string LocationCity
  private String LocationCity;

  // 41: Optional string OperationName
  private String OperationName;

  // 42: Optional string OperationId
  private String OperationId;

  // 43: Optional string OperationParentId
  private String OperationParentId;

  // 44: Optional string OperationRootId
  private String OperationRootId;

  private String OperationCorrelationVector;

  // 51: Optional string SessionId
  private String SessionId;

  // 52: Optional string SessionIsFirst
  private String SessionIsFirst;

  // 53: Optional string SessionIsNew
  private String SessionIsNew;

  // 61: Optional string UserType
  private String UserType;

  // 62: Optional string UserId
  private String UserId;

  // 63: Optional string UserAuthUserId
  private String UserAuthUserId;

  // 64: Optional string UserAccountId
  private String UserAccountId;

  // 65: Optional string UserAnonymousUserAcquisitionDate
  private String UserAnonymousUserAcquisitionDate;

  // 66: Optional string UserAuthenticatedUserAcquisitionDate
  private String UserAuthenticatedUserAcquisitionDate;

  // 67: Optional string UserAccountAcquisitionDate
  private String UserAccountAcquisitionDate;

  // 68: Optional string UserAgent
  private String UserAgent;

  // 71: Optional string SampleRate
  private String SampleRate;

  private String SyntheticSource;

  // 1000: Optional string InternalSdkVersion
  private String InternalSdkVersion;

  // 1001: Optional string InternalAgentVersion
  private String InternalAgentVersion;

  // 1002: Optional string InternalNodeName
  private String InternalNodeName;

  private String CloudRole;

  private String CloudRoleInstance;

  /** @return current value of ApplicationId property */
  public final String getApplicationId() {
    return this.ApplicationId;
  }

  /** @param value new value of ApplicationId property */
  public final void setApplicationId(String value) {
    this.ApplicationId = value;
  }

  /** @return current value of ApplicationVersion property */
  public final String getApplicationVersion() {
    return this.ApplicationVersion;
  }

  /** @param value new value of ApplicationVersion property */
  public final void setApplicationVersion(String value) {
    this.ApplicationVersion = value;
  }

  /** @return current value of ApplicationTypeId property */
  public final String getApplicationTypeId() {
    return this.ApplicationTypeId;
  }

  /** @param value new value of ApplicationTypeId property */
  public final void setApplicationTypeId(String value) {
    this.ApplicationTypeId = value;
  }

  /** @return current value of DeviceId property */
  public final String getDeviceId() {
    return this.DeviceId;
  }

  /** @param value new value of DeviceId property */
  public final void setDeviceId(String value) {
    this.DeviceId = value;
  }

  /** @return current value of DeviceOS property */
  public final String getDeviceOS() {
    return this.DeviceOS;
  }

  /** @param value new value of DeviceOS property */
  public final void setDeviceOS(String value) {
    this.DeviceOS = value;
  }

  /** @return current value of DeviceOSVersion property */
  public final String getDeviceOSVersion() {
    return this.DeviceOSVersion;
  }

  /** @param value new value of DeviceOSVersion property */
  public final void setDeviceOSVersion(String value) {
    this.DeviceOSVersion = value;
  }

  /** @return current value of DeviceLocale property */
  public final String getDeviceLocale() {
    return this.DeviceLocale;
  }

  /** @param value new value of DeviceLocale property */
  public final void setDeviceLocale(String value) {
    this.DeviceLocale = value;
  }

  /** @return current value of DeviceType property */
  public final String getDeviceType() {
    return this.DeviceType;
  }

  /** @param value new value of DeviceType property */
  public final void setDeviceType(String value) {
    this.DeviceType = value;
  }

  /** @return current value of DeviceVMName property */
  public final String getDeviceVMName() {
    return this.DeviceVMName;
  }

  /** @param value new value of DeviceVMName property */
  public final void setDeviceVMName(String value) {
    this.DeviceVMName = value;
  }

  /**
   * @return current value of DeviceRoleName property
   * @deprecated use {@link #getCloudRole()}
   */
  @Deprecated
  public final String getDeviceRoleName() {
    return this.DeviceRoleName;
  }

  /** @param value new value of DeviceRoleName property */
  @Deprecated
  public final void setDeviceRoleName(String value) {
    this.DeviceRoleName = value;
  }

  /**
   * @return current value of DeviceRoleInstance property
   * @deprecated use {@link #getCloudRoleInstance()}
   */
  @Deprecated
  public final String getDeviceRoleInstance() {
    return this.DeviceRoleInstance;
  }

  /** @param value new value of DeviceRoleInstance property */
  @Deprecated
  public final void setDeviceRoleInstance(String value) {
    this.DeviceRoleInstance = value;
  }

  /** @return current value of DeviceOEMName property */
  public final String getDeviceOEMName() {
    return this.DeviceOEMName;
  }

  /** @param value new value of DeviceOEMName property */
  public final void setDeviceOEMName(String value) {
    this.DeviceOEMName = value;
  }

  /** @return current value of DeviceModel property */
  public final String getDeviceModel() {
    return this.DeviceModel;
  }

  /** @param value new value of DeviceModel property */
  public final void setDeviceModel(String value) {
    this.DeviceModel = value;
  }

  /** @return current value of DeviceNetwork property */
  public final String getDeviceNetwork() {
    return this.DeviceNetwork;
  }

  /** @param value new value of DeviceNetwork property */
  public final void setDeviceNetwork(String value) {
    this.DeviceNetwork = value;
  }

  /** @return current value of DeviceScreenResolution property */
  public final String getDeviceScreenResolution() {
    return this.DeviceScreenResolution;
  }

  /** @param value new value of DeviceScreenResolution property */
  public final void setDeviceScreenResolution(String value) {
    this.DeviceScreenResolution = value;
  }

  /** @return current value of DeviceLanguage property */
  public final String getDeviceLanguage() {
    return this.DeviceLanguage;
  }

  /** @param value new value of DeviceLanguage property */
  public final void setDeviceLanguage(String value) {
    this.DeviceLanguage = value;
  }

  /** @return current value of DeviceIp property */
  public final String getDeviceIp() {
    return this.DeviceIp;
  }

  /** @param value new value of DeviceIp property */
  public final void setDeviceIp(String value) {
    this.DeviceIp = value;
  }

  /** @return current value of LocationLatitude property */
  public final String getLocationLatitude() {
    return this.LocationLatitude;
  }

  /** @param value new value of LocationLatitude property */
  public final void setLocationLatitude(String value) {
    this.LocationLatitude = value;
  }

  /** @return current value of LocationLongitude property */
  public final String getLocationLongitude() {
    return this.LocationLongitude;
  }

  /** @param value new value of LocationLongitude property */
  public final void setLocationLongitude(String value) {
    this.LocationLongitude = value;
  }

  /** @return current value of LocationIP property */
  public final String getLocationIP() {
    return this.LocationIP;
  }

  /** @param value new value of LocationIP property */
  public final void setLocationIP(String value) {
    this.LocationIP = value;
  }

  /** @return current value of LocationContinent property */
  public final String getLocationContinent() {
    return this.LocationContinent;
  }

  /** @param value new value of LocationContinent property */
  public final void setLocationContinent(String value) {
    this.LocationContinent = value;
  }

  /** @return current value of LocationCountry property */
  public final String getLocationCountry() {
    return this.LocationCountry;
  }

  /** @param value new value of LocationCountry property */
  public final void setLocationCountry(String value) {
    this.LocationCountry = value;
  }

  /** @return current value of LocationProvince property */
  public final String getLocationProvince() {
    return this.LocationProvince;
  }

  /** @param value new value of LocationProvince property */
  public final void setLocationProvince(String value) {
    this.LocationProvince = value;
  }

  /** @return current value of LocationCity property */
  public final String getLocationCity() {
    return this.LocationCity;
  }

  /** @param value new value of LocationCity property */
  public final void setLocationCity(String value) {
    this.LocationCity = value;
  }

  /** @return current value of OperationName property */
  public final String getOperationName() {
    return this.OperationName;
  }

  /** @param value new value of OperationName property */
  public final void setOperationName(String value) {
    this.OperationName = value;
  }

  /** @return current value of OperationId property */
  public final String getOperationId() {
    return this.OperationId;
  }

  /** @param value new value of OperationId property */
  public final void setOperationId(String value) {
    this.OperationId = value;
  }

  /** @return current value of SyntheticSource property */
  public final String getSyntheticSource() {
    return this.SyntheticSource;
  }

  /** @param value new value of SyntheticSource property */
  public final void setSyntheticSource(String value) {
    this.SyntheticSource = value;
  }

  /** @return current value of OperationParentId property */
  public final String getOperationParentId() {
    return this.OperationParentId;
  }

  /** @param value new value of OperationParentId property */
  public final void setOperationParentId(String value) {
    this.OperationParentId = value;
  }

  /** @return current value of OperationRootId property */
  public final String getOperationRootId() {
    return this.OperationRootId;
  }

  /** @param value new value of OperationRootId property */
  public final void setOperationRootId(String value) {
    this.OperationRootId = value;
  }

  /** @return current value of SessionId property */
  public final String getSessionId() {
    return this.SessionId;
  }

  /** @param value new value of SessionId property */
  public final void setSessionId(String value) {
    this.SessionId = value;
  }

  /** @return current value of SessionIsFirst property */
  public final String getSessionIsFirst() {
    return this.SessionIsFirst;
  }

  /** @param value new value of SessionIsFirst property */
  public final void setSessionIsFirst(String value) {
    this.SessionIsFirst = value;
  }

  /** @return current value of SessionIsNew property */
  public final String getSessionIsNew() {
    return this.SessionIsNew;
  }

  /** @param value new value of SessionIsNew property */
  public final void setSessionIsNew(String value) {
    this.SessionIsNew = value;
  }

  /** @return current value of UserType property */
  public final String getUserType() {
    return this.UserType;
  }

  /** @param value new value of UserType property */
  public final void setUserType(String value) {
    this.UserType = value;
  }

  /** @return current value of UserId property */
  public final String getUserId() {
    return this.UserId;
  }

  /** @param value new value of UserId property */
  public final void setUserId(String value) {
    this.UserId = value;
  }

  /** @return current value of UserAuthUserId property */
  public final String getUserAuthUserId() {
    return this.UserAuthUserId;
  }

  /** @param value new value of UserAuthUserId property */
  public final void setUserAuthUserId(String value) {
    this.UserAuthUserId = value;
  }

  /** @return current value of UserAccountId property */
  public final String getUserAccountId() {
    return this.UserAccountId;
  }

  /** @param value new value of UserAccountId property */
  public final void setUserAccountId(String value) {
    this.UserAccountId = value;
  }

  /** @return current value of UserAnonymousUserAcquisitionDate property */
  public final String getUserAnonymousUserAcquisitionDate() {
    return this.UserAnonymousUserAcquisitionDate;
  }

  /** @param value new value of UserAnonymousUserAcquisitionDate property */
  public final void setUserAnonymousUserAcquisitionDate(String value) {
    this.UserAnonymousUserAcquisitionDate = value;
  }

  /** @return current value of UserAuthenticatedUserAcquisitionDate property */
  public final String getUserAuthenticatedUserAcquisitionDate() {
    return this.UserAuthenticatedUserAcquisitionDate;
  }

  /** @param value new value of UserAuthenticatedUserAcquisitionDate property */
  public final void setUserAuthenticatedUserAcquisitionDate(String value) {
    this.UserAuthenticatedUserAcquisitionDate = value;
  }

  /** @return current value of UserAccountAcquisitionDate property */
  public final String getUserAccountAcquisitionDate() {
    return this.UserAccountAcquisitionDate;
  }

  /** @param value new value of UserAccountAcquisitionDate property */
  public final void setUserAccountAcquisitionDate(String value) {
    this.UserAccountAcquisitionDate = value;
  }

  /** @return current value of UserAgent property */
  public final String getUserAgent() {
    return this.UserAgent;
  }

  /** @param value new value of UserAgent property */
  public final void setUserAgent(String value) {
    this.UserAgent = value;
  }

  /** @return current value of SampleRate property */
  public final String getSampleRate() {
    return this.SampleRate;
  }

  /** @param value new value of SampleRate property */
  public final void setSampleRate(String value) {
    this.SampleRate = value;
  }

  /** @return current value of InternalSdkVersion property */
  public final String getInternalSdkVersion() {
    return this.InternalSdkVersion;
  }

  /** @param value new value of InternalSdkVersion property */
  public final void setInternalSdkVersion(String value) {
    this.InternalSdkVersion = value;
  }

  /** @return current value of InternalAgentVersion property */
  public final String getInternalAgentVersion() {
    return this.InternalAgentVersion;
  }

  /** @param value new value of InternalAgentVersion property */
  public final void setInternalAgentVersion(String value) {
    this.InternalAgentVersion = value;
  }

  /**
   * The node name used for billing purposes. Use it to override the standard detection of nodes.
   *
   * @return current value of InternalNodeNName
   */
  public final String getInternalNodeName() {
    return this.InternalNodeName;
  }

  /**
   * The node name used for billing purposes. Use it to override the standard detection of nodes.
   *
   * @param value new value of InternalNodeName
   */
  public final void setInternalNodeName(String value) {
    this.InternalNodeName = value;
  }

  public final String getCloudRole() {
    return this.CloudRole;
  }

  public final String getCloudRoleInstance() {
    return this.CloudRoleInstance;
  }

  public final String getOperationCorrelationVector() {
    return OperationCorrelationVector;
  }

  public final void setOperationCorrelationVector(String value) {
    OperationCorrelationVector = value;
  }

  public static ContextTagKeys getKeys() {
    return s_keys;
  }

  static {
    s_keys = new ContextTagKeys();
  }

  private static final ContextTagKeys s_keys;

  // Constructor
  public ContextTagKeys() {
    reset();
  }

  /*
   * As describe: com.microsoft.bond.BondSerializable#reset()
   */
  public void reset() {
    reset(
        "ContextTagKeys", "com.microsoft.applicationinsights.extensibility.context.ContextTagKeys");
  }

  protected void reset(String name, String qualifiedName) {
    ApplicationId = "ai.application.id";
    ApplicationVersion = "ai.application.ver";
    ApplicationTypeId = "ai.application.typeId";
    DeviceId = "ai.device.id";
    DeviceOS = "ai.device.os";
    DeviceOSVersion = "ai.device.osVersion";
    DeviceLocale = "ai.device.locale";
    DeviceType = "ai.device.type";
    DeviceVMName = "ai.device.vmName";
    DeviceRoleName = "ai.device.roleName";
    DeviceRoleInstance = "ai.device.roleInstance";
    DeviceOEMName = "ai.device.oemName";
    DeviceModel = "ai.device.model";
    DeviceNetwork = "ai.device.network";
    DeviceScreenResolution = "ai.device.screenResolution";
    DeviceLanguage = "ai.device.language";
    DeviceIp = "ai.device.ip";
    LocationLatitude = "ai.location.latitude";
    LocationLongitude = "ai.location.longitude";
    LocationIP = "ai.location.ip";
    LocationContinent = "ai.location.continent";
    LocationCountry = "ai.location.country";
    LocationProvince = "ai.location.province";
    LocationCity = "ai.location.city";
    OperationName = "ai.operation.name";
    OperationId = "ai.operation.id";
    OperationParentId = "ai.operation.parentId";
    OperationRootId = "ai.operation.rootId";
    OperationCorrelationVector = "ai.operation.correlationVector";
    SessionId = "ai.session.id";
    SessionIsFirst = "ai.session.isFirst";
    SessionIsNew = "ai.session.isNew";
    UserType = "ai.user.type";
    UserId = "ai.user.id";
    UserAuthUserId = "ai.user.authUserId";
    UserAccountId = "ai.user.accountId";
    UserAnonymousUserAcquisitionDate = "ai.user.anonUserAcquisitionDate";
    UserAuthenticatedUserAcquisitionDate = "ai.user.authUserAcquisitionDate";
    UserAccountAcquisitionDate = "ai.user.accountAcquisitionDate";
    UserAgent = "ai.user.userAgent";
    SampleRate = "ai.sample.sampleRate";
    InternalSdkVersion = "ai.internal.sdkVersion";
    InternalAgentVersion = "ai.internal.agentVersion";
    SyntheticSource = "ai.operation.syntheticSource";
    InternalNodeName = "ai.internal.nodeName";
    CloudRole = "ai.cloud.role";
    CloudRoleInstance = "ai.cloud.roleInstance";
  }
} // class ContextTagKeys
