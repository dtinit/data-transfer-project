/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.transfer.solid.contacts;

import static com.google.common.truth.Truth.assertWithMessage;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import java.io.StringReader;
import java.io.StringWriter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

public class RoundTripToRdfTest {

  @Test
  public void testFromRDF() {
    VCard vcard = SolidContactsExport.parsePerson(getPersonResource(TestData.RDF_TEST_DATA1));

    assertWithMessage("Formatted Name is correct")
        .that(vcard.getFormattedName().getValue())
        .isEqualTo("Cool Kid 1");

    assertWithMessage("Note is correct")
        .that(vcard.getNotes().get(0).getValue())
        .isEqualTo("This is a note for Cool Kid 1");

    assertWithMessage("One email found")
        .that(vcard.getEmails())
        .hasSize(1);

    assertWithMessage("email is correct")
        .that(vcard.getEmails().get(0).getValue())
        .isEqualTo("a@b.com");
  }

  @Test
  public void testFromVcard() {
    for (VCard vcardInput : Ezvcard.parse(TestData.VCARD_TEXT).all()) {
      Model personModel = SolidContactsImport.getPersonModel(vcardInput);

      StringWriter stringWriter = new StringWriter();
      personModel.write(stringWriter, "TURTLE");
      String rdf = stringWriter.toString();

      VCard vcardOutput = SolidContactsExport.parsePerson(getPersonResource(rdf));
      checkEquality(vcardInput, vcardOutput);
    }
  }

  private Resource getPersonResource(String data) {
    String example = "https://example.com/resource1";
    Model defaultModel = ModelFactory.createDefaultModel();
    Model model = defaultModel.read(
        new StringReader(data),
        example,
        "TURTLE");
    return model.getResource(example + "#this");
  }

  private void checkEquality(VCard input, VCard output) {
    String name = input.getFormattedName().getValue();

    assertWithMessage("Structured names match")
        .that(output.getStructuredName())
        .isEqualTo(input.getStructuredName());

    assertWithMessage("Formatted names match for %s", name)
        .that(output.getFormattedName())
        .isEqualTo(input.getFormattedName());

    assertWithMessage("telephone numbers match for %s", name)
        .that(output.getTelephoneNumbers())
        .containsAllIn(input.getTelephoneNumbers());

    assertWithMessage("emails match for %s", name)
        .that(output.getEmails())
        .containsAllIn(input.getEmails());
  }
}
