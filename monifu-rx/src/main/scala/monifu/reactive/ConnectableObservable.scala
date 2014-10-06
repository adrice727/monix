/*
 * Copyright (c) 2014 by its authors. Some rights reserved.
 * See the project homepage at
 *
 *     http://www.monifu.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package monifu.reactive

import monifu.concurrent.Cancelable
import monifu.concurrent.atomic.Atomic
import monifu.concurrent.cancelables.BooleanCancelable

/**
 * Represents an [[monifu.reactive.Observable Observable]] that waits for
 * the call to `connect()` before
 * starting to emit elements to its subscriber(s).
 *
 * Useful for converting cold observables into hot observables and thus returned by
 * [[monifu.reactive.Observable.multicast Observable.multicast]].
 */
trait ConnectableObservable[+T] extends Observable[T] {
  /**
   * Starts emitting events to subscribers.
   */
  def connect(): BooleanCancelable
}


object ConnectableObservable {
  /**
   * Builds a [[ConnectableObservable]] for the given observable source
   * and a given [[Subject]].
   */
  def apply[T, R](source: Observable[T], subject: Subject[T, R]): ConnectableObservable[R] =
    new ConnectableObservable[R] {
      private[this] val notCanceled = Atomic(true)

      private[this] val cancelAction =
        BooleanCancelable { notCanceled set false }
      private[this] val notConnected =
        Cancelable { source.takeWhileRefIsTrue(notCanceled).unsafeSubscribe(subject) }

      def connect() = {
        notConnected.cancel()
        cancelAction
      }

      def subscribeFn(observer: Observer[R]): Unit = {
        subject.unsafeSubscribe(observer)
      }
    }
}