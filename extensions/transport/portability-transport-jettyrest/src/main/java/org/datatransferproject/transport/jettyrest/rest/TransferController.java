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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.types.client.transfer.*;

/** */
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/transfer")
public class TransferController {
  private final Action<CreateTransferJob, TransferJob> createJobAction;
  private final Action<GenerateServiceAuthData, ServiceAuthData> generateAuthDataAction;
  private final Action<ReserveWorker, ReservedWorker> reserveWorkerAction;
  private final Action<GetReservedWorker, ReservedWorker> getReservedWorkerAction;
  private final Action<StartTransferJob, TransferJob> startJobAction;
  private final Action<GetTransferJob, TransferJob> getJobAction;

  public TransferController(
      Action<CreateTransferJob, TransferJob> createJobAction,
      Action<GenerateServiceAuthData, ServiceAuthData> generateAuthDataAction,
      Action<ReserveWorker, ReservedWorker> reserveWorkerAction,
      Action<GetReservedWorker, ReservedWorker> getReservedWorkerAction,
      Action<StartTransferJob, TransferJob> startJobAction,
      Action<GetTransferJob, TransferJob> getJobAction) {
    this.createJobAction = createJobAction;
    this.generateAuthDataAction = generateAuthDataAction;
    this.reserveWorkerAction = reserveWorkerAction;
    this.getReservedWorkerAction = getReservedWorkerAction;
    this.startJobAction = startJobAction;
    this.getJobAction = getJobAction;
  }

  @GET
  @Path("{id}")
  public TransferJob getTransferJob(@PathParam("id") String id) {
    return getJobAction.handle((new GetTransferJob(id)));
  }

  @POST
  public TransferJob createTransferJob(CreateTransferJob request) {
    return createJobAction.handle(request);
  }

  @POST
  @Path("{id}/generate")
  public ServiceAuthData generate(GenerateServiceAuthData generate) {
    return generateAuthDataAction.handle(generate);
  }

  @POST
  @Path("worker/{id}")
  public ReservedWorker reserveWorker(ReserveWorker reserveWorker) {
    return reserveWorkerAction.handle(reserveWorker);
  }

  @GET
  @Path("worker/{id}")
  public ReservedWorker getWorker(@PathParam("id") String id) {
    return getReservedWorkerAction.handle((new GetReservedWorker(id)));
  }

  @POST
  @Path("{id}/start")
  public TransferJob startTransferJob(StartTransferJob request) {
    return startJobAction.handle(request);
  }
}
