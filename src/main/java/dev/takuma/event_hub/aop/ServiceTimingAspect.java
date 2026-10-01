package dev.takuma.event_hub.aop;

import dev.takuma.event_hub.utils.ApiException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceTimingAspect {

	private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);
	private static final long SLOW_CALL_MS = 500;

	@Around("execution(public * dev.takuma.event_hub.service.*.*(..))")
	public Object time(ProceedingJoinPoint joinPoint) throws Throwable {
		long startedAt = System.nanoTime();
		try {
			Object result = joinPoint.proceed();
			logSuccess(joinPoint, elapsedMillis(startedAt));
			return result;
		}
		catch (ApiException exception) {
			log.debug("{} rejected in {} ms: {}", method(joinPoint), elapsedMillis(startedAt), exception.getMessage());
			throw exception;
		}
		catch (Throwable exception) {
			log.error("{} failed in {} ms", method(joinPoint), elapsedMillis(startedAt), exception);
			throw exception;
		}
	}

	private void logSuccess(ProceedingJoinPoint joinPoint, long elapsedMs) {
		String method = method(joinPoint);
		if (elapsedMs >= SLOW_CALL_MS) {
			log.warn("{} completed in {} ms", method, elapsedMs);
			return;
		}
		log.info("{} completed in {} ms", method, elapsedMs);
	}

	private static long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000;
	}

	private static String method(ProceedingJoinPoint joinPoint) {
		return joinPoint.getSignature().toShortString();
	}

}
