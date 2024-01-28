package com.example.foo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FooTest {
    @Test
    void greeting() {
        assertEquals("Hello, World!", Foo.greeting());
    }
}
