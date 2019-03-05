package org.datatransferproject.transfer.deezer.model;

/**
 * Response format for user/me/platlists, see:
 * https://developers.deezer.com/api/explorer?url=user/me
 */
public class PlaylistsResponse {
  private PlaylistSummary[] data;

  public PlaylistSummary[] getData() {
    return data;
  }
}
