package jp.dressingroom.apiguard.requestlogger;

public enum ConfigKeyNames {
  REQUEST_LOGGER_SERVER_PORT("requestlogger.server.port"),

  REQUEST_LOGGER_PROXY_HOSTNAME("requestlogger.proxy.hostname"),
  REQUEST_LOGGER_PROXY_PORT("requestlogger.proxy.port"),
  REQUEST_LOGGER_PROXY_USERAGENT("requestlogger.proxy.ua"),
  REQUEST_LOGGER_PROXY_USESSL("requestlogger.proxy.usessl"),
  ;

  private final String text;

  ConfigKeyNames(final String text) {
    this.text = text;
  }

  public String value() {
    return this.text;
  }
}
