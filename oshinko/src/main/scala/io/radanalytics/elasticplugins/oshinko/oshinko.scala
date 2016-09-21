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

package io.radanalytics.elasticplugins.oshinko

import io.radanalytics.elasticplugins.Service

import org.slf4j.{Logger, LoggerFactory}

import dispatch._
import Defaults._
import scala.util.{ Try, Failure, Success }
import scala.concurrent.duration._
import scala.concurrent.Await

class OshinkoService extends Service {
  val successCode = 202

  val minWorkers = 1
  val relMinDelta = 0.1
  val coresPerWorker = 1

  def relDiff(newWorkers: Int, curWorkers: Int) = {
    val nw = newWorkers.toDouble
    val cw = curWorkers.toDouble
    math.abs(nw - cw) / math.max(1.0, cw)
  }

  def request(
    curWorkers: Int,
    curCores: Int,
    usedCores: Int,
    curExecutors: Int,
    reqExecutors: Int): Try[Int] = {
    // Assming one executor per container (the default behavior, also desirable)
    val surplusWorkers = math.max(0, curCores - usedCores) / coresPerWorker
    val reqExec = if (reqExecutors > 999999 || reqExecutors < 0) math.max(minWorkers, curWorkers * 2) else reqExecutors
    val deficitWorkers = math.max(0, reqExec - curExecutors)
    val newWorkers = math.max(minWorkers, curWorkers + (deficitWorkers - surplusWorkers))
    logWarning(s"surplus= $surplusWorkers  deficit= $deficitWorkers  new= $newWorkers  diff= ${relDiff(newWorkers, curWorkers)}")
    if (relDiff(newWorkers, curWorkers) < relMinDelta && curWorkers >= minWorkers) {
      logWarning(s"No change to current cluster size")
      Success(curWorkers)
    } else {
      // otherwise resize to new number of workers
      logWarning(s"Changing cluster size to $newWorkers")
      val status = for(
        hostName <- Try { sys.env("OSHINKO_REST_HOST") } ;
        portStr <- Try { sys.env("OSHINKO_REST_PORT") } ;
        port <- Try { portStr.toInt } ;
        cluster <- Try { sys.env("OSHINKO_SPARK_CLUSTER") } ;
        put <- Try {
          val uri = host(hostName, port) / "clusters" / cluster
          uri.PUT.setContentType("application/json", "UTF-8")
          } ;
        body <- Try {
            s"""{"name": "${cluster}", "masterCount": 1, "workerCount": $newWorkers}"""
        };
        res <- Try {
          logWarning(s"""Request url= "${put.url}" body= "$body" """)
          Await.result(Http(put << body), Duration(5, SECONDS))
        }
      ) yield {
        (res.getStatusCode(), res.getResponseBody())
      }
      status match {
        case Failure(e) =>
          Failure(e)
        case Success((code, body)) if (code != successCode) =>
          logError(s"Oshinko API request returned with error: code= $code  body= $body")
          Failure(new Exception(s"Oshinko worker request failed with code $code"))
        case Success(_) =>
          Success(newWorkers)
      }
    }
  }
}

object OshinkoService {
}
