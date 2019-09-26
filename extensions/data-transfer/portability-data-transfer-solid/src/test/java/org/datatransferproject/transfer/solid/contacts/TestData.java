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

class TestData {

  // Gotten from concatenating the data at:
  // https://www.w3.org/2002/12/cal/vcard-examples/
  // add more use cases as needed.
  static final String VCARD_TEXT =
      "BEGIN:VCARD\n"
          + "VERSION:3.0\n"
          + "N:Doe;John;;;\n"
          + "FN:John Doe\n"
          + "ORG:Example.com Inc.;\n"
          + "TITLE:Imaginary test person\n"
          + "EMAIL;type=INTERNET;type=WORK;type=pref:johnDoe@example.org\n"
          + "TEL;type=WORK;type=pref:+1 617 555 1212\n"
          + "TEL;type=WORK:+1 (617) 555-1234\n"
          + "TEL;type=CELL:+1 781 555 1212\n"
          + "TEL;type=HOME:+1 202 555 1212\n"
          + "item1.ADR;type=WORK:;;2 Enterprise Avenue;Worktown;NY;01111;USA\n"
          + "item1.X-ABADR:us\n"
          + "item2.ADR;type=HOME;type=pref:;;3 Acacia Avenue;Hoemtown;MA;02222;USA\n"
          + "item2.X-ABADR:us\n"
          + "NOTE:John Doe has a long and varied history\\, being documented on more police files that anyone else. Reports of his death are alas numerous.\n"
          + "item3.URL;type=pref:http\\://www.example/com/doe\n"
          + "item3.X-ABLabel:_$!<HomePage>!$_\n"
          + "item4.URL:http\\://www.example.com/Joe/foaf.df\n"
          + "item4.X-ABLabel:FOAF\n"
          + "item5.X-ABRELATEDNAMES;type=pref:Jane Doe\n"
          + "item5.X-ABLabel:_$!<Friend>!$_\n"
          + "CATEGORIES:Work,Test group\n"
          + "X-ABUID:5AD380FD-B2DE-4261-BA99-DE1D1DB52FBE\\:ABPerson\n"
          + "END:VCARD\n"
          + "begin:VCARD\n"
          + "VERSION:3.0\n"
          + "source:ldap://cn=bjorn%20Jensen, o=university%20of%20Michigan, c=US\n"
          + "name:Bjorn Jensen\n"
          + "fn:Bj=F8rn Jensen\n"
          + "n:Jensen;Bj=F8rn\n"
          + "email;type=internet:bjorn@umich.edu\n"
          + "tel;type=work,voice,msg:+1 313 747-4454\n"
          + "key;type=x509;encoding=B:dGhpcyBjb3VsZCBiZSAKbXkgY2VydGlmaWNhdGUK\n"
          + "end:VCARD\n"
          + "begin:vcard\n"
          + "VERSION:3.0\n"
          + "source:ldap://cn=Meister%20Berger,o=Universitaet%20Goerlitz,c=DE\n"
          + "name:Meister Berger\n"
          + "fn:Meister Berger\n"
          + "n:Berger;Meister\n"
          + "bday;value=date:1963-09-21\n"
          + "o:Universit=E6t G=F6rlitz\n"
          + "title:Mayor\n"
          + "title;language=de;value=text:Burgermeister\n"
          + "note:The Mayor of the great city of\n"
          + "  Goerlitz in the great country of Germany.\n"
          + "email;internet:mb@goerlitz.de\n"
          + "tel;type=fax,voice,msg:+49 3581 123456\n"
          + "key;type=X509;encoding=b:MIICajCCAdOgAwIBAgICBEUwDQYJKoZIhvcNAQEEBQ\n"
          + " AwdzELMAkGA1UEBhMCVVMxLDAqBgNVBAoTI05ldHNjYXBlIENvbW11bmljYXRpb25zI\n"
          + " ENvcnBvcmF0aW9uMRwwGgYDVQQLExNJbmZvcm1hdGlvbiBTeXN0ZW1zMRwwGgYDVQQD\n"
          + " ExNyb290Y2EubmV0c2NhcGUuY29tMB4XDTk3MDYwNjE5NDc1OVoXDTk3MTIwMzE5NDc\n"
          + " 1OVowgYkxCzAJBgNVBAYTAlVTMSYwJAYDVQQKEx1OZXRzY2FwZSBDb21tdW5pY2F0aW\n"
          + " 9ucyBDb3JwLjEYMBYGA1UEAxMPVGltb3RoeSBBIEhvd2VzMSEwHwYJKoZIhvcNAQkBF\n"
          + " hJob3dlc0BuZXRzY2FwZS5jb20xFTATBgoJkiaJk/IsZAEBEwVob3dlczBcMA0GCSqG\n"
          + " SIb3DQEBAQUAA0sAMEgCQQC0JZf6wkg8pLMXHHCUvMfL5H6zjSk4vTTXZpYyrdN2dXc\n"
          + " oX49LKiOmgeJSzoiFKHtLOIboyludF90CgqcxtwKnAgMBAAGjNjA0MBEGCWCGSAGG+E\n"
          + " IBAQQEAwIAoDAfBgNVHSMEGDAWgBT84FToB/GV3jr3mcau+hUMbsQukjANBgkqhkiG9\n"
          + " w0BAQQFAAOBgQBexv7o7mi3PLXadkmNP9LcIPmx93HGp0Kgyx1jIVMyNgsemeAwBM+M\n"
          + " SlhMfcpbTrONwNjZYW8vJDSoi//yrZlVt9bJbs7MNYZVsyF1unsqaln4/vy6Uawfg8V\n"
          + " UMk1U7jt8LYpo4YULU7UZHPYVUaSgVttImOHZIKi4hlPXBOhcUQ==\n"
          + "end:vcard\n"
          + "BEGIN:VCARD\n"
          + "FN:Rene van der Harten\n"
          + "N:van der Harten;Rene;J.;Sir;R.D.O.N.\n"
          + "SORT-STRING:Harten\n"
          + "END:VCARD\n"
          + "BEGIN:VCARD\n"
          + "FN:Robert Pau Shou Chang\n"
          + "N:Pau;Shou Chang;Robert\n"
          + "SORT-STRING:Pau\n"
          + "END:VCARD\n"
          + "";

  static String RDF_TEST_DATA1 =
      "@prefix : <#>.\n"
          + "@prefix n: <http://www.w3.org/2006/vcard/ns#>.\n"
          + "\n"
          + ":id1543700021614 n:value <mailto:a@b.com>.\n"
          + "\n"
          + ":this\n"
          + "    a n:Individual;\n"
          + "    n:fn \"Cool Kid 1\";\n"
          + "    n:hasEmail :id1543700021614;\n"
          + "    n:note \"This is a note for Cool Kid 1\";\n"
          + "    n:organization-name \"Org1\".";
}
