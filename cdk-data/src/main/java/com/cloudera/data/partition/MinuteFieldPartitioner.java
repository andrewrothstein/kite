/**
 * Copyright 2013 Cloudera Inc.
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
package com.cloudera.data.partition;

import com.google.common.annotations.Beta;
import java.util.Calendar;

@Beta
public class MinuteFieldPartitioner extends CalendarFieldPartitioner {
  public MinuteFieldPartitioner(String sourceName, String name) {
    super(sourceName, name, Calendar.MINUTE, 60);
  }
}
