/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.registry.es.store.review;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wink.client.ClientResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.registry.es.store.search.StoreSearchHistoryTestCase;
import org.wso2.carbon.registry.es.utils.ESTestBaseTest;
import org.wso2.es.integration.common.utils.GenericRestClient;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class tests the functionality of adding review and updating review for an asset.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class StoreReviewTestCase extends ESTestBaseTest {
    protected Log log = LogFactory.getLog(StoreSearchHistoryTestCase.class);
    private TestUserMode userMode;
    private String storeUrl;
    private String cookieHeader;
    private GenericRestClient genericRestClient;
    private Map<String, String> queryParamMap;
    private Map<String, String> headerMap;
    private final String USER_REVIEW_UPDATE_URL = "/user-review/1";

    @Factory(dataProvider = "userModeProvider")
    public StoreReviewTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(userMode);
        genericRestClient = new GenericRestClient();
        headerMap = new HashMap<>();
        queryParamMap = new HashMap<>();
        storeUrl = storeContext.getContextUrls().getSecureServiceUrl().replace("services", "store/apis");
        setTestEnvironment(true);
    }

    /**
     * To set up a test environment.
     *
     * @throws XPathExpressionException XpathExpressionException will be thrown while getting tenant
     * @throws JSONException            JSONException will be thrown during Json Manipulations.
     */
    private void setTestEnvironment(boolean isCurrentAdmin) throws XPathExpressionException, JSONException {
        User user = automationContext.getContextTenant().getTenantAdmin();

        if (!isCurrentAdmin) {
            String storeUserKey = "subscribeUser";
            user = automationContext.getContextTenant().getTenantUser(storeUserKey);
        }
        JSONObject objSessionStore = new JSONObject(
                authenticate(storeUrl, genericRestClient, user.getUserName(), user.getPassword())
                        .getEntity(String.class));
        String jSessionId = objSessionStore.getJSONObject("data").getString("sessionId");
        cookieHeader = "JSESSIONID=" + jSessionId;
        assertNotNull(jSessionId, "Invalid JSession ID received");
    }

    @Test(groups = { "wso2.greg", "wso2.greg.es" }, description = "Test add review for an asset")
    public void testAddReview() throws JSONException, XPathExpressionException {
        ClientResponse response = genericRestClient
                .geneticRestRequestPost(storeUrl + "/user-reviews", MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, createReviewPayload(false), queryParamMap, headerMap, cookieHeader);
        assertTrue((response.getStatusCode() == Response.Status.OK.getStatusCode()),
                "Wrong status code ,Expected " + Response.Status.OK.getStatusCode() + ", But received " + response
                        .getStatusCode());
    }

    @Test(groups = { "wso2.greg",
            "wso2.greg.es" }, description = "Test Update review for an asset", dependsOnMethods = { "testAddReview" })
    public void testUpdateReview() throws JSONException, XPathExpressionException {
        ClientResponse response = genericRestClient
                .genericRestRequestPut(storeUrl + USER_REVIEW_UPDATE_URL, MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, createReviewPayload(true), queryParamMap, headerMap, cookieHeader);
        assertTrue((response.getStatusCode() == Response.Status.OK.getStatusCode()),
                "Wrong status code ,Expected 200 OK ,But Received " + response.getStatusCode());
    }

    @Test(groups = { "wso2.greg",
            "wso2.greg.es" }, description = "Test Update review for an asset from a different user", dependsOnMethods = {
            "testUpdateReview" })
    public void testUpdateReviewAttack() throws XPathExpressionException, JSONException {
        setTestEnvironment(false);
        ClientResponse response = genericRestClient
                .genericRestRequestPut(storeUrl + USER_REVIEW_UPDATE_URL, MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, createReviewPayload(true), queryParamMap, headerMap, cookieHeader);
        assertTrue((response.getStatusCode() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()),
                "Wrong status code ,Expected " + Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
                        + ", But received " + response.getStatusCode());
    }

    /**
     * To create a review for the pre-shipped asset
     *
     * @param isUpdate To indicate whether it is an update or not.
     * @return Relevant review object in string that need to be passed with the request.
     */
    private String createReviewPayload(boolean isUpdate) throws JSONException, XPathExpressionException {
        String assetId = "gadget:a52c3524-aad2-411f-84a2-7aa32a98bde5";
        String targetField = "target";
        String urlDomainField = "url-domain";
        String verbField = "verb";
        String objectField = "object";
        String review = "review";
        String post = "post";
        String isMyCommentField = "isMyComment";
        String objectTypeField = "objectType";
        String contentField = "content";
        String ratingField = "rating";
        String totalItemsField = "totalItems";
        String likeField = "likes";
        String disLikeField = "dislikes";
        String assetIdField = "id";

        queryParamMap.clear();
        queryParamMap.put(targetField, assetId);
        queryParamMap.put(urlDomainField, automationContext.getContextTenant().getDomain());

        JSONObject reviewObject = new JSONObject();
        if (isUpdate) {
            reviewObject.put(verbField, review);
        } else {
            reviewObject.put(verbField, post);
        }
        reviewObject.put(isMyCommentField, true);
        JSONObject commentObject = new JSONObject();
        commentObject.put(objectTypeField, review);
        commentObject.put(contentField, "Update from admin");
        commentObject.put(ratingField, 4);

        if (isUpdate) {
            commentObject.put(assetIdField, 1);
        } else {
            JSONObject likeObject = new JSONObject();
            likeObject.put(totalItemsField, 0);
            JSONObject disLikeObject = new JSONObject();
            disLikeObject.put(totalItemsField, 0);
            commentObject.put(likeField, likeObject);
            commentObject.put(disLikeField, disLikeObject);
        }
        JSONObject targetObject = new JSONObject();
        targetObject.put(assetIdField, assetId);
        reviewObject.put(targetField, targetObject);
        reviewObject.put(objectField, commentObject);
        return reviewObject.toString();
    }

    @DataProvider
    private static Object[][] userModeProvider() {
        return new TestUserMode[][]{
                new TestUserMode[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

}
