package jp.dressingroom.apiguard.requestlogger.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;


public class HttpReverseProxyVerticle extends AbstractVerticle {
  WebClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // you must prepare redis to start MainVerticle
    HttpServer server = vertx.createHttpServer();
    client = WebClient.create((Vertx) vertx);

    Router router = Router.router(vertx);
    router.get("/").handler(getTopRoutingHandler());
    router.get("/login").handler(getLoginRoutingHandler());
    router.post("/api").handler(postApiRoutingHandler());
    router.get("/simple").handler(getSimpleProxyHandler());
    server.requestHandler(router).listen(8888);

    startPromise.complete();
  }

  private Handler<RoutingContext> getSimpleProxyHandler() {
    return routingContext -> {
      System.out.println("1");
      WebClient client = WebClient.create((Vertx) vertx);
      System.out.println("2");

      client
        .get(443, "yahoo.co.jp", "/")
        .ssl(true)
        .send(ar -> {
          System.out.println("3");
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            System.out.println(response.body().toString());
            // build response body
            HttpServerResponse proxyResponse = routingContext.response();

            // Write to the response and end it
            proxyResponse.end((io.vertx.core.buffer.Buffer) response.body());

          } else {
            // error
            sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
          }
        });
    };
  }

  private Handler<RoutingContext> getTopRoutingHandler() {
    return routingContext -> {

      EventBus eventBus = vertx.eventBus();
      String requestParamId = routingContext.request().getParam("id");

      eventBus.request(ApiguardEventBusNames.DECRYPT.value(), requestParamId, decrypted -> {
        if (decrypted.failed()) {
          sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
          return;
        }

        // verify token
        String decryptedMessage = decrypted.result().body().toString();
        eventBus.request(ApiguardEventBusNames.ONETIME_TOKEN_VERIFY.value(), decryptedMessage, verifyOnetimeTokenReply -> {
          if (verifyOnetimeTokenReply.failed()) {
            sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
          }

          String responseBodyString = "Hello World from Vert.x-Web! id=" + requestParamId
            + " :" + verifyOnetimeTokenReply.result().body().toString();

          // TODO: place proxy call here
          // if 5xx error returns, DO NOT UPDATE onetime token!!!!
          // because when you update with error reeturn, user client will get "error result" with current token.

          // call proxy

          // check result

          // update onetime token with proxy response.
          eventBus.request(ApiguardEventBusNames.ONETIME_TOKEN_UPDATE.value(), responseBodyString, updateOnetimeTokenReply -> {
            if (updateOnetimeTokenReply.failed()) {
              sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
              return;
            }

            String updatedResponseBodyString = updateOnetimeTokenReply.result().body().toString();
            // encrypt proxy response body payload.
            eventBus.request(ApiguardEventBusNames.ENCRYPT.value(), updatedResponseBodyString, encrypted -> {
              if (encrypted.failed()) {
                sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
                return;
              }

              // build response body
              HttpServerResponse response = routingContext.response();
              response.putHeader("content-type", "text/plain");
              // Write to the response and end it
              response.end(encrypted.result().body().toString());
            });
          });
        });
      });
    };
  }

  private Handler<RoutingContext> getLoginRoutingHandler() {
    return routingContext -> {
      // This handler will be called for every request

      EventBus eventBus = vertx.eventBus();
      String message = routingContext.request().getParam("id");
      eventBus.request(ApiguardEventBusNames.ONETIME_TOKEN_RESET.value(), message, resetOnetimeTokenReply -> {
        // guard
        if (resetOnetimeTokenReply.failed()) {
          sendResponse(routingContext, HttpStatusCodes.INTERNAL_SERVER_ERROR);
          return;
        }

        // build response body
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "text/plain");
        // Write to the response and end it
        response.end("Login Handler from Vert.x-Web!");
        return;
      });
    };
  }

  private Handler<RoutingContext> postApiRoutingHandler() {
    return routingContext -> {
      routingContext.request().bodyHandler(bodyHandler -> {
        byte[] body = bodyHandler.getBytes();
        String id = routingContext.request().getParam("id");

        System.out.println("posted id: " + id + " body: " + new String(body));

        // build response body
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "text/plain");
        // Write to the response and end it
        response.end("postApiRoutingHandler received: " + new String(body));
        return;
      });
    };
  }

  /**
   * send response to requester with status
   * @param routingContext
   * @param status
   */
  private void sendResponse(RoutingContext routingContext, HttpStatusCodes status) {
    sendResponse(routingContext, status, null);
  }

  /**
   *
   * @param routingContext
   * @param status
   * @param message
   */
  private void sendResponse(RoutingContext routingContext, HttpStatusCodes status, String message) {
    HttpServerResponse response = routingContext.response();
    response.setStatusCode(status.value());
    if (message == null) {
      response.end();
    } else {
      response.end(message);
    }
  }
}
