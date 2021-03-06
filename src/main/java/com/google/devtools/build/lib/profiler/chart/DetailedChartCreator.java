// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.profiler.chart;

import com.google.devtools.build.lib.profiler.ProfileInfo;
import com.google.devtools.build.lib.profiler.ProfileInfo.CriticalPathEntry;
import com.google.devtools.build.lib.profiler.ProfileInfo.Task;
import com.google.devtools.build.lib.profiler.ProfilePhaseStatistics;
import com.google.devtools.build.lib.profiler.ProfilerTask;

import java.util.EnumSet;
import java.util.List;

/**
 * Implementation of {@link ChartCreator} that creates Gantt Charts that contain
 * bars for all tasks in the profile.
 */
public class DetailedChartCreator implements ChartCreator {

  /** The data of the profiled build. */
  private final ProfileInfo info;

  /**
   * Statistics of the profiled build. This is expected to be a formatted
   * string, ready to be printed out.
   */
  private final List<ProfilePhaseStatistics> statistics;

  /**
   * Creates the chart creator.
   *
   * @param info the data of the profiled build
   * @param statistics Statistics of the profiled build. This is expected to be
   *        a formatted string, ready to be printed out.
   */
  public DetailedChartCreator(ProfileInfo info, List<ProfilePhaseStatistics> statistics) {
    this.info = info;
    this.statistics = statistics;
  }

  @Override
  public Chart create() {
    Chart chart = new Chart(info.comment, statistics);
    CommonChartCreator.createCommonChartItems(chart, info);
    createTypes(chart);

    // calculate the critical path
    EnumSet<ProfilerTask> typeFilter = EnumSet.noneOf(ProfilerTask.class);
    CriticalPathEntry criticalPath = info.getCriticalPath(typeFilter);
    info.analyzeCriticalPath(typeFilter, criticalPath);

    for (Task task : info.allTasksById) {
      String label = task.type.description + ": " + task.getDescription();
      ChartBarType type = chart.lookUpType(task.type.description);
      long stop = task.startTime + task.duration;
      CriticalPathEntry entry = null;

      // for top level tasks, check if they are on the critical path
      if (task.parentId == 0 && criticalPath != null) {
        entry = info.getNextCriticalPathEntryForTask(criticalPath, task);
        // find next top-level entry
        if (entry != null) {
          CriticalPathEntry nextEntry = entry.next;
          while (nextEntry != null && nextEntry.task.parentId != 0) {
            nextEntry = nextEntry.next;
          }
          if (nextEntry != null) {
            // time is start and not stop as we traverse the critical back backwards
            chart.addVerticalLine(task.threadId, nextEntry.task.threadId, task.startTime);
          }
        }
      }

      chart.addBar(task.threadId, task.startTime, stop, type, (entry != null), label);
    }

    return chart;
  }

  /**
   * Creates a {@link ChartBarType} for every known {@link ProfilerTask} and
   * adds it to the chart.
   *
   * @param chart the chart to add the types to
   */
  private void createTypes(Chart chart) {
    for (ProfilerTask task : ProfilerTask.values()) {
      chart.createType(task.description, new Color(task.color));
    }
  }
}
