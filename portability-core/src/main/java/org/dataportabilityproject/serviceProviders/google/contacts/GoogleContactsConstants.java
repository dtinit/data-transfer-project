package org.dataportabilityproject.serviceProviders.google.contacts;

class GoogleContactsConstants {

  static final String SELF_RESOURCE = "people/me";

  // List of all fields we want to get from the Google Contacts API
  // NB: this will have to be updated as we support more fields
  static final String PERSON_FIELDS = "emailAddresses,names,phoneNumbers";

  static final int VCARD_PRIMARY_PREF = 1;
  static final String SOURCE_PARAM_NAME_TYPE = "Source_type";
  static final String CONTACT_SOURCE_TYPE = "CONTACT";
}
