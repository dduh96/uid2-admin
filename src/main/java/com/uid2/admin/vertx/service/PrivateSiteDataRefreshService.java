package com.uid2.admin.vertx.service;

import com.uid2.admin.job.JobDispatcher;
import com.uid2.admin.job.jobsync.PrivateSiteDataSyncJob;
import com.uid2.admin.vertx.WriteLock;
import com.uid2.shared.auth.Role;
import com.uid2.shared.middleware.AuthMiddleware;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class PrivateSiteDataRefreshService implements IService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateSiteDataRefreshService.class);

    private final AuthMiddleware auth;
    private final JobDispatcher jobDispatcher;
    private final WriteLock writeLock;
    private final JsonObject config;

    public PrivateSiteDataRefreshService(
            AuthMiddleware auth,
            JobDispatcher jobDispatcher,
            WriteLock writeLock,
            JsonObject config) {
        this.auth = auth;
        this.jobDispatcher = jobDispatcher;
        this.writeLock = writeLock;
        this.config = config;
    }

    @Override
    public void setupRoutes(Router router) {
        // this can be called by a k8s cronjob setup such as
        // https://gitlab.adsrvr.org/uid2/k8-deployment/-/tree/master/uid2
        router.post("/api/private-sites/refresh").blockingHandler(auth.handle((ctx) -> {
                    synchronized (writeLock) {
                        this.handlePrivateSiteDataGenerate(ctx);
                    }
                },
                //can be other role
                Role.ADMINISTRATOR, Role.SECRET_MANAGER));

        router.post("/api/private-sites/refreshNow").blockingHandler(auth.handle((ctx) -> {
                    synchronized (writeLock) {
                        this.handlePrivateSiteDataGenerateNow(ctx);
                    }
                },
                //can be other role
                Role.ADMINISTRATOR, Role.SECRET_MANAGER));
    }

    private void handlePrivateSiteDataGenerate(RoutingContext rc) {
        try {
            PrivateSiteDataSyncJob job = new PrivateSiteDataSyncJob(config, writeLock);
            jobDispatcher.enqueue(job);
            rc.response().end("OK");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            rc.fail(500, e);
        }
    }

    private void handlePrivateSiteDataGenerateNow(RoutingContext rc) {
        try {
            PrivateSiteDataSyncJob job = new PrivateSiteDataSyncJob(config, writeLock);
            jobDispatcher.enqueue(job);
            jobDispatcher.executeNextJob();
            rc.response().end("OK");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            rc.fail(500, e);
        }
    }
}
