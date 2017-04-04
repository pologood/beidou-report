package com.baidu.beidou.test;
/**
 * cp from https://github.com/fenriswolf/examples/blob/master/springtest/src/com/fw/springtest/SpringParameterizedRunner.java
 * 
 */
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
public class SpringParameterizedRunner extends Suite {
    private class TestClassRunnerForParameters extends SpringJUnit4ClassRunner {
        private final int fParameterSetNumber;

        private final List<Object[]> fParameterList;

        TestClassRunnerForParameters(Class<?> type, List<Object[]> parameterList, int i)
                throws InitializationError {
            super(type);
            fParameterList = parameterList;
            fParameterSetNumber = i;
        }

        @Override
        public Object createTest() throws Exception {
            Object testInstance = getTestClass().getOnlyConstructor().newInstance(
                    computeParams());
            getTestContextManager().prepareTestInstance(testInstance);
            return testInstance;
        }

        private Object[] computeParams() throws Exception {
            try {
                return fParameterList.get(fParameterSetNumber);
            } catch (ClassCastException e) {
                throw new Exception(
                        String.format("%s.%s() must return a Collection of arrays.",
                                getTestClass().getName(),
                                getParametersMethod(getTestClass()).getName()));
            }
        }

        @Override
        protected String getName() {
            return String.format("[%s]", fParameterSetNumber);
        }

        @Override
        protected String testName(final FrameworkMethod method) {
            return String.format("%s[%s]", method.getName(), fParameterSetNumber);
        }

        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
        }

        @Override
        protected Statement classBlock(RunNotifier notifier) {
            return childrenInvoker(notifier);
        }
    }

    private final ArrayList<Runner> runners = new ArrayList<Runner>();

    /**
     * Only called reflectively. Do not use programmatically.
     */
    public SpringParameterizedRunner(Class<?> klass) throws Throwable {
        super(klass, Collections.<Runner> emptyList());
        List<Object[]> parametersList = getParametersList(getTestClass());
        for (int i = 0; i < parametersList.size(); i++)
            runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(),
                    parametersList, i));
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> getParametersList(TestClass klass) throws Throwable {
        return (List<Object[]>) getParametersMethod(klass).invokeExplosively(null);
    }

    private FrameworkMethod getParametersMethod(TestClass testClass) throws Exception {
        List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Parameters.class);
        for (FrameworkMethod each : methods) {
            int modifiers = each.getMethod().getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers))
                return each;
        }

        throw new Exception("No public static parameters method on class "
                + testClass.getName());
    }
}