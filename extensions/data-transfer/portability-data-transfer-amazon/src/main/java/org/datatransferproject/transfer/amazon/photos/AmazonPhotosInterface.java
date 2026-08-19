/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos;

import org.datatransferproject.transfer.amazon.photos.model.AmazonPhotosNode;

import java.io.File;
import java.io.IOException;

/** Interface for Amazon Photos API operations used during data transfer. */
public interface AmazonPhotosInterface {

  /**
   * Resolves regional API endpoints. Must be called before any other method.
   *
   * @throws IOException if the endpoint API is unreachable or returns an error
   */
  void resolveEndpoints() throws IOException;

  /**
   * Creates an album in Amazon Photos.
   *
   * @param name album name
   * @return the created node with id and name
   * @throws IOException on API errors (4xx/5xx)
   */
  AmazonPhotosNode createAlbum(String name) throws IOException;

  /**
   * Uploads content (photo or video) to Amazon Photos.
   * Uses multipart upload for files exceeding the threshold, otherwise simple multiform upload.
   * If albumId is provided, the content is linked to the album atomically.
   *
   * @param fileName the content filename
   * @param fileContent the file to upload
   * @param md5Hex hex-encoded MD5 of the file content
   * @param fileSize size in bytes
   * @param fallbackContentDate fallback content date (ISO 8601) if EXIF is unavailable
   * @param isFavorite whether to mark as favorite
   * @param albumId album to link the content to, or null for no album
   * @return the created node with id and name
   * @throws IOException on API errors (4xx/5xx)
   */
  AmazonPhotosNode uploadContent(String fileName, File fileContent,
                                 String md5Hex, long fileSize, String fallbackContentDate,
                                 boolean isFavorite, String albumId) throws IOException;


}
