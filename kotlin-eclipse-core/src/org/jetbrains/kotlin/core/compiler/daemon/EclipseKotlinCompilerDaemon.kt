package org.jetbrains.kotlin.core.compiler.daemon

import org.eclipse.jdt.launching.JavaRuntime
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KOTLIN_COMPILER_PATH
import org.jetbrains.kotlin.daemon.CompileServiceImpl
import org.jetbrains.kotlin.daemon.CompilerSelector
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_FIND_PORT_ATTEMPTS
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_PORTS_RANGE_END
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_PORTS_RANGE_START
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.findPortAndCreateRegistry
import java.util.Timer

object EclipseKotlinCompilerDaemon {
	
	var compilerService : CompileServiceImpl? = null
	var timer : Timer? = null
	var process : Process? = null;
		
	val compilerSelector = object : CompilerSelector {
                override fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> {
                     return K2JVMCompiler()
                }
            }
	
	public fun start(inProcess : Boolean) {
		if(inProcess)
			startDaemonInProcess()
		else
			startDaemonExt()
	}
	
	private fun startDaemonInProcess() {
		val compilerId = CompilerId()
        val daemonOptions = DaemonOptions()
		
		val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true,
                                                             inheritOtherJvmOptions = true,
                                                             inheritAdditionalProperties = true)
		
		val (registry, port) = findPortAndCreateRegistry(COMPILE_DAEMON_FIND_PORT_ATTEMPTS, COMPILE_DAEMON_PORTS_RANGE_START, COMPILE_DAEMON_PORTS_RANGE_END)
		
		// timer with a daemon thread, meaning it should not prevent JVM to exit normally
        val newTimer = Timer(true)
		compilerService = CompileServiceImpl(registry = registry,
                                                     compiler = compilerSelector,
                                                     compilerId = compilerId,
                                                     daemonOptions = daemonOptions,
                                                     daemonJVMOptions = daemonJVMOptions,
                                                     port = port,
                                                     timer = newTimer,
                                                     onShutdown = {                                                        
                                                     })
		timer = newTimer
		KotlinLogger.logInfo("Kotlim compiler daemon is listening on port: $port")
	}
	
	private fun startDaemonExt() {
		val compilerJar = KOTLIN_COMPILER_PATH
		val jvmLocation = JavaRuntime.getDefaultVMInstall().getInstallLocation()		
		////bin\java -cp kotlin-compiler.jar -Djava.awt.headless=true -Xmx700m -Dkotlin.environment.keepalive org.jetbrains.kotlin.daemon.KotlinCompileDaemon --daemon-runFilesPath C:\Users\Саша и Аня\AppData\Local\kotlin\daemon --daemon-autoshutdownIdleSeconds=7200 --daemon-verbose --daemon-compilerClasspath lib\kotlin-compiler.jar
		process = ProcessBuilder(jvmLocation.getAbsolutePath()+"/bin/java", "-cp", compilerJar, "-Djava.awt.headless=true", "org.jetbrains.kotlin.daemon.KotlinCompileDaemon").start();
	}
	
	public fun stop() {
		if(compilerService != null)
			compilerService?.shutdown()
		if(timer != null)
			timer?.cancel()
		if(process != null)
			process?.destroy()
	}
	
}