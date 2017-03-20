package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import org.junit.Test;

import java.net.URL;

import static com.google.common.truth.Truth.assertThat;

public class RememberTheMilkSignatureGeneratorTest {
    private static final String SECRET = "BANANAS";

    private final RememberTheMilkSignatureGenerator signatureGenerator = new RememberTheMilkSignatureGenerator(SECRET);

    @Test
    public void signatureTest() throws Exception {
        URL url = new URL("http://example.com?yxz=foo&feg=bar&abc=baz");
        assertThat(signatureGenerator.getSignature(url)).isEqualTo("82044aae4dd676094f23f1ec152159ba");
    }
}
