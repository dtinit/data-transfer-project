package org.datatransferproject.transfer.microsoft;

import org.datatransferproject.types.common.models.TransmogrificationConfig;

// This class defines transmogrification paramaters for Microsoft imports
public class MicrosoftTransmogrificationConfig extends TransmogrificationConfig {

  /**
   * OneDrive has forbidden characters for file names: https://support.office.com/en-us/article/invalid-file-names-and-file-types-in-onedrive-onedrive-for-business-and-sharepoint-64883a5d-228e-48f5-b3d2-eb39e07630fa#invalidcharacters
   */
  private static final String PHOTO_TITLE_FORBIDDEN_CHARACTERS = "~\"#%&*:<>?/\\{|}";
  private static final String ALBUM_NAME_FORBIDDEN_CHARACTERS = ".~\"#%&*:<>?/\\{|}";

  /**
   * We need to override the methods
   */
  @Override
  public String getPhotoTitleForbiddenCharacters() {
    return PHOTO_TITLE_FORBIDDEN_CHARACTERS;
  }

  @Override
  public String getAlbumNameForbiddenCharacters() {
    return ALBUM_NAME_FORBIDDEN_CHARACTERS;
  }
}
