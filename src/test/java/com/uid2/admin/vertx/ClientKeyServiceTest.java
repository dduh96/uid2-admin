package com.uid2.admin.vertx;

import com.uid2.admin.model.Site;
import com.uid2.admin.vertx.service.ClientKeyService;
import com.uid2.admin.vertx.service.IService;
import com.uid2.admin.vertx.test.ServiceTestBase;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.Role;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClientKeyServiceTest extends ServiceTestBase {
    @Override
    protected IService createService() {
        return new ClientKeyService(config, auth, writeLock, clientKeyStoreWriter, clientKeyProvider, siteProvider, keyGenerator);
    }

    private void checkClientKeyResponse(ClientKey[] expectedClients, Object[] actualClients) {
        assertEquals(expectedClients.length, actualClients.length);
        for (int i = 0; i < expectedClients.length; ++i) {
            ClientKey expectedClient = expectedClients[i];
            JsonObject actualClient = (JsonObject) actualClients[i];
            assertEquals(expectedClient.getName(), actualClient.getString("name"));
            assertEquals(expectedClient.getContact(), actualClient.getString("contact"));
            assertEquals(expectedClient.isDisabled(), actualClient.getBoolean("disabled"));
            assertEquals(expectedClient.getSiteId(), actualClient.getInteger("site_id"));

            List<Role> actualRoles = actualClient.getJsonArray("roles").stream()
                    .map(r -> Role.valueOf((String) r))
                    .collect(Collectors.toList());
            assertEquals(expectedClient.getRoles().size(), actualRoles.size());
            for (Role role : expectedClient.getRoles()) {
                assertTrue(actualRoles.contains(role));
            }
        }
    }

    @Test
    void clientAdd(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        setSites(new Site(5, "test_site", true));
        ClientKey[] expectedClients = {
                new ClientKey("", "", "test_client").withRoles(Role.GENERATOR).withSiteId(5)
        };

        post(vertx, "api/client/add?name=test_client&roles=generator&site_id=5", "", ar -> {
            assertTrue(ar.succeeded());
            HttpResponse response = ar.result();
            assertEquals(200, response.statusCode());
            checkClientKeyResponse(expectedClients, new Object[]{response.bodyAsJsonObject()});

            try {
                verify(clientKeyStoreWriter).upload(collectionOfSize(1), isNull());
            } catch (Exception ex) {
                fail(ex);
            }

            testContext.completeNow();
        });
    }

    @Test
    void clientAddUnknownSiteId(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        post(vertx, "api/client/add?name=test_client&roles=generator&site_id=5", "", expectHttpError(testContext, 404));
    }

    @Test
    void clientAddSpecialSiteId1(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        post(vertx, "api/client/add?name=test_client&roles=generator&site_id=1", "", expectHttpError(testContext, 400));
    }

    @Test
    void clientAddSpecialSiteId2(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        post(vertx, "api/client/add?name=test_client&roles=generator&site_id=2", "", expectHttpError(testContext, 400));
    }

    @Test
    void clientUpdate(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        setSites(new Site(5, "test_site", true));
        setClientKeys(new ClientKey("", "","test_client").withRoles(Role.GENERATOR).withSiteId(4));
        ClientKey[] expectedClients = {
                new ClientKey("", "", "test_client").withRoles(Role.GENERATOR).withSiteId(5)
        };

        post(vertx, "api/client/update?name=test_client&site_id=5", "", ar -> {
            assertTrue(ar.succeeded());
            HttpResponse response = ar.result();
            assertEquals(200, response.statusCode());
            checkClientKeyResponse(expectedClients, new Object[]{response.bodyAsJsonObject()});

            try {
                verify(clientKeyStoreWriter).upload(collectionOfSize(1), isNull());
            } catch (Exception ex) {
                fail(ex);
            }

            testContext.completeNow();
        });
    }

    @Test
    void clientUpdateUnknownClientName(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        setSites(new Site(5, "test_site", true));
        post(vertx, "api/client/update?name=test_client&site_id=5", "", expectHttpError(testContext, 404));
    }

    @Test
    void clientUpdateUnknownSiteId(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        setClientKeys(new ClientKey("", "","test_client").withRoles(Role.GENERATOR).withSiteId(4));
        post(vertx, "api/client/update?name=test_client&site_id=5", "", expectHttpError(testContext, 404));
    }

    @Test
    void clientUpdateSpecialSiteId1(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        setClientKeys(new ClientKey("", "","test_client").withRoles(Role.GENERATOR).withSiteId(4));
        post(vertx, "api/client/update?name=test_client&site_id=1", "", expectHttpError(testContext, 400));
    }

    @Test
    void clientUpdateSpecialSiteId2(Vertx vertx, VertxTestContext testContext) {
        fakeAuth(Role.CLIENTKEY_ISSUER);
        setClientKeys(new ClientKey("", "","test_client").withRoles(Role.GENERATOR).withSiteId(4));
        post(vertx, "api/client/update?name=test_client&site_id=2", "", expectHttpError(testContext, 400));
    }
}
