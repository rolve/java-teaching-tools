package students;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StudentClientTest {

    @Test
    void fullName() {
        Student student = new Student("Max", "Mustermann");
        assertEquals("Max Mustermann", StudentClient.fullName(student));
    }
}
