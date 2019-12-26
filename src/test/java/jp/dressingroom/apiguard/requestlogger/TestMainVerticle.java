package jp.dressingroom.apiguard.requestlogger;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.dressingroom.apiguard.httpresponder.HttpResponderMainVerticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {


  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {

    System.setProperty("server.port","18888");
    System.setProperty("requestlogger.server.port","18889");
    System.setProperty("requestlogger.proxy.port","18888");

    vertx.deployVerticle(new HttpResponderMainVerticle(), testContext.succeeding(id->testContext.completeNow()));
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

}
