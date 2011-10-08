package org.plus.score;

/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import com.google.api.client.extensions.appengine.http.urlfetch.UrlFetchTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Strings;

import java.io.IOException;

/**
 * @author Jennifer Murphy
 */
public class Util {
  public static final JsonFactory JSON_FACTORY = new GsonFactory();
  public static final HttpTransport TRANSPORT = new UrlFetchTransport();

  /**
   * A simple HTML tag stripper to prepare HTML for rendering. This is a
   * quick and dirty solution so please do not depend on it to prevent XSS
   * attacks.
   *
   * @return The same string with all xml/html tags stripped.
   */
  public static String stripTags(String input) {
    return input.replaceAll("\\<[^>]*>","");
  }

  /**
   * Extract the request error details from a HttpResponseException
   * @param e An HttpResponseException that contains error details
   * @return The String representation of all errors that caused the
   *     HttpResponseException
   * @throws java.io.IOException when the response cannot be parsed or stringified
   */
  public static String extractError(HttpResponseException e) throws IOException {
    if (!Json.CONTENT_TYPE.equals(e.getResponse().getContentType())) {
      return e.getResponse().parseAsString();
    }

    GoogleJsonError errorResponse =
            GoogleJsonError.parse(JSON_FACTORY, e.getResponse());
    StringBuilder errorReportBuilder = new StringBuilder();

    errorReportBuilder.append(errorResponse.code);
    errorReportBuilder.append(" Error: ");
    errorReportBuilder.append(errorResponse.message);

    for (GoogleJsonError.ErrorInfo error : errorResponse.errors) {
      errorReportBuilder.append(JSON_FACTORY.toString(error));
      errorReportBuilder.append(Strings.LINE_SEPARATOR);
    }
    return errorReportBuilder.toString();
  }
}