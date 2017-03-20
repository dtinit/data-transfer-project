package org.dataportabilityproject.serviceProviders.microsoft;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.microsoft.calendar.MicrosoftCalendarService;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;

/**
 * The {@link ServiceProvider} for Microsoft (http://www.microsoft.com/).
 */
public final class MicrosoftServiceProvider implements ServiceProvider {
    private final Supplier<MicrosoftCalendarService> calendarService;

    public MicrosoftServiceProvider(Secrets secrets) throws IOException {
        MicrosoftAuth auth = new MicrosoftAuth(secrets);
        this.calendarService = Suppliers.memoize(() -> new MicrosoftCalendarService(auth));
    }

    @Override public String getName() {
        return "Microsoft";
    }

    @Override
    public ImmutableList<PortableDataType> getExportTypes() {
        return ImmutableList.of(PortableDataType.CALENDAR);
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
}
