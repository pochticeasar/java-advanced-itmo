package info.kgeorgiy.ja.faizieva.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class StudentDB implements StudentQuery {
    private static final Comparator<Student> studentComparator =
            Comparator.comparing(Student::getLastName).reversed().
                    thenComparing(Comparator.comparing(Student::getFirstName).reversed()).
                    thenComparingInt(Student::getId);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getSpecifiedList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getSpecifiedList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        //Note: dupcode
        return getSpecifiedList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getSpecifiedList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getStream(students).map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return getStream(students).max(Comparator.comparingInt(Student::getId)).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return getSortedStream(students, Comparator.comparingInt(Student::getId)).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return getSortedStream(students, studentComparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        // Student::getName, name
        return getSortedFilteredStreamToList(students, function(name, Student::getFirstName));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return getSortedFilteredStreamToList(students, function(name, Student::getLastName));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return getSortedFilteredStreamToList(students, function(group, Student::getGroup));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return getSortedFilteredStreamToMap(students, function(group, Student::getGroup));
    }

    private <R> List<R> getSpecifiedList(List<Student> students, Function<Student, R> function) {
        return getStream(students).map(function).collect(Collectors.toList());
    }

    private static <T, P> Predicate<P> function(T item, Function<P, T> function) {
        return student -> function.apply(student).equals(item);
    }

    private Stream<Student> getStream(List<Student> students) {
        return students.stream();
    }

    private Stream<Student> getSortedStream(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator);
    }

    private List<Student> getSortedFilteredStreamToList(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().sorted(StudentDB.studentComparator).filter(predicate).collect(Collectors.toList());
    }

    private Map<String, String> getSortedFilteredStreamToMap(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().sorted(StudentDB.studentComparator).
                filter(predicate).
                collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}