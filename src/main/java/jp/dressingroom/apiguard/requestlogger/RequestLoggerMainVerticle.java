package jp.dressingroom.apiguard.requestlogger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;


public class RequestLoggerMainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.deployVerticle("jp.dressingroom.apiguard.requestlogger.verticle.HttpReverseProxyVerticle", res -> {
      if (res.failed()) {
        System.out.println("HttpReverseProxyVerticle start failed: " + res.cause());
        startPromise.fail("HttpReverseProxyVerticle start failed: " + res.cause());
      }
    });
    startPromise.complete();
  }

//  @Override
//  public void stop() {
//  }
}
