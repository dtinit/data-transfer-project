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

package org.dataportabilityproject.transfer.rememberthemilk.model.tasks;

import com.google.api.client.util.Key;
import com.google.common.base.MoreObjects;

/* A Transaction Object as detailed in https://www.rememberthemilk.com/services/api/timelines.rtm
 * A transaction is returned in each state changing request such as ListAdd and TaskAdd*/
public class Transaction {
  // The id of the transaction
  @Key("@id")
  public int id;
  // Whether this transaction is undoable or not.
  @Key("undoable")
  public int undoable;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).add("undoable", undoable).toString();
  }
}
