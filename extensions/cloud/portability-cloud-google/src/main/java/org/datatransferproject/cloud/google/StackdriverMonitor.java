package org.datatransferproject.cloud.google;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import org.datatransferproject.api.launcher.Monitor;

import java.util.Collections;
import java.util.function.Supplier;

class StackdriverMonitor implements Monitor {
    private static final String LOG_NAME = "worker-instance-log";
    private final Logging logging;

    public StackdriverMonitor(Logging logging) {
        this.logging = logging;
    }

    @Override
    public void severe(Supplier<String> supplier, Object... data) {
        log(Severity.ERROR, supplier, data);
    }

    @Override
    public void info(Supplier<String> supplier, Object... data) {
        log(Severity.INFO, supplier, data);
    }

    @Override
    public void debug(Supplier<String> supplier, Object... data) {
        log(Severity.NOTICE, supplier, data);
    }

    private void log(Severity severity, Supplier<String> supplier, Object... data) {
        if (true) {
            //throw new RuntimeException("cool my code is called");
        }
        String logMessage = String.format(supplier.get(), data);
        LogEntry entry = LogEntry.newBuilder(Payload.StringPayload.of(logMessage))
                .setSeverity(severity)
                .setLogName(LOG_NAME)
                .setResource(MonitoredResource.newBuilder("TODO:JobId").build())
                .build();

        // Writes the log entry asynchronously
        logging.write(Collections.singleton(entry));
    }
}
