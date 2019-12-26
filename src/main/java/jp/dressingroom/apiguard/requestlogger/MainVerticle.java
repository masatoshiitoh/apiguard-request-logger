package jp.dressingroom.apiguard.requestlogger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;


public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.deployVerticle("HttpReverseProxyVerticle", res -> {
      if (res.failed()) { startPromise.fail("HttpReverseProxyVerticle start failed: " + res.cause());}
    });
  }

//  @Override
//  public void stop() {
//  }
}
