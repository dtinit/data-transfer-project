package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;

/** FavoriteInfo about a {@code MediaItem}. */
public class FavoriteInfo implements Serializable {

  @JsonProperty("favorite")
  private boolean favorited;

  /* the most recent time when the favorite state was changed or set. For new items, this field
   * might be set to the item creation time.
  */
  @JsonProperty("lastUpdateTime")
  private Date lastUpdateTime;

  public FavoriteInfo(){
  }

  public FavoriteInfo(boolean favorited, Date lastUpdateTime)
  {
    this.favorited = favorited;
    this.lastUpdateTime = lastUpdateTime;
  }

  /* Expected possibly true IFF lastUpdatedTime field is populated.  */
  public boolean getFavorited() {
    return favorited;
  }

  public Date getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setFavorited(boolean favorited) {
    this.favorited = favorited;
  }

  public void setLastUpdateTime(Date lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public static FavoriteInfo unknown() {
    return new FavoriteInfo(false /*favorited*/, null /*lastUpdateTime*/);
  }

  public static FavoriteInfo unfavoritedAt(Date lastUpdateTime) {
    return new FavoriteInfo(false /*favorited*/, lastUpdateTime );
  }
}