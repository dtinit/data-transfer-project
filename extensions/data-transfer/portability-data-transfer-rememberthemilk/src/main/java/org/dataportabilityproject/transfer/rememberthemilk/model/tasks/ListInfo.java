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

public class ListInfo {

  @Key("@id")
  public int id;

  @Key("@name")
  public String name;

  @Key("@deleted")
  public int deleted;

  @Key("@locked")
  public int locked;

  @Key("@archived")
  public int archived;

  @Key("@position")
  public int position;

  @Key("@smart")
  public int smart;

  @Key("@sort_order")
  public int sort_order;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("name", name)
        .add("deleted", deleted)
        .add("locked", locked)
        .add("archived", archived)
        .add("position", position)
        .add("smart", smart)
        .add("sort_order", sort_order)
        .toString();
  }
}
