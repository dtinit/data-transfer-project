/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Represents the configuration for a copy job
export class CopyConfiguration {
  public dataType: string;
  public exportService: string;
  public exportAuthUrl: string;
  public importService: string;
  public importAuthUrl: string;
  constructor (
    dataType: string,
    exportService: string,
    exportAuthUrl: string,
    importService: string,
    importAuthUrl: string) {
    this.dataType = dataType;
    this.exportService = exportService;
    this.exportAuthUrl = exportAuthUrl;
    this.importService = importService;
    this.importAuthUrl = importAuthUrl;
  }
}
