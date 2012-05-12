/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vertx.java.examples.httpperf;

import org.vertx.java.core.Handler;
import org.vertx.java.core.SimpleHandler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.deploy.Verticle;

import java.util.ArrayDeque;
import java.util.Deque;

public class PerfClient extends Verticle implements Handler<HttpClientResponse> {

  private HttpClient client;

  private long start;

  private int count = 0;

  // This determines the degree of pipelining
  private static final int CREDITS_BATCH = 2000;

  // Number of connections to create
  private static final int MAX_CONNS = 10;

  private int requestCredits = CREDITS_BATCH;

  private EventBus eb;

  private final Deque<Long> startTimeMs = new ArrayDeque<Long>();

  private static final int NUM_DELTAS = 10000;
  private final long[] deltas = new long[NUM_DELTAS];
  private int nextDelta = 0;
  boolean deltasFull = false;

  public void handle(HttpClientResponse response) {
    if (response.statusCode != 200) {
      throw new IllegalStateException("Invalid response");
    }
    response.endHandler(new SimpleHandler() {
      public void handle() {
        long start = startTimeMs.remove();
        long now = System.currentTimeMillis();
        long delta = now - start;;

        deltas[nextDelta] = delta;
        nextDelta += 1;
        if(nextDelta >= NUM_DELTAS) {
          nextDelta = 0;
          deltasFull = true;
        }

        count++;
        if (count % 2000 == 0) {
          eb.send("rate-counter", count);

          if(deltasFull) {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            double sum = 0;
            for(long d : deltas) {
              sum += d;
              min = Math.min(min, d);
              max = Math.max(max, d);
            }
            eb.send("delta-min", min);
            eb.send("delta-max", max);
            eb.send("delta-avg", sum/(double)NUM_DELTAS);
          }

          count = 0;
        }
        requestCredits++;
        makeRequest();
      }
    });
  }

  public void start() {
    eb = vertx.eventBus();
    client = vertx.createHttpClient().setPort(8080).setHost("localhost").setMaxPoolSize(MAX_CONNS);
    makeRequest();
  }

  private void makeRequest() {
    if (start == 0) {
      start = System.currentTimeMillis();
    }
    while (requestCredits > 0) {
      startTimeMs.add(System.currentTimeMillis());
      client.getNow("/", this);
      requestCredits--;
    }
  }

}
