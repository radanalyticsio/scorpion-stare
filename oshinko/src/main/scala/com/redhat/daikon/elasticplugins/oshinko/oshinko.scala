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
    val deficitWorkers = math.max(0, reqExecutors - curExecutors)
    val newWorkers = math.max(minWorkers, curWorkers + (deficitWorkers - surplusWorkers))
    logWarning(s"surplus= $surplusWorkers  deficit= $deficitWorkers  new= $newWorkers  diff= ${relDiff(newWorkers,curWorkers)}")
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
          val h = host(hostName, port) / "clusters" / cluster
          h.POST <:< Map(
            "name" -> cluster,
            "masterCount" -> "1",
            "workerCount" -> newWorkers.toString
            )
          } ;
        res <- Try {
          logWarning(s"""REST URL = "${put.url}" """)
          Await.result(Http(put), Duration(5, SECONDS))
        }
      ) yield {
        res.getStatusCode()
      }
      status match {
        case Failure(e) =>
          Failure(e)
        case Success(code) if (code != successCode) =>
          Failure(new Exception(s"WARNING - oshinko worker request failed with code $code"))
        case Success(_) =>
          Success(newWorkers)
      }
    }
  }
}

object OshinkoService {
}
