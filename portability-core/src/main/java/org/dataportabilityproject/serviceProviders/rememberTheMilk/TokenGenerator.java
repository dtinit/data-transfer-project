package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.AuthElement;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.Frob;
import org.dataportabilityproject.shared.IOInterface;

import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkState;

/**
 * Generates a token using the flow described: https://www.rememberthemilk.com/services/api/authentication.rtm
 */
public class TokenGenerator {
    private static final String AUTH_URL = "http://api.rememberthemilk.com/services/auth/";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private final RememberTheMilkSignatureGenerator signatureGenerator;
    private final IOInterface ioInterface;
    private final String apiKey;

    private AuthElement authElement;

    public TokenGenerator(
            RememberTheMilkSignatureGenerator signatureGenerator,
            IOInterface ioInterface,
            String apiKey) {
        this.signatureGenerator = signatureGenerator;
        this.ioInterface = ioInterface;
        this.apiKey = apiKey;
    }

    public String getToken() throws IOException {
        return getToken(false);
    }

    public String getToken(boolean force) throws IOException {
        if (authElement == null || force) {
            String frob = getFrob();
            presentLinkToUser(frob);
            authElement = getAuthToken(frob);
        }

        return authElement.auth.token;
    }

    public AuthElement validateToken(String auth_token) throws IOException {
        URL url = new URL(RememberTheMilkMethods.CHECK_TOKEN.getUrl()
                + "&api_key=" + apiKey
                + "&auth_token=" + auth_token);
        String signature = signatureGenerator.getSignature(url);
        URL signedUrl = new URL(url + "&api_sig=" + signature);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
        getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
        HttpResponse response = getRequest.execute();
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
        }

        AuthElement authElement = response.parseAs(AuthElement.class);
        checkState(authElement.stat.equals("ok"), "state must be ok: %s", authElement);
        checkState(!Strings.isNullOrEmpty(authElement.auth.token), "token must not be empty", authElement);
        return authElement;
    }

    private String getFrob() throws IOException{
        URL url = new URL(RememberTheMilkMethods.GET_FROB.getUrl() + "&api_key=" + apiKey);
        String signature = signatureGenerator.getSignature(url);
        URL signedUrl = new URL(url + "&api_sig=" + signature);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
        getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
        HttpResponse response = getRequest.execute();
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
        }
        Frob frob =response.parseAs(Frob.class);

        checkState(frob.stat.equals("ok"), "frob state must be ok: %s", frob);
        checkState(!Strings.isNullOrEmpty(frob.frob), "frob must not be empty", frob);
        return frob.frob;
    }

    private void presentLinkToUser(String frob) throws IOException {
        URL authUrlUnsigned = new URL(AUTH_URL + "?api_key=" + apiKey + "&perms=write&frob=" + frob);
        String authSignature = signatureGenerator.getSignature(authUrlUnsigned);
        URL authUrlSigned = new URL(authUrlUnsigned + "&api_sig=" + authSignature);

        ioInterface.ask("Please visit " + authUrlSigned + " and flow the flow there then hit return/enter");
    }

    private AuthElement getAuthToken(String frob) throws IOException {
        URL url = new URL(RememberTheMilkMethods.GET_TOKEN.getUrl()
                + "&api_key=" + apiKey
                + "&frob=" + frob);
        String signature = signatureGenerator.getSignature(url);
        URL signedUrl = new URL(url + "&api_sig=" + signature);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
        getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
        HttpResponse response = getRequest.execute();
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
        }
        AuthElement authElement =response.parseAs(AuthElement.class);
        checkState(authElement.stat.equals("ok"), "state must be ok: %s", authElement);
        checkState(!Strings.isNullOrEmpty(authElement.auth.token), "token must not be empty", authElement);
        System.out.println("Auth Token: " + authElement);
        return authElement;
    }
}
