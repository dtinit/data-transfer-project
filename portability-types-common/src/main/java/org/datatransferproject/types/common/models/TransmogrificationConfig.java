package org.datatransferproject.types.common.models;

/**
    This class is meant to be extended with custom logic for each service.
    These defaults represent no-chnage transmogrifications.  
*/
public class TransmogrificationConfig {
    private static final String    PHOTO_TITLE_FORBIDDEN_CHARACTERS    = "";
    private static final char      PHOTO_TITLE_REPLACEMENT_CHARACTER   = '_';
    private static final int       PHOTO_TITLE_MAX_LENGTH              = -1;

    private static final String    ALBUM_NAME_FORBIDDEN_CHARACTERS     = "";
    private static final char      ALBUM_NAME_REPLACEMENT_CHARACTER    = '_';
    private static final int       ALBUM_NAME_MAX_LENGTH               = -1;
    private static final boolean   ALBUM_ALLOW_ROOT_PHOTOS             = true;
    private static final int       ALBUM_MAX_SIZE                      = -1;

    public String getPhotoTitleForbiddenCharacters() {
        return PHOTO_TITLE_FORBIDDEN_CHARACTERS;
    }

    public char getPhotoTitleReplacementCharater() {
        return PHOTO_TITLE_REPLACEMENT_CHARACTER;
    }

    public int getPhotoTitleMaxLength() {
        return PHOTO_TITLE_MAX_LENGTH;
    }

    public String getAlbumNameForbiddenCharacters() {
        return ALBUM_NAME_FORBIDDEN_CHARACTERS;
    }

    public char getAlbumNameReplacementCharacter() {
        return ALBUM_NAME_REPLACEMENT_CHARACTER;
    }

    public int getAlbumNameMaxLength() {
        return ALBUM_NAME_MAX_LENGTH;
    }

    public boolean getAlbumAllowRootPhotos() {
        return ALBUM_ALLOW_ROOT_PHOTOS;
    }

    public int getAlbumMaxSize() {
        return ALBUM_MAX_SIZE;
    }
}
