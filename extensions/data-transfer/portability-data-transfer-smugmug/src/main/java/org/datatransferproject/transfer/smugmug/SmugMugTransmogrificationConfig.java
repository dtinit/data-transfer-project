package org.datatransferproject.transfer.smugmug;

import org.datatransferproject.types.common.models.TransmogrificationConfig;

// This class defines transmogrification paramaters for SmugMug imports
public class SmugMugTransmogrificationConfig extends TransmogrificationConfig {
	// Smugmug doesn't allow photos to exist outside of a folder
	private final boolean 	ALBUM_ALLOW_ROOT_PHOTOS				= false;
	// Album size specified here:
  	// https://github.com/google/data-transfer-project/pull/805/files
	private final int       ALBUM_MAX_SIZE						= 5000;
}