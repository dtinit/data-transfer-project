package org.dataportabilityproject.serviceProviders.microsoft;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.function.Supplier;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.microsoft.calendar.MicrosoftCalendarService;
import org.dataportabilityproject.serviceProviders.microsoft.mail.MicrosoftMailService;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;

/**
 * The {@link ServiceProvider} for Microsoft (http://www.microsoft.com/).
 */
public final class MicrosoftServiceProvider implements ServiceProvider {
    private final Supplier<MicrosoftCalendarService> calendarService;
    private final Supplier<MicrosoftMailService> mailService;

    public MicrosoftServiceProvider(Secrets secrets, IOInterface consoleIO) {
        MicrosoftAuth auth = new MicrosoftAuth(secrets);
        // NB: this isn't memoized so that you can do a round trip with different accounts.
        this.calendarService = () -> {
            try {
                String account = consoleIO.ask("Enter Microsoft email account");
                return new MicrosoftCalendarService(getToken(auth, account), account);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't fetch account info", e);
            }
        };
        this.mailService = () -> {
            try {
              String account = consoleIO.ask("Enter Microsoft email account");
              String password = consoleIO.ask("Enter Microsoft email account password");
              return new MicrosoftMailService(account, password);
            } catch (IOException e) {
              throw new IllegalStateException("Couldn't fetch account info", e);
            }
        };
    }

    @Override public String getName() {
        return "Microsoft";
    }

    @Override
    public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(PortableDataType.CALENDAR, PortableDataType.MAIL);
    }

    @Override
    public ImmutableList<PortableDataType> getImportTypes() {
        return ImmutableList.of(PortableDataType.CALENDAR);
    }

    @Override
    public Exporter<? extends DataModel> getExporter(PortableDataType type) throws IOException {
       switch (type) {
            case CALENDAR:
                return calendarService.get();
            case MAIL:
                return mailService.get();
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }

    @Override
    public Importer<? extends DataModel> getImporter(PortableDataType type) throws IOException {
        switch (type) {
            case CALENDAR:
                return calendarService.get();
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
    }

    /** Obtains an oauth token for the given account. */
    private String getToken(MicrosoftAuth auth, String account) {
        String token;
        try {
            token = auth.getToken(account);
        } catch (IOException e) {
            System.out.println("Error obtaining token");
            e.printStackTrace();
            throw new IllegalStateException("Error obtaining token", e);
        }
        return token;
    }
}
