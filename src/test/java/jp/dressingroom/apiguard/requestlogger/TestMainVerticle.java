package jp.dressingroom.apiguard.requestlogger;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.dressingroom.apiguard.httpresponder.HttpResponderMainVerticle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  static int nextPort;

  int responderPort;
  int targetPort;

  @BeforeAll
  static void initPort() {
    nextPort = 8000;
  }

  static int getPort() {
    return nextPort ++;
  }

  @BeforeEach
  void deployVerticle(Vertx vertx, VertxTestContext testContext) {

    responderPort = getPort();
    targetPort = getPort();

    System.setProperty("server.port", String.valueOf(responderPort)); // http-responder port
    System.setProperty("requestlogger.server.port", String.valueOf(targetPort));
    System.setProperty("requestlogger.proxy.port", String.valueOf(responderPort));

    vertx.deployVerticle(new HttpResponderMainVerticle(), r1 -> {
      vertx.deployVerticle(new RequestLoggerMainVerticle(), r2 -> {
        if (r2.succeeded()) {
          testContext.completeNow();
        } else {
          testContext.failNow(new Exception());
        }
      });
    });
  }

  @Test
  void checkHttpResponderDeployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    WebClient client = WebClient.create(vertx);

    client.get(responderPort, "localhost", "/hello")
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

    client.get(targetPort, "localhost", "/")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> {
        assertTrue(response.statusCode() == 200);
        assertTrue(response.headers().contains("httpresponder"));
        assertTrue(response.headers().get("httpresponder").equals("true"));
        testContext.completeNow();
      })));
  }

}
