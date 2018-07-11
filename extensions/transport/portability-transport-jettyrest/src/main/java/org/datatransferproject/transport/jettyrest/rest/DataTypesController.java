/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.datatransferproject.transport.jettyrest.rest;

import org.datatransferproject.api.action.Action;
import org.datatransferproject.types.client.datatype.DataTypes;
import org.datatransferproject.types.client.datatype.GetDataTypes;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/** Returns the available data types supported by the system, e.g. contacts, photos, etc. */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/datatypes")
public class DataTypesController {
  private static final GetDataTypes REQUEST = new GetDataTypes();

  private Action<GetDataTypes, DataTypes> action;

  public DataTypesController(Action<GetDataTypes, DataTypes> action) {
    this.action = action;
  }

  @GET
  public DataTypes dataTypes() {
    return action.handle(REQUEST);
  }
}
