package ch.trick17.jtt.testrunner;

import org.junit.jupiter.api.*;

import static java.util.Comparator.comparingInt;

public class OrderAnnotationThenDisplayName implements ClassOrderer, MethodOrderer {

    @Override
    public void orderClasses(ClassOrdererContext context) {
        context.getClassDescriptors().sort(comparingInt(this::getClassOrder)
                .thenComparing(ClassDescriptor::getDisplayName));
    }

    private int getClassOrder(ClassDescriptor descriptor) {
        return descriptor.findAnnotation(Order.class)
                .map(Order::value).orElse(Order.DEFAULT);
    }

    @Override
    public void orderMethods(MethodOrdererContext context) {
        context.getMethodDescriptors().sort(comparingInt(this::getMethodOrder)
                .thenComparing(MethodDescriptor::getDisplayName));
    }

    private int getMethodOrder(MethodDescriptor descriptor) {
        return descriptor.findAnnotation(Order.class)
                .map(Order::value).orElse(Order.DEFAULT);
    }
}
