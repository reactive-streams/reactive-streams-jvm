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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.example.unicast.AsyncIterablePublisher;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Test
public class BrokenExampleBlackboxTest extends SubscriberBlackboxVerification<Integer> {

  private ExecutorService ex;

  public BrokenExampleBlackboxTest() {
    super(new TestEnvironment());
  }

  @BeforeClass
  void before() { ex = Executors.newFixedThreadPool(4); }

  @AfterClass
  void after() { if (ex != null) ex.shutdown(); }


  @Override
  public Integer createElement(int element) {
    return element;
  }

  @Override
  public Subscriber<Integer> createSubscriber() {
    return new MySubscriber<Integer>();
  }
  
  public class MySubscriber<T> implements Subscriber<T> {  
    private Subscription subscription;  
    
    @Override  
    public void onSubscribe(Subscription subscription) {  
      this.subscription = subscription;  
      subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded  
    }  
    
    @Override  
    public void onNext(T item) {  
      System.out.println("Got : " + item);  
      subscription.request(1); //a value of  Long.MAX_VALUE may be considered as effectively unbounded  
    }  
    
    @Override  
    public void onError(Throwable t) {  
      t.printStackTrace();  
    }  
    
    @Override  
    public void onComplete() {  
      System.out.println("Done");  
    }  
  }  
}
