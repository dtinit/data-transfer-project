/*
 * Copyright 2024 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.constants;

public class AppleMusicConstants {

    // Maximum number of playlists to import in a single request
    public static final Integer MAX_NEW_PLAYLIST_REQUESTS = Integer.parseInt(System.getProperty("AppleMusicImporter.MAX_NEW_PLAYLIST_REQUESTS", "100"));

    // Maximum number of playlist items to import in a single request
    public static final Integer MAX_NEW_PLAYLIST_ITEM_REQUESTS = Integer.parseInt(System.getProperty("AppleMusicImporter.MAX_NEW_PLAYLIST_ITEM_REQUESTS", "100"));

    public static final String PLAYLISTS_COUNT_DATA_NAME = "playlistsCount";

    public static final String PLAYLIST_ITEMS_COUNT_DATA_NAME = "playlistItemsCount";

}
