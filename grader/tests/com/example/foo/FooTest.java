package com.example.foo;

import com.example.foo.impl.FooImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FooTest {
    @Test
    void greeting() {
        assertEquals("Hello, World!", Foo.greeting());
    }

    @Test
    void greetingImpl() {
        assertEquals("Hello, World!", FooImpl.greeting());
    }
}
