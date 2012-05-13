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

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.deploy.Verticle;

public class PerfAsyncFileServer extends Verticle {

  public void start() {
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        vertx.fileSystem().open("httpperf/foo.html", new AsyncResultHandler<AsyncFile>() {
          public void handle(AsyncResult<AsyncFile> ar) {
            final AsyncFile af = ar.result;
            ReadStream rs = af.getReadStream();

            req.response.setChunked(true);
            req.response.putHeader("Content-Type", "text/html");

            rs.dataHandler(new Handler<Buffer>() {
              public void handle(Buffer b) {
                req.response.write(b);
              }
            });

            rs.endHandler(new Handler<Void>() {
              public void handle(Void v) {
                af.close();
                req.response.end();
              }
            });
          }
        });
      }
    }).listen(8080, "10.112.1.245");
  }
}
