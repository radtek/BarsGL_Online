package ru.rbt.barsgl.ejbcore.remote.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * Реализация {@link ServiceInvoker} на базе протокола HTTP
 */
public class HttpServiceInvoker implements ServiceInvoker {

    private final URL url;
    private final Proxy proxy;

    public HttpServiceInvoker(URL url, Proxy proxy) {
        this.url = url;
        this.proxy = proxy;
    }

    public ServiceResponse invokeService(ServiceRequest request) throws ServiceInvocationException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) (proxy == null ? this.url.openConnection() : this.url.openConnection(proxy));
            connection.setConnectTimeout(0);
            connection.setReadTimeout(0);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();
            // send request
            byte[] requestData = Serializer.writeObject(request);
            OutputStream outs = connection.getOutputStream();
            outs.write(requestData);
            outs.flush();
            outs.close();
            // get response data
            InputStream ins = connection.getInputStream();
            int code = connection.getResponseCode();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new ServiceInvocationException("HTTP error " + code + ": " + connection.getResponseMessage());
            }
            if (ins == null) {
                // особый случай: на серверной стороне не удалось сериализовать ответ
                throw new ServiceInvocationException("Failed to get a valid response. Please, see server-side log for more details");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(ins.available());
            byte[] buffer = new byte[1024];
            do {
                int length = ins.read(buffer);
                if (length < 0) {
                    break;
                }
                baos.write(buffer, 0, length);
            } while (true);
            ins.close();
            byte[] data = baos.toByteArray();
            return (ServiceResponse)Serializer.readObject(data);
        } catch (Throwable th) {
            if (th instanceof ServiceInvocationException) {
                throw (ServiceInvocationException)th;
            }
            throw new ServiceInvocationException("Failed to perform call to " + url, th);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable ignore) {}

        }
    }

}