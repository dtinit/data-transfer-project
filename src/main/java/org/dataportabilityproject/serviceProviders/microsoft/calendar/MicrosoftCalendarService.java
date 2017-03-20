package org.dataportabilityproject.serviceProviders.microsoft.calendar;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.calendar.CalendarModel;
import org.dataportabilityproject.serviceProviders.microsoft.MicrosoftAuth;

/**
 * Stub for the Microsoft calendar service.
 */
public class MicrosoftCalendarService implements Importer<CalendarModel>, Exporter<CalendarModel> {
    private MicrosoftAuth auth;
    public MicrosoftCalendarService(MicrosoftAuth auth) {
        this.auth = auth;
    }

    @Override public void importItem(CalendarModel object) throws IOException {
      System.out.println("importItem: " + object);
    }

    @Override public Collection<CalendarModel> export() throws IOException {
      System.out.println("export called");
      try {
        String token = auth.getToken();
        System.out.println("Obtained token");
      } catch (Exception e) {
        System.out.println("Error obtaining token");
        e.printStackTrace();
      }
      return ImmutableList.<CalendarModel>of();
    }

}
