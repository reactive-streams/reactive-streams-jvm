/************************************************************************
* Licensed under Public Domain (CC0)                                    *
*                                                                       *
* To the extent possible under law, the person who associated CC0 with  *
* this code has waived all copyright and related or neighboring         *
* rights to this code.                                                  *
*                                                                       *
* You should have received a copy of the CC0 legalcode along with this  *
* work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
************************************************************************/

package org.reactivestreams.example.unicast;

import java.lang.Override;
import java.util.Collections;
import java.util.Iterator;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Test // Must be here for TestNG to find and run this, do not remove
public class IterablePublisherTest extends PublisherVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public IterablePublisherTest() {
    super(new TestEnvironment());
  }

  @SuppressWarnings("unchecked")
  @Override public Publisher<Integer> createPublisher(final long elements) {
    assert(elements <= maxElementsFromPublisher());
    return new NumberIterablePublisher(0, (int)elements, e);
  }

  @Override public Publisher<Integer> createFailedPublisher() {
    return new AsyncIterablePublisher<Integer>(new Iterable<Integer>() {
      @Override public Iterator<Integer> iterator() {
        throw new RuntimeException("Error state signal!");
      }
    }, e);
  }

  @Override public long maxElementsFromPublisher() {
    return Integer.MAX_VALUE;
  }
}
