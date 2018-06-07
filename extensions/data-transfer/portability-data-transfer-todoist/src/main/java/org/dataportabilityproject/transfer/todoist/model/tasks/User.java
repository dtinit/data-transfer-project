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

package org.dataportabilityproject.transfer.todoist.model.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
  @JsonProperty("id")
  private int id;

  @JsonProperty("token")
  private String token;

  @JsonProperty("email")
  private String email;

  @JsonProperty("full_name")
  private String fullName;

  @JsonProperty("inbox_project")
  private int inboxProject;

  @JsonProperty("tz_info")
  private Object tzInfo;

  @JsonProperty("start_page")
  private String startPage;

  @JsonProperty("start_day")
  private int startDay;

  @JsonProperty("next_week")
  private int nextWeek;

  @JsonProperty("time_format")
  private int time_format;

  @JsonProperty("date_format")
  private int dateFormat;

  @JsonProperty("sort_order")
  private int sortOrder;

  @JsonProperty("default_reminder")
  private String defaultReminder;

  @JsonProperty("auto_reminder")
  private int autoReminder;

  @JsonProperty("mobile_number")
  private String mobileNumber;

  @JsonProperty("mobile_host")
  private String mobileHost;

  @JsonProperty("completed_count")
  private int completedCount;

  @JsonProperty("completed_today")
  private int completedToday;

  @JsonProperty("karma")
  private int karma;

  @JsonProperty("karma_trend")
  private String karmaTrend;

  @JsonProperty("is_premium")
  private String isPremium;

  @JsonProperty("premium_until")
  private String premiumUntil;

  @JsonProperty("is_biz_admin")
  private String isBizAdmin;

  @JsonProperty("business_account_id")
  private int businessAccountId;

  @JsonProperty("image_id")
  private String imageId;

  @JsonProperty("avatar_small")
  private String avatarSmall;

  @JsonProperty("avatar_medium")
  private String avatarMedium;

  @JsonProperty("avatar_big")
  private String avatarBig;

  @JsonProperty("avatar_s640")
  private String avatarS640;

  @JsonProperty("theme")
  private int theme;

  @JsonProperty("features")
  private Object features;

  @JsonProperty("join_date")
  private String joinDate;
}
