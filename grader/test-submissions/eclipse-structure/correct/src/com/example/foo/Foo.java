package com.example.foo;

import com.example.foo.impl.FooImpl;

public class Foo {
    public static String greeting() {
        return FooImpl.greeting();
    }
}
