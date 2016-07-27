/*
 * Copyright (c) 2016 Red Hat, Inc.
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

package com.redhat.daikon.elasticplugins.oshinko

import com.redhat.daikon.elasticplugins.Service

import dispatch._
import Defaults._
import scala.util.{ Try, Failure, Success }
import scala.concurrent.duration._
import scala.concurrent.Await

class OshinkoService extends Service {
  val successCode = 202

  def request(newTotalWorkers: Int): Try[Int] = {
    val status = for(
      url <- Try { sys.env("OSHINKO_REST_URL") } ;
      portStr <- Try { sys.env("OSHINKO_REST_PORT") } ;
      port <- Try { portStr.toInt } ;
      cluster <- Try { sys.env("OSHINKO_SPARK_CLUSTER") } ;
      put <- Try {
        val h = host(url, port) / "clusters" / cluster
        h.POST <:< Map(
          "name" -> cluster,
          "masterCount" -> "1",
          "workerCount" -> newTotalWorkers.toString
          )
        } ;
      putURL <- Try { put.url } ;
      res <- Try { Await.result(Http(put), Duration(5, SECONDS)) }
    ) yield {
      println(s"""REST URL = "$putURL" """)
      res.getStatusCode()
    }
    status match {
      case Failure(e) =>
        Failure(e)
      case Success(code) if (code != successCode) =>
        Failure(new Exception(s"WARNING - oshinko worker scaleout request failed with code $code"))
      case Success(_) =>
        Success(newTotalWorkers)
    }
  }
}

object OshinkoService {
}
