package io;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Person {

    private final String name;
    private final int age;
    private final boolean positive;

    public Person(String name, int age, boolean positive) {
        this.name = requireNonNull(name);
        if (age < 0) {
            throw new IllegalArgumentException();
        }
        this.age = age;
        this.positive = positive;
    }

    public String name() {
        return name;
    }

    public int age() {
        return age;
    }

    public boolean positive() {
        return positive;
    }

    public boolean negative() {
        return !positive;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        Person other = (Person) obj;
        return name.equals(other.name) && age == other.age
                && positive == other.positive;
    }

    @Override
    public String toString() {
        return "(" + name + ", " + age + ", " + (positive ? "+" : "-") + ")";
    }
}
