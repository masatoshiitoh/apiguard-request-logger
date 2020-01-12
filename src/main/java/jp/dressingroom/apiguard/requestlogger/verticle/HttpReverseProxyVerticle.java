package jp.dressingroom.apiguard.requestlogger.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
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
        startPromise.complete();
      } catch (Exception e) {
        startPromise.fail(e);
      }
    }));
  }

  private RequestOptions copyFromRequest(RoutingContext routingContext) {
    RequestOptions requestOptions = new RequestOptions();
    String uri = routingContext.request().uri();
    MultiMap headers = routingContext.request().headers();
    headers.entries().forEach(s -> requestOptions.addHeader(s.getKey(), s.getValue()));
    requestOptions.setHeaders(headers);
    requestOptions.setHost(proxyHost);
    requestOptions.setURI(uri);
    requestOptions.setSsl(proxyUseSsl);
    requestOptions.setPort(proxyPort);

    return requestOptions;
  }

  private Handler<RoutingContext> bodiedProxyHandler() {
    return requestorContext -> {
      requestorContext.request().bodyHandler(bodiedProxyHandler -> {

          JsonObject req;
          req = getRequestInfo(requestorContext);

          HttpMethod method = requestorContext.request().method();
          RequestOptions requestOptions = copyFromRequest(requestorContext);
          // System.out.println("bodiedProxyHandler decryptor: request received as:" + body.toString());
          HttpServerResponse responseToRequestor = requestorContext.response();

          client
            .request(method, requestOptions)
            .ssl(requestOptions.isSsl())
            .sendBuffer(
              requestorContext.getBody(),
              originRequest -> {
                if (originRequest.succeeded()) {
                  HttpResponse<Buffer> responseFromOrigin = originRequest.result();
                  int statusCode = responseFromOrigin.statusCode();

                  responseToRequestor.headers().setAll(responseFromOrigin.headers());
                  if (originRequest.result().body() != null) {
                    responseToRequestor.write(originRequest.result().body());
                  }
                  responseToRequestor
                    .setStatusCode(originRequest.result().statusCode())
                    .end();

                  // success log
                  logRequestResponse(
                    setResponseInfo(req, statusCode, responseFromOrigin.bodyAsString(), responseFromOrigin.headers().entries().toString()));
                } else {
                  responseToRequestor
                    .setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR.value())
                    .end("Origin request failed.");

                  // origin request failed log
                  logRequestResponse(
                    setResponseInfo(req, HttpStatusCodes.INTERNAL_SERVER_ERROR.value(), "", ""));
                }
              });
        }
      );
    };
  }

  JsonObject getRequestInfo(RoutingContext requestorContext) {
    JsonObject req = new JsonObject();

    String remoteHost = requestorContext.request().connection().remoteAddress().host();
    String absoluteUri = requestorContext.request().absoluteURI();
    String requestHeaders = requestorContext.request().headers().entries().toString();
    String requestBody = requestorContext.getBodyAsString();

    req.put("remoteHost", remoteHost)
      .put("requestUri", absoluteUri)
      .put("requestHeader", requestHeaders)
      .put("requestBody", requestBody);
    return req;
  }

  JsonObject setResponseInfo(JsonObject src, int statusCode, String body, String headersString) {
    src.put("statusCode", statusCode)
      .put("responseHeader", headersString)
      .put("responseBody", body);
    return src;
  }

  void logRequestResponse(JsonObject logdata) {
    System.out.println(logdata.toString());
  }
}
