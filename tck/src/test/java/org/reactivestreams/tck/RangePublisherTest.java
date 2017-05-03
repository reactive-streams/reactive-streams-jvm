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

package org.reactivestreams.tck;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;
import org.testng.annotations.*;

@Test
public class RangePublisherTest extends PublisherVerification<Integer> {

    static final Map<Integer, StackTraceElement[]> stacks = new ConcurrentHashMap<Integer, StackTraceElement[]>();
    
    static final Map<Integer, Boolean> states = new ConcurrentHashMap<Integer, Boolean>();
    
    static final AtomicInteger id = new AtomicInteger();
    
    @AfterClass
    public static void afterClass() {
        boolean fail = false;
        StringBuilder b = new StringBuilder();
        for (Map.Entry<Integer, Boolean> t : states.entrySet()) {
            if (!t.getValue()) {
                b.append("\r\n-------------------------------");
                for (Object o : stacks.get(t.getKey())) {
                    b.append("\r\nat ").append(o);
                }
                fail = true;
            }
        }
        if (fail) {
            throw new AssertionError("Cancellations were missing:" + b);
        }
    }
    
    public RangePublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Integer> createPublisher(long elements) {
        return new RangePublisher(1, elements);
    }

    @Override
    public Publisher<Integer> createFailedPublisher() {
        return null;
    }

    static final class RangePublisher
    implements Publisher<Integer> {
        
        final StackTraceElement[] stacktrace;

        final long start;
        
        final long count;
        
        RangePublisher(long start, long count) {
            this.stacktrace = Thread.currentThread().getStackTrace();
            this.start = start;
            this.count = count;
        }
        
        @Override
        public void subscribe(Subscriber<? super Integer> s) {
            if (s == null) {
                throw new NullPointerException();
            }
            
            int ids = id.incrementAndGet();
            
            RangeSubscription parent = new RangeSubscription(s, ids, start, start + count);
            stacks.put(ids, stacktrace);
            states.put(ids, false);
            s.onSubscribe(parent);
        }
        
        static final class RangeSubscription extends AtomicLong implements Subscription {

            private static final long serialVersionUID = 9066221863682220604L;

            final Subscriber<? super Integer> actual;
            
            final int ids;

            final long end;

            long index;
            
            volatile boolean cancelled;
            
            RangeSubscription(Subscriber<? super Integer> actual, int ids, long start, long end) {
                this.actual = actual;
                this.ids = ids;
                this.index = start;
                this.end = end;
            }
            
            @Override
            public void request(long n) {
                if (!cancelled) {
                    if (n <= 0L) {
                        cancelled = true;
                        states.put(ids, true);
                        actual.onError(new IllegalArgumentException("§3.9 violated"));
                        return;
                    }
                    
                    for (;;) {
                        long r = get();
                        long u = r + n;
                        if (u < 0L) {
                            u = Long.MAX_VALUE;
                        }
                        if (compareAndSet(r, u)) {
                            if (r == 0) {
                                break;
                            }
                            return;
                        }
                    }
                    
                    long idx = index;
                    long f = end;
                    
                    for (;;) {
                        long e = 0;
                        while (e != n && idx != f) {
                            if (cancelled) {
                                return;
                            }
                            
                            actual.onNext((int)idx);
                            
                            idx++;
                            e++;
                        }
                        
                        if (idx == f) {
                            if (!cancelled) {
                                states.put(ids, true);
                                actual.onComplete();
                            }
                            return;
                        }
                        
                        index = idx;
                        n = addAndGet(-n);
                        if (n == 0) {
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void cancel() {
                cancelled = true;
                states.put(ids, true);
            }
        }
    }
}
