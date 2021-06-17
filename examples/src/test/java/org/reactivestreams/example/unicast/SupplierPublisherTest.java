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

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test // Must be here for TestNG to find and run this, do not remove
public class SupplierPublisherTest extends PublisherVerification<Integer> {

  private ExecutorService e;
  @BeforeClass void before() { e = Executors.newFixedThreadPool(4); }
  @AfterClass void after() { if (e != null) e.shutdown(); }

  public SupplierPublisherTest() {
    super(new TestEnvironment());
  }

  @SuppressWarnings("unchecked")
  @Override public Publisher<Integer> createPublisher(final long elements) {
    assert(elements <= maxElementsFromPublisher());
    return new SupplierPublisher<Integer>(new SupplierPublisher.Supplier<Integer>() {
      private int at;
      @Override
      public Integer get() {
        return at < elements ? at++ : null;
      }
    }, e);
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
