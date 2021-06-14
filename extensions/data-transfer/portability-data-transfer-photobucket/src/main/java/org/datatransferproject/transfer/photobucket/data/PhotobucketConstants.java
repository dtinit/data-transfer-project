package org.datatransferproject.transfer.photobucket.data;

public final class PhotobucketConstants {
  // Keys
  public static final String PB_KEY = "PB_KEY";
  public static final String PB_SECRET = "PB_SECRET";
  public static final String PB_SERVICE_ID = "Photobucket";

  // Titles and prefixes
  public static final String MAIN_ALBUM_TITLE = "Imported media";
  public static final String ALBUM_TITLE_PREFIX = "Copy of ";

  // Clients
  public static final long ACCESS_TOKEN_EXPIRE_TIME_IN_SECONDS = 3600L;
  public static final String GQL_URL = "https://app.photobucket.com/api/graphql";
  public static final String UPLOAD_URL = "https://app.photobucket.com/api/upload_by_url";

  // Headers
  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String AUTHORIZATION_HEADER = "Authorization";

}
