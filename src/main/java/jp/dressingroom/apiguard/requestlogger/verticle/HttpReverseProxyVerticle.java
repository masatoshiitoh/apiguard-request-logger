package jp.dressingroom.apiguard.requestlogger.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jp.dressingroom.apiguard.requestlogger.ConfigKeyNames;

import java.util.Base64;


public class HttpReverseProxyVerticle extends AbstractVerticle {
  WebClient client;
  String proxyHost;
  String proxyUserAgent;
  int proxyPort;
  Boolean proxyUseSsl;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
      configRetriever.getConfig((json -> {
        try {
          JsonObject result = json.result();

          // setup proxy client
          proxyHost = result.getString(ConfigKeyNames.REQUEST_LOGGER_PROXY_HOSTNAME.value(), "localhost");
          proxyPort = result.getInteger(ConfigKeyNames.REQUEST_LOGGER_PROXY_PORT.value(), 8080);
          proxyUserAgent = result.getString(ConfigKeyNames.REQUEST_LOGGER_PROXY_USERAGENT.value(), "ApiGuard/RequestLogger 1.0");
          proxyUseSsl = result.getBoolean(ConfigKeyNames.REQUEST_LOGGER_PROXY_USESSL.value(), false);

          WebClientOptions webClientOptions = new WebClientOptions();
          webClientOptions.setUserAgent(proxyUserAgent);
          client = WebClient.create((Vertx) vertx, webClientOptions);

          Integer port = result.getInteger(ConfigKeyNames.REQUEST_LOGGER_SERVER_PORT.value());
          HttpServer server = vertx.createHttpServer();
          Router router = Router.router(vertx);

          // Catch all - methods and paths.
          Route route = router.route();
          route.handler(bodiedProxyHandler());
          server.requestHandler(router).listen(port);
        } catch (Exception e) {
          startPromise.fail(e);
        }
      startPromise.complete();
      }));
  }

  private Handler<RoutingContext> bodiedProxyHandler() {
    return routingContext -> {
      routingContext.request().bodyHandler(bodiedProxyHandler -> {
          Base64.Encoder encoder = Base64.getEncoder();
          sendResponse(routingContext, HttpStatusCodes.OK,
            "request logger called." +
              "payload(raw): " + new String(bodiedProxyHandler.getBytes())
          );
        }
      );
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
