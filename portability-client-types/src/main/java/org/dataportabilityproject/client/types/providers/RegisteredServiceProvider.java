package org.dataportabilityproject.client.types.providers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A service provider registered in the system.
 */
@ApiModel(description = "A service provider registered in the system")
public class RegisteredServiceProvider {
    private String id;
    private String name;
    private String description;

    private String[] contentTypes;

    @JsonCreator
    public RegisteredServiceProvider(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "description", required = true) String description,
            @JsonProperty(value = "contentTypes", required = true)  String[] contentTypes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contentTypes = contentTypes;
    }

    @ApiModelProperty(value = "The unique service provider id")
    public String getId() {
        return id;
    }

    @ApiModelProperty(value = "The service provider name")
    public String getName() {
        return name;
    }

    @ApiModelProperty(value = "The service provider description")
    public String getDescription() {
        return description;
    }

    @ApiModelProperty(value = "The content types supported by this service provider")
    public String[] getContentTypes() {
        return contentTypes;
    }
}
