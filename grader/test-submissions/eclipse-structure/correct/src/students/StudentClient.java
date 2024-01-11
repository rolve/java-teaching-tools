package students;

public class StudentClient {
    public static String fullName(Student student) {
        // correctly solved without modifying the Student class
        return student.getFirstName() + " " + student.getLastName();
    }
}
