package org.datatransferproject.datatransfer.backblaze.common;

import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.conn.DnsResolver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

// This class is only required because Backblaze only supports IPV4 on the B2 Native APIs, the S3
// compatible APIs support IPV6. This can be deleted when all B2 API endpoints support IPV6.
public class CustomIPv4DnsResolver implements DnsResolver {
    private final SystemDefaultDnsResolver systemDefaultDnsResolver = new SystemDefaultDnsResolver();

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        // Filter to return only IPv4 addresses
        return Arrays.stream(systemDefaultDnsResolver.resolve(host))
                .filter(inetAddress -> inetAddress.getAddress().length == 4)
                .toArray(InetAddress[]::new);
    }
}

