/*
 * Infinitest, a Continuous Test Runner.
 *
 * Copyright (C) 2010-2013
 * "Ben Rady" <benrady@gmail.com>,
 * "Rod Coffin" <rfciii@gmail.com>,
 * "Ryan Breidenbach" <ryan.breidenbach@gmail.com>
 * "David Gageot" <david@gageot.net>, et al.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.infinitest.plugin;

import static org.infinitest.environment.FakeEnvironments.currentJavaHome;
import static org.infinitest.environment.FakeEnvironments.fakeBuildPaths;
import static org.infinitest.environment.FakeEnvironments.fakeEnvironment;
import static org.infinitest.environment.FakeEnvironments.fakeWorkingDirectory;
import static org.infinitest.environment.FakeEnvironments.systemClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.infinitest.FakeEventQueue;
import org.infinitest.InfinitestCore;
import org.infinitest.InfinitestCoreBuilder;
import org.infinitest.environment.RuntimeEnvironment;
import org.infinitest.filter.TestFilter;
import org.infinitest.parser.TestDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhenConfiguringBuilder {
	protected InfinitestCoreBuilder builder;
	private TestFilter filterUsedToCreateCore;

	@BeforeEach
	final void mustProvideRuntimeEnvironmentAndEventQueue() {
		RuntimeEnvironment environment = new RuntimeEnvironment(currentJavaHome(), fakeWorkingDirectory(), systemClasspath(), systemClasspath(), fakeBuildPaths(), systemClasspath());
		builder = new InfinitestCoreBuilder(environment, new FakeEventQueue(), "myCoreName");
	}

	@Test
	void canUseCustomFilterToRemoveTestsFromTestRun() {
		TestFilter testFilter = mock(TestFilter.class);
		builder = new InfinitestCoreBuilder(fakeEnvironment(), new FakeEventQueue(), "myCoreName") {
			@Override
			protected TestDetector createTestDetector(TestFilter testFilter) {
				filterUsedToCreateCore = testFilter;
				return super.createTestDetector(testFilter);
			}
		};
		builder.setFilter(testFilter);
		builder.createCore();
		assertSame(filterUsedToCreateCore, testFilter);
	}

	@Test
	void canSetCoreName() {
		InfinitestCore core = builder.createCore();
		assertEquals("myCoreName", core.getName());
	}

	@Test
	void shouldSetRuntimeEnvironment() {
		InfinitestCore core = builder.createCore();
		assertEquals(fakeEnvironment(), core.getRuntimeEnvironment());
	}
}
