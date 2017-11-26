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

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

final class ReplayProcessor<T> implements Processor<T, T> {

    final AtomicReference<Subscription> upstream;
    
    final List<T> list;

    final boolean delayError;
    
    final AtomicReference<ReplaySubscription<T>[]> subscribers;
    
    final AtomicReference<Throwable> error;
    
    volatile int size;
    
    volatile boolean done;
    
    @SuppressWarnings("rawtypes")
    static final ReplaySubscription[] EMPTY = new ReplaySubscription[0];

    @SuppressWarnings("rawtypes")
    static final ReplaySubscription[] TERMINATED = new ReplaySubscription[0];

    static final Subscription CANCELLED = new EmptySubscription();
    
    ReplayProcessor(boolean delayError) {
        this.delayError = delayError;
        this.list = new ArrayList<T>();
        this.upstream = new AtomicReference<Subscription>();
        this.subscribers = new AtomicReference<ReplaySubscription<T>[]>(EMPTY);
        this.error = new AtomicReference<Throwable>();
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            s.request(Long.MAX_VALUE);
        } else {
            s.cancel();
        }
    }

    @Override
    public void onNext(T t) {
        if (t == null) {
            throw new NullPointerException();
        }
        
        list.add(t);
        size++;
        for (ReplaySubscription<T> rs : subscribers.get()) {
            replay(rs);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Throwable t) {
        if (t == null) {
            throw new NullPointerException();
        }
        if (error.compareAndSet(null, t)) {
            done = true;
            for (ReplaySubscription<T> rs : subscribers.getAndSet(TERMINATED)) {
                replay(rs);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onComplete() {
        done = true;
        for (ReplaySubscription<T> rs : subscribers.getAndSet(TERMINATED)) {
            replay(rs);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        ReplaySubscription<T> rs = new ReplaySubscription<T>(s, this);
        s.onSubscribe(rs);
        if (add(rs)) {
            if (rs.isCancelled()) {
                remove(rs);
                return;
            }
        }
        replay(rs);
    }

    public void replay(ReplaySubscription<T> rs) {
        if (rs.getAndIncrement() != 0) {
            return;
        }
        
        int missed = 1;
        Subscriber<? super T> a = rs.actual;
        List<T> list = this.list;
        int idx = rs.index;
        AtomicLong req = rs.requested;
        
        for (;;) {
            
            long r = req.get();
            
            while (idx != r) {
                if (req.get() == Long.MIN_VALUE) {
                    return;
                }
                
                if (rs.badRequest) {
                    a.onError(new IllegalArgumentException("§3.9 violated: positive request amount required"));
                    return;
                }
                
                boolean d = done;
                
                if (d && !delayError) {
                    Throwable ex = error.get();
                    if (ex != null) {
                        a.onError(ex);
                        return;
                    }
                }
                
                boolean empty = idx >= size;
                
                if (d && empty) {
                    Throwable ex = error.get();
                    if (ex != null) {
                        a.onError(ex);
                    } else {
                        a.onComplete();
                    }
                    return;
                }
                
                if (empty) {
                    break;
                }
                
                a.onNext(list.get(idx++));
            }
            
            if (idx == r) {
                if (req.get() == Long.MIN_VALUE) {
                    return;
                }
                
                if (rs.badRequest) {
                    a.onError(new IllegalArgumentException("§3.9 violated: positive request amount required"));
                    return;
                }
                
                boolean d = done;
                
                if (d && !delayError) {
                    Throwable ex = error.get();
                    if (ex != null) {
                        a.onError(ex);
                        return;
                    }
                }
                
                boolean empty = idx >= size;
                if (d && empty) {
                    Throwable ex = error.get();
                    if (ex != null) {
                        a.onError(ex);
                    } else {
                        a.onComplete();
                    }
                    return;
                }
            }
            
            int w = rs.get();
            if (w == missed) {
                rs.index = idx;
                missed = rs.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            } else {
                missed = w;
            }
        }
    }
    
    boolean add(ReplaySubscription<T> rs) {
        for (;;) {
            ReplaySubscription<T>[] a = subscribers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            @SuppressWarnings("unchecked")
            ReplaySubscription<T>[] b = new ReplaySubscription[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = rs;
            
            if (subscribers.compareAndSet(a, b)) {
                return true;
            }
        }
    }
    
    void remove(ReplaySubscription<T> rs) {
        for (;;) {
            ReplaySubscription<T>[] a = subscribers.get();
            int n = a.length;
            if (n == 0) {
                break;
            }
            
            int j = -1;
            
            for (int i = 0; i < n; i++) {
                if (a[i] == rs) {
                    j = i;
                    break;
                }
            }
            
            if (j < 0) {
                break;
            }
            if (n == 1) {
                @SuppressWarnings("unchecked")
                ReplaySubscription<T>[] b = TERMINATED;
                if (subscribers.compareAndSet(a, b)) {
                    Subscription s = upstream.getAndSet(CANCELLED);
                    if (s != null) {
                        s.cancel();
                    }
                    break;
                }
            } else {
                @SuppressWarnings("unchecked")
                ReplaySubscription<T>[] b = new ReplaySubscription[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
                if (subscribers.compareAndSet(a, b)) {
                    break;
                }
            }
        }
    }
    
    static final class ReplaySubscription<T>
    extends AtomicInteger
    implements Subscription {
        
        /** */
        private static final long serialVersionUID = -3704758100845141134L;

        final ReplayProcessor<T> parent;
        
        final Subscriber<? super T> actual;
        
        final AtomicLong requested;
        
        long emitted;
        
        int index;
        
        volatile boolean badRequest;
        
        ReplaySubscription(Subscriber<? super T> actual, ReplayProcessor<T> parent) {
            this.actual = actual;
            this.requested = new AtomicLong();
            this.parent = parent;
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                badRequest = true;
                parent.replay(this);
            } else {
                for (;;) {
                    long r = requested.get();
                    if (r == Long.MIN_VALUE) {
                        break;
                    }
                    long u = r + n;
                    if (u < 0L) {
                        u = Long.MAX_VALUE;
                    }
                    if (requested.compareAndSet(r, u)) {
                        parent.replay(this);
                        break;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            if (requested.getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }
        
        public boolean isCancelled() {
            return requested.get() == Long.MIN_VALUE;
        }
    }
    
    static final class EmptySubscription implements Subscription {
        @Override
        public void request(long n) {
        }
        @Override
        public void cancel() {
        }
    }
}
