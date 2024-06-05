package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;

/** FavoriteInfo about a {@code MediaItem}. */
public class FavoriteInfo implements Serializable {

  @JsonProperty("favorite")
  private boolean favorited;

  /* the most recent time when the favorite state was changed. */
  @JsonProperty("lastUpdateTime")
  private Date lastUpdateTime;

  public FavoriteInfo(){
  }

  public FavoriteInfo(boolean favorited, Date lastUpdateTime)
  {
    this.favorited = favorited;
    this.lastUpdateTime = lastUpdateTime;
  }

  /* whether is favorited as of the last updated time */
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
}