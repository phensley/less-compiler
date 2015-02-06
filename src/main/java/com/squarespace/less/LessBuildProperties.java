/**
 * Copyright (c) 2014 SQUARESPACE, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squarespace.less;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * Build-related properties available at runtime.
 */
public class LessBuildProperties {

  private static final String UNDEFINED = "<undefined>";

  private static final String BUILD_PROPERTIES = "build.properties";

  private static final Properties properties = load();

  public static String version() {
    return properties.getProperty("build.version", UNDEFINED);
  }

  public static String date() {
    return properties.getProperty("build.date", UNDEFINED);
  }

  public static String commit() {
    return properties.getProperty("build.commit", UNDEFINED);
  }

  private static Properties load() {
    Properties properties = new Properties();
    try (InputStream in = LessBuildProperties.class.getResourceAsStream(BUILD_PROPERTIES)) {
      properties.load(in);
    } catch (IOException e) {
      System.err.println("Warning: build.properties failed to load: " + e.getMessage());
    }
    return properties;
  }
}
