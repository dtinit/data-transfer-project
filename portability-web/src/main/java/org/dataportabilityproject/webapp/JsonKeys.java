package org.dataportabilityproject.webapp;

/** Keys for returned Json data. */
public class JsonKeys {

  private JsonKeys(){} /** no instantiation */

  public static final String ID_COOKIE_KEY = "e_id";
  public static final String EXPORT_AUTH_DATA_COOKIE_KEY = "ead_id";
  public static final String IMPORT_AUTH_DATA_COOKIE_KEY = "iad_id";

  public static final String DATA_TYPE = "dataType";
  public static final String EXPORT = "export";
  public static final String EXPORT_SERVICE = "exportService";
  public static final String EXPORT_SERVICE_AUTH_EXISTS = "exportServiceAuthExists";
  public static final String EXPORT_AUTH_URL = "exportAuthUrl";
  public static final String IMPORT = "import";
  public static final String IMPORT_SERVICE = "importService";
  public static final String IMPORT_SERVICE_AUTH_EXISTS = "importServiceAuthExists";
  public static final String IMPORT_AUTH_URL = "importAuthUrl";
  public static final String TOKEN = "token";
}
