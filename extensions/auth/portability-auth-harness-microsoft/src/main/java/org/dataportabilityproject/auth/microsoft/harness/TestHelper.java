package org.dataportabilityproject.auth.microsoft.harness;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 *
 */
public final class TestHelper {

    /**
     * Creates an OKHttpClient.Builder that accepts self-signd certs from a host and Microsoft domains.
     */
    public static OkHttpClient.Builder createTestBuilder(String host) throws Exception {
        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
        }};

        X509TrustManager x509TrustManager = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
        };

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        SecureRandom secureRandom = new SecureRandom();
        sslContext.init(null, trustManagers, secureRandom);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        clientBuilder.sslSocketFactory(socketFactory, x509TrustManager);

        HostnameVerifier hostnameVerifier = (hostname, session) -> host.equals(hostname) || hostname.endsWith("microsoftonline.com") || hostname.endsWith("microsoft.com");

        clientBuilder.hostnameVerifier(hostnameVerifier);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        clientBuilder.addInterceptor(logging);
        return clientBuilder;
    }

    private TestHelper() {
    }
}
