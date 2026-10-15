// Copyright (C) 2026  Timofei Pushkin
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package io.github.timpushkin.tapok.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

/**
 * Allows one party to send a request and another to handle it. There can be at most one member on
 * each side.
 */
interface RequestBroker<S, R> {
    /**
     * Publishes a request and suspends until it is served. Cancelling the caller cancels the
     * request.
     *
     * @param payload information accompanying the request.
     *
     * @throws IllegalStateException if a request is already active.
     */
    suspend fun request(payload: S): R

    /**
     * Suspends indefinitely, processing incoming requests using the [handler].
     *
     * If a request is canceled, its [handler] is automatically canceled as well.
     *
     * If [handler] throws an exception, it is delivered to the requester and not propagated out of
     * this method.
     *
     * @throws IllegalStateException if there is a server already.
     */
    suspend fun serve(handler: suspend (S) -> R): Nothing
}

/**
 * In-process implementation.
 */
class InMemoryRequestBroker<S, R> : RequestBroker<S, R> {
    private val state = MutableStateFlow(State<S, R>(serverPresent = false, request = null))

    override suspend fun request(payload: S): R {
        val request = Request<S, R>(payload)

        state.update {
            check(it.request == null) { "A request is already open" }
            it.copy(request = request)
        }

        try {
            return request.delivery.await()
        } finally {
            // Check before null-ing in case the server left and null-ed
            state.update { if (it.request === request) it.copy(request = null) else it }
        }
    }

    override suspend fun serve(handler: suspend (S) -> R): Nothing {
        state.update {
            check(!it.serverPresent) { "A server is already present" }
            it.copy(serverPresent = true)
        }
        try {
            state.collectLatest {
                val request = it.request ?: return@collectLatest
                try {
                    val result = handler(request.payload)
                    request.delivery.complete(result)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    request.delivery.completeExceptionally(e)
                }
            }
            awaitCancellation() // Unreachable but required by the compiler to return Nothing
        } finally {
            state.update { it.copy(serverPresent = false) }
        }
    }

    private data class State<S, R>(val serverPresent: Boolean, val request: Request<S, R>?)

    private class Request<S, R>(val payload: S) {
        val delivery = CompletableDeferred<R>()
    }
}
