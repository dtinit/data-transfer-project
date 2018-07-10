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
package org.datatransferproject.transfer.microsoft.transformer.contacts;

import ezvcard.property.Address;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;

/**
 * Maps from a Graph API physical address resource as defined by:
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/physicaladdress.
 */
public class ToVCardAddressTransformer
    implements BiFunction<Map<String, String>, TransformerContext, Address> {

  @Override
  public Address apply(Map<String, String> addressMap, TransformerContext context) {
    Address address = new Address();
    address.setStreetAddress(addressMap.get("street"));
    address.setLocality(addressMap.get("city"));
    address.setCountry(addressMap.get("countryOrRegion"));
    address.setPostalCode(addressMap.get("postalCode"));
    address.setRegion(addressMap.get("state"));
    return address;
  }
}
