package ru.rbt.barsgl.ejbcore.remote.http;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

/**
 * Реализация {@link ServiceInvokerFactory} на базе протокола HTTP
 */
public final class HttpServiceInvokerFactory implements ServiceInvokerFactory {

    private URL url;
    private Proxy proxy = null;

    /**
     * Создает новый экземпляр {@link ServiceInvokerFactory} с заданным URL http-сервлета
     * @param url URL http-сервлета
     * @throws IllegalArgumentException Если не задан URL
     */
    public HttpServiceInvokerFactory(String url) {
        setUrl(url);
    }

    /**
     * Создает новый экземпляр {@link ServiceInvokerFactory} с заданным URL http-сервлета
     * и настройками прокси-сервера
     * @param url URL http-сервлета
     * @param proxyHost Хост прокси-сервара
     * @param proxyPort Порт прокси-сервера
     * @throws IllegalArgumentException Если не задан URL
     */
    public HttpServiceInvokerFactory(String url, String proxyHost, int proxyPort) {
        setUrl(url);
        setProxy(proxyHost, proxyPort);
    }

    /**
     * Возвращает URL удаленного сервиса
     * @return URL удаленного сервиса
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Устанавливает URL удаленного сервиса
     * @param url URL удаленного сервиса
     */
    public void setUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }
        if (!url.toLowerCase().startsWith("http")) {
            throw new IllegalArgumentException("Invalid URL protocol (only HTTP calls are supported): " + url);
        }
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid URL format: " + url, ex);
        }
    }

    /**
     * Возвращает информацию об используемом прокси-сервере
     * @return Используемый прокси-сервер
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Устанавливает прокси-сервер для использования
     * @param proxyHost Хост прокси-сервара
     * @param proxyPort Порт прокси-сервера
     */
    public void setProxy(String proxyHost, int proxyPort) {
        if (proxyHost == null) {
            proxy = null;
            return;
        }
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    }

    /**
     * Создает новый клиент-объект
     * @return Вновь созданный экземпляр клиент-объекта
     */
    public ServiceInvoker createInvoker() {
        return new HttpServiceInvoker(url, proxy);
    }
}