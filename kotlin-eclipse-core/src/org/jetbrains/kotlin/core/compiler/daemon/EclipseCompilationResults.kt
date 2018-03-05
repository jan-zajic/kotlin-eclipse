package org.jetbrains.kotlin.core.compiler.daemon

import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import java.rmi.server.UnicastRemoteObject
import java.io.Serializable
import java.rmi.RemoteException

internal open class EclipseCompilationResults : CompilationResults,
   UnicastRemoteObject(SOCKET_ANY_FREE_PORT, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory) {
	
	@Throws(RemoteException::class)
	override fun add(compilationResultCategory: Int, value: Serializable) {
	}
	
}