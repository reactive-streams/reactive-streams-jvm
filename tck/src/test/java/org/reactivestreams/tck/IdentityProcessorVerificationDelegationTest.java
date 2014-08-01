package org.reactivestreams.tck;

import junit.framework.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link org.reactivestreams.tck.IdentityProcessorVerification} must also run all tests from
 * {@link org.reactivestreams.tck.PublisherVerification} and {@link org.reactivestreams.tck.SubscriberWhiteboxVerification}.
 *
 * Since in Java this can be only achieved by delegating, we need to make sure we delegate to each of the tests,
 * so that if in the future we add more tests to these verifications we're sure to not forget to add the delegating methods.
 */
public class IdentityProcessorVerificationDelegationTest {

  @Test
  public void shouldIncludeAllTestsFromPublisherVerification() throws Exception {
    // given
    List<String> processorTests = getTestNames(IdentityProcessorVerification.class);
    Class<PublisherVerification> delegatedToClass = PublisherVerification.class;

    // when
    List<String> publisherTests = getTestNames(delegatedToClass);

    // then
    assertSuiteDelegatedAllTests(IdentityProcessorVerification.class, processorTests, delegatedToClass, publisherTests);
  }

  @Test
  public void shouldIncludeAllTestsFromSubscriberVerification() throws Exception {
    // given
    List<String> processorTests = getTestNames(IdentityProcessorVerification.class);
    Class<SubscriberWhiteboxVerification> delegatedToClass = SubscriberWhiteboxVerification.class;

    // when
    List<String> publisherTests = getTestNames(delegatedToClass);

    // then
    assertSuiteDelegatedAllTests(IdentityProcessorVerification.class, processorTests, delegatedToClass, publisherTests);
  }

  private void assertSuiteDelegatedAllTests(Class<?> delegatingFrom, List<String> allTests, Class<?> targetClass, List<String> delegatedToTests) {
    for (String targetTest : delegatedToTests) {
      String msg = String.format(
          "Test '%s' in '%s' has not been properly delegated to in aggregate '%s'! \n" +
              "You must delegate to this test from %s, like this: \n" +
              "@Test public void %s() throws Exception { delegate%s.%s(); }",
          targetTest, targetClass, delegatingFrom,
          delegatingFrom,
          targetTest, targetClass.getSimpleName(), targetTest);

      Assert.assertTrue(msg, testsInclude(allTests, targetTest));
    }
  }


  private boolean testsInclude(List<String> processorTests, String publisherTest) {
    return processorTests.contains(publisherTest);
  }

  private List<String> getTestNames(Class<?> clazz) {
    List<String> tests = new ArrayList<String>();
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Test.class)) {
        tests.add(method.getName());
      }
    }

    return tests;
  }
}
