package ch.trick17.jtt.sandbox;

public class ClassUnderTest {

    public static void foo() {
        var y = Math.sin(Math.PI);
        System.out.println(y);
    }

    public static void main(String[] args) {
        foo();
    }
}
