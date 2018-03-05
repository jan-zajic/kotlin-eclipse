package org.jetbrains.kotlin.core.compiler.daemon

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.server.UnicastRemoteObject

internal open class EclipseCompilerServicesFacadeImpl(
        val messageCollector: MessageCollector,
		val outputsCollector: ((File, List<File>) -> Unit)? = null,
        port: Int = SOCKET_ANY_FREE_PORT
) : UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory),
    CompilerServicesFacadeBase,
    Remote {
	
	override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        messageCollector.reportFromDaemon(outputsCollector, category, severity, message, attachment)
    }

}

internal class EclipseIncrementalCompilerServicesFacadeImpl(
        messageCollector: MessageCollector,
		outputsCollector: ((File, List<File>) -> Unit)? = null,
        port: Int = SOCKET_ANY_FREE_PORT
) : EclipseCompilerServicesFacadeImpl(messageCollector, outputsCollector, port), IncrementalCompilerServicesFacade {
	
	override fun hasAnnotationsFileUpdater(): Boolean = false

    override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        val jvmNames = outdatedClassesJvmNames.map { JvmClassName.byInternalName(it) }
        //TODO: ?
    }

    override fun revert() {
        //TODO: ?
    }

    override fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        //TODO: ?
		return null;
    }

    override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
    	//TODO: ?
    }

    override fun unknownChanges(timestamp: Long) {
       //TODO: ?
    }
	
}

fun MessageCollector.reportFromDaemon(outputsCollector: ((File, List<File>) -> Unit)?, category: Int, severity: Int, message: String?, attachment: Serializable?) {
    val reportCategory = ReportCategory.fromCode(category)

    when (reportCategory) {
        ReportCategory.OUTPUT_MESSAGE -> {
            if (outputsCollector != null) {
                OutputMessageUtil.parseOutputMessage(message.orEmpty())?.let { outs ->
                    outs.outputFile?.let {
                        outputsCollector.invoke(it, outs.sourceFiles.toList())
                    }
                }
            }
            else {
                report(CompilerMessageSeverity.OUTPUT, message!!)
            }
        }
        ReportCategory.EXCEPTION -> {
            report(CompilerMessageSeverity.EXCEPTION, message.orEmpty())
        }
        ReportCategory.COMPILER_MESSAGE -> {
            val compilerSeverity = when (ReportSeverity.fromCode(severity)) {
                ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
                ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
                ReportSeverity.INFO -> CompilerMessageSeverity.INFO
                ReportSeverity.DEBUG -> CompilerMessageSeverity.LOGGING
                else -> throw IllegalStateException("Unexpected compiler message report severity $severity")
            }
            if (message != null) {
                report(compilerSeverity, message, attachment as? CompilerMessageLocation)
            }
            else {
                reportUnexpected(category, severity, message, attachment)
            }
        }
        ReportCategory.DAEMON_MESSAGE,
        ReportCategory.IC_MESSAGE -> {
            if (message != null) {
                report(CompilerMessageSeverity.LOGGING, message)
            }
            else {
                reportUnexpected(category, severity, message, attachment)
            }
        }
        else -> {
            reportUnexpected(category, severity, message, attachment)
        }
    }
}

private fun MessageCollector.reportUnexpected(category: Int, severity: Int, message: String?, attachment: Serializable?) {
    val compilerMessageSeverity = when (ReportSeverity.fromCode(severity)) {
        ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
        ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
        ReportSeverity.INFO -> CompilerMessageSeverity.INFO
        else -> CompilerMessageSeverity.LOGGING
    }

    report(compilerMessageSeverity, "Unexpected message: category=$category; severity=$severity; message='$message'; attachment=$attachment")
}