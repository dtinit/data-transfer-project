package org.dataportabilityproject.transfer.google.contacts;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects.SOURCE_PARAM_NAME_TYPE;

import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.Source;
import ezvcard.VCard;
import ezvcard.parameter.VCardParameters;
import ezvcard.property.StructuredName;
import ezvcard.property.TextProperty;
import ezvcard.property.VCardProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.google.gdata.util.common.base.Pair;
import org.dataportabilityproject.datatransfer.google.contacts.GoogleContactsExporter;
import org.junit.Test;

public class GoogleContactsExporterTest {
  private static final String DEFAULT_SOURCE_TYPE = "CONTACT";
  private static final Source DEFAULT_SOURCE = new Source().setType(DEFAULT_SOURCE_TYPE);
  private static final FieldMetadata PRIMARY_FIELD_METADATA =
      new FieldMetadata().setSource(DEFAULT_SOURCE).setPrimary(true);
  private static final FieldMetadata SECONDARY_FIELD_METADATA =
      new FieldMetadata().setSource(DEFAULT_SOURCE).setPrimary(false);
  private static final Name DEFAULT_NAME =
      new Name().setFamilyName("Church").setGivenName("Alonzo").setMetadata(PRIMARY_FIELD_METADATA);
  private static final Person DEFAULT_PERSON =
      new Person().setNames(Collections.singletonList(DEFAULT_NAME));

  @Test
  public void testConversionToVCardNames() {
    // Set up Person with a primary name and two secondary names
    String primaryGivenName = "Mark";
    String primaryFamilyName = "Twain";
    Name primaryName =
        new Name()
            .setGivenName(primaryGivenName)
            .setFamilyName(primaryFamilyName)
            .setMetadata(PRIMARY_FIELD_METADATA);

    String alternateGivenName1 = "Samuel";
    String alternateFamilyName1 = "Clemens";
    String alternateSourceType1 = "PROFILE";
    Name alternateName1 =
        new Name()
            .setGivenName(alternateGivenName1)
            .setFamilyName(alternateFamilyName1)
            .setMetadata(
                new FieldMetadata()
                    .setPrimary(false)
                    .setSource(new Source().setType(alternateSourceType1)));
    String alternateGivenName2 = "Louis";
    String alternateFamilyName2 = "de Conte";
    String alternateSourceType2 = "PEN_NAME";
    Name alternateName2 =
        new Name()
            .setGivenName(alternateGivenName2)
            .setFamilyName(alternateFamilyName2)
            .setMetadata(
                new FieldMetadata()
                    .setPrimary(false)
                    .setSource(new Source().setType(alternateSourceType2)));

    // Order shouldn't matter
    Person person =
        new Person().setNames(Arrays.asList(alternateName2, alternateName1, primaryName));

    // Run test
    VCard vCard = GoogleContactsExporter.convert(person);

    // Check name conversion correctness
    List<StructuredName> structuredNames = vCard.getStructuredNames();
    assertThat(structuredNames.size()).isEqualTo(3);

    // Check primary (non-alternate) names
    List<StructuredName> actualPrimaryNames =
        structuredNames.stream().filter(n -> n.getAltId() == null).collect(Collectors.toList());
    List<Pair<String, String>> actualPrimaryNamesValues =
        actualPrimaryNames
            .stream()
            .map(GoogleContactsExporterTest::getGivenAndFamilyNames)
            .collect(Collectors.toList());
    assertThat(actualPrimaryNamesValues)
        .containsExactly(Pair.of(primaryGivenName, primaryFamilyName));
    List<String> actualPrimarySourceValues =
        actualPrimaryNames
            .stream()
            .map(a -> a.getParameter(SOURCE_PARAM_NAME_TYPE))
            .collect(Collectors.toList());
    assertThat(actualPrimarySourceValues).containsExactly(DEFAULT_SOURCE_TYPE);

    // Check alternate names
    List<StructuredName> actualAlternateNames =
        structuredNames.stream().filter(n -> n.getAltId() != null).collect(Collectors.toList());
    List<Pair<String, String>> actualAlternateNamesValues =
        actualAlternateNames
            .stream()
            .map(GoogleContactsExporterTest::getGivenAndFamilyNames)
            .collect(Collectors.toList());
    assertThat(actualAlternateNamesValues)
        .containsExactly(
            Pair.of(alternateGivenName1, alternateFamilyName1),
            Pair.of(alternateGivenName2, alternateFamilyName2));
    List<String> actualAlternateSourceValues =
        actualAlternateNames
            .stream()
            .map(a -> a.getParameter(SOURCE_PARAM_NAME_TYPE))
            .collect(Collectors.toList());
    assertThat(actualAlternateSourceValues)
        .containsExactly(alternateSourceType1, alternateSourceType2);
  }

  private static Pair<String, String> getGivenAndFamilyNames(StructuredName structuredName) {
    return Pair.of(structuredName.getGiven(), structuredName.getFamily());
  }

  private static <T extends VCardProperty, V> List<V> getValuesFromProperties(
      List<T> propertyList, Function<T, V> function) {
    return propertyList.stream().map(function).collect(Collectors.toList());
  }

  private static <T extends TextProperty> List<String> getValuesFromTextProperties(
      List<T> propertyList) {
    return getValuesFromProperties(propertyList, T::getValue);
  }

  private static <T extends VCardProperty> List<T> getPropertiesWithPreference(
      VCard vCard, Class<T> clazz, int preference) {
    return vCard
        .getProperties(clazz)
        .stream()
        .filter(p -> p.getParameter(VCardParameters.PREF).equals(Integer.toString(preference)))
        .collect(Collectors.toList());
  }
}
