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

package com.microsoft.applicationinsights.management.rest.operations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.applicationinsights.management.common.Logger;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.client.Client;
import com.microsoft.applicationinsights.management.rest.model.Tenant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO: implement

/**
 * Created by yonisha on 4/28/2015.
 *
 * The operation retrieves all available locations for resources of type 'component', under microsoft.insights provider.
 */
public class GetAvailableGeoLocations implements RestOperation<List<String>> {

    private static final Logger LOG = Logger.getLogger(GetAvailableGeoLocations.class.toString());
    private final String OPERATION_API_VERSION = "2015-01-01";
    private final String OPERATION_PATH_TEMPLATE = "providers/microsoft.insights?api-version=%s";
    private Tenant commonTenant;
    private String operationPath;

    public GetAvailableGeoLocations(Tenant commonTenant) {
        this.operationPath = String.format(OPERATION_PATH_TEMPLATE, OPERATION_API_VERSION);
        this.commonTenant = commonTenant;
    }

    @Override
    public List<String> execute(Client restClient) throws IOException, RestOperationException {
        LOG.info("Getting available geo-locations.\nURL Path: {0}.", this.operationPath);

        String resourceJson = restClient.executeGet(commonTenant, operationPath, OPERATION_API_VERSION);
        List<String> locations = parseResult(resourceJson);

        return locations;
    }

    private List<String> parseResult(String resultJson) {
        List<String> locations = new ArrayList<String>();

        JsonObject json = new JsonParser().parse(resultJson).getAsJsonObject();
        JsonArray jsonResourceTypes = json.getAsJsonArray("resourceTypes");

        for (int i = 0; i < jsonResourceTypes.size(); i++) {
            Object obj = jsonResourceTypes.get(i);
            if (obj instanceof JsonObject) {
                JsonObject jsonResourceType = (JsonObject) obj ;

                String resourceType = jsonResourceType.get("resourceType").getAsString();

                if (!resourceType.equalsIgnoreCase("components")) {
                    continue;
                }

                JsonArray jsonLocations = jsonResourceType.getAsJsonArray("locations");

                for (Object location : jsonLocations) {
                    if (location instanceof JsonElement && !((JsonElement)location).getAsString().equalsIgnoreCase("")) {
                        JsonElement jsonLocation = (JsonElement) location;
                        locations.add(jsonLocation.getAsString());
                    }
                }
            }
        }

        return locations;
    }
}
