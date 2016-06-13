package org.squiddev.luaj.luajc.compilation;

import org.squiddev.luaj.luajc.analysis.ProtoInfo;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Delegates compilation into a separate thread
 */
public class ThreadedCompilation {
	private static int THREAD_PRIORITY = Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2;
	private static int THREADS = 1;

	private static ScheduledExecutorService createThread(String name, int threads) {
		final String prefix = "luaj.luajc-" + name + "-";
		final AtomicInteger counter = new AtomicInteger(1);

		SecurityManager manager = System.getSecurityManager();
		final ThreadGroup group = manager == null ? Thread.currentThread().getThreadGroup() : manager.getThreadGroup();
		return Executors.newScheduledThreadPool(threads, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(group, runnable, prefix + counter.getAndIncrement());
				if (!thread.isDaemon()) thread.setDaemon(true);
				if (thread.getPriority() != THREAD_PRIORITY) thread.setPriority(THREAD_PRIORITY);

				return thread;
			}
		});
	}

	private static final ScheduledExecutorService COMPILATION_THREAD = createThread("compiler", THREADS);

	public static void scheduleCompilation(ProtoInfo info, JavaLoader loader) {
		final WeakReference<ProtoInfo> infoRef = new WeakReference<ProtoInfo>(info);
		final WeakReference<JavaLoader> loaderRef = new WeakReference<JavaLoader>(loader);

		COMPILATION_THREAD.submit(new Runnable() {
			@Override
			public void run() {
				ProtoInfo info = infoRef.get();
				JavaLoader loader = loaderRef.get();
				if (info == null || loader == null) return;

				info.executor = loader.includeImpl(info);
			}
		});
	}
}
