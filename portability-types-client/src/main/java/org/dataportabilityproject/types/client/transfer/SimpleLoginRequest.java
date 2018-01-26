package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SimpleLoginRequest {
  private final String username;
  private final String password;

  @JsonCreator
  public SimpleLoginRequest(
      @JsonProperty() String username,
      @JsonProperty() String password){
    this.username = username;
    this.password = password;
  }

  @ApiModelProperty
  public String getPassword() {
    return password;
  }

  @ApiModelProperty
  public String getUsername() {
    return username;
  }
}
