/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.facebook.utils;

import com.restfb.exception.FacebookOAuthException;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.facebook.exceptions.FacebookSessionInvalidatedException;
import org.datatransferproject.transfer.facebook.exceptions.FacebookUnconfirmedUserException;

public class FacebookTransferUtils {

  public static FacebookOAuthException handleFacebookOAuthException(FacebookOAuthException e)
      throws CopyExceptionWithFailureReason {
    String message = e.getMessage();
    if (message != null && message.contains("the user is not a confirmed user")) {
      throw new FacebookUnconfirmedUserException(
          "The user account is not confirmed or deactivated", e);
    } else if (message != null && message.contains("code 190, subcode 460")) {
      throw new FacebookSessionInvalidatedException("The user session has been invalidated", e);
    } else {
      return e;
    }
  }
}
