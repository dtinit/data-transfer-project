package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import org.junit.Test;

import java.net.URL;

import static com.google.common.truth.Truth.assertThat;

public class RememberTheMilkSignatureGeneratorTest {
    private static final String KEY = "BANANAS1";
    private static final String SECRET = "BANANAS2";
    private static final String TOKEN = "BANANAS3";

    private final RememberTheMilkSignatureGenerator signatureGenerator = new RememberTheMilkSignatureGenerator(KEY, SECRET, TOKEN);

    @Test
    public void signatureTest() throws Exception {
        URL url = new URL("http://example.com?yxz=foo&feg=bar&abc=baz");
        URL expected = new URL("http://example.com?yxz=foo&feg=bar&abc=baz&api_key=BANANAS1&auth_token=BANANAS3&api_sig=b48f0dd1a18179b3068b16728e214561");
        assertThat(signatureGenerator.getSignature(url)).isEqualTo(expected);
    }
}
