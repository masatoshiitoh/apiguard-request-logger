package jp.dressingroom.apiguard.requestlogger;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
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

    System.setProperty("server.port","18888"); // http-responder port

    System.setProperty("requestlogger.server.port","18889");
    System.setProperty("requestlogger.proxy.port","18888");

    vertx.deployVerticle(new HttpResponderMainVerticle(), testContext.succeeding(id->testContext.completeNow()));
    vertx.deployVerticle(new RequestLoggerMainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticleDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }

  @Test
  void checkHttpResponderDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18888, "localhost", "/hello")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.body().equals("Hello"));
        assertTrue(response.headers().contains("httpresponder"));
        assertTrue(response.headers().get("httpresponder").equals("true"));
        testContext.completeNow();
      })));
  }

  @Test
  void requestLoggerDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(18889, "localhost", "/")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 200);
        assertTrue(response.headers().contains("httpresponder"));
        assertTrue(response.headers().get("httpresponder").equals("true"));
        testContext.completeNow();
      })));
  }

}
