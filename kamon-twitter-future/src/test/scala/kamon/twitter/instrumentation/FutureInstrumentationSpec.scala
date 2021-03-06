/* ===================================================
 * Copyright © 2016 the kamon project <http://kamon.io/>
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
 * ========================================================== */
package kamon.twitter.instrumentation

import java.util.concurrent.Executors

import org.scalatest.{Matchers, OptionValues, WordSpec}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import com.twitter.util.{Await, FuturePool}
import kamon.Kamon
import kamon.testkit.ContextTesting

class FutureInstrumentationSpec extends WordSpec with Matchers with ScalaFutures with PatienceConfiguration
    with OptionValues with ContextTesting {

  implicit val execContext = Executors.newCachedThreadPool()

  "a Twitter Future created when instrumentation is active" should {
    "capture the active span available when created" which {
      "must be available when executing the future's body" in {

        val context = contextWithLocal("in-future-body")
        val baggageInBody = Kamon.withContext(context) {
          FuturePool(execContext)(Kamon.currentContext().get(StringKey))
        }

        Await.result(baggageInBody) should equal(Some("in-future-body"))
      }

      "must be available when executing callbacks on the future" in {

        val context = contextWithLocal("in-future-transformations")
        val baggageAfterTransformations = Kamon.withContext(context) {
          FuturePool.unboundedPool("Hello Kamon!")
            // The active span is expected to be available during all intermediate processing.
            .map(_.length)
            .flatMap(len ⇒ FuturePool.unboundedPool(len.toString))
            .map(_ ⇒ Kamon.currentContext().get(StringKey))
        }

        Await.result(baggageAfterTransformations) should equal(Some("in-future-transformations"))
      }
    }
  }
}

