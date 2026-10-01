package dev.takuma.event_hub.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.takuma.event_hub.utils.ApiException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceTimingAspectTest {

	@InjectMocks
	private ServiceTimingAspect aspect;

	@Mock
	private ProceedingJoinPoint joinPoint;

	@Mock
	private Signature signature;

	@Test
	void returnsTheServiceResult() throws Throwable {
		when(joinPoint.getSignature()).thenReturn(signature);
		when(signature.toShortString()).thenReturn("EventService.list()");
		when(joinPoint.proceed()).thenReturn("events");

		assertThat(aspect.time(joinPoint)).isEqualTo("events");
		verify(joinPoint).proceed();
	}

	@Test
	void rethrowsBusinessFailures() throws Throwable {
		when(joinPoint.getSignature()).thenReturn(signature);
		when(signature.toShortString()).thenReturn("OrderService.purchase(..)");
		ApiException failure = ApiException.conflict("sold out");
		when(joinPoint.proceed()).thenThrow(failure);

		assertThatThrownBy(() -> aspect.time(joinPoint)).isSameAs(failure);
	}

	@Test
	void rethrowsUnexpectedFailures() throws Throwable {
		when(joinPoint.getSignature()).thenReturn(signature);
		when(signature.toShortString()).thenReturn("OrderService.purchase(..)");
		IllegalStateException failure = org.mockito.Mockito.mock(IllegalStateException.class);
		when(joinPoint.proceed()).thenThrow(failure);

		assertThatThrownBy(() -> aspect.time(joinPoint)).isSameAs(failure);
	}

}
