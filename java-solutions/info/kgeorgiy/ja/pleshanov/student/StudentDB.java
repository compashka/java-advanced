package info.kgeorgiy.ja.pleshanov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements GroupQuery {
    private static final Comparator<Group> COMPARE_BY_GROUP_NAME = Comparator.comparing(Group::getName);
    private static final Comparator<Student> COMPARE_BY_ID = Comparator.comparing(Student::getId);
    private static final Comparator<Student> COMPARE_BY_NAME = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .reversed()
            .thenComparingInt(Student::getId);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return streamMap(students, Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.naturalOrder())
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, COMPARE_BY_ID);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, COMPARE_BY_NAME);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterToList(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterToList(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterToList(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return streamFilter(students, Student::getGroup, group)
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                ));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortGroups(students, COMPARE_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortGroups(students, COMPARE_BY_ID);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroup(students, true);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroup(students, false);
    }

    private GroupName getLargestGroup(Collection<Student> students, boolean isDistinctNames) {
        return getStreamGroups(students.stream())
                .max(Comparator
                        .comparing((Group g) -> (isDistinctNames ? g.getStudents() :
                                getDistinctFirstNames(g.getStudents())).size())
                        .thenComparing(isDistinctNames ? COMPARE_BY_GROUP_NAME :
                                COMPARE_BY_GROUP_NAME.reversed()))
                .map(Group::getName)
                .orElse(null);
    }

    private <T> Stream<T> streamMap(Collection<Student> students, Function<Student, T> mapper) {
        return students.stream()
                .map(mapper);
    }

    private <T> List<T> mapToList(Collection<Student> students, Function<Student, T> mapper) {
        return streamMap(students, mapper).toList();
    }

    private <T> Stream<Student> streamFilter(Collection<Student> students, Function<Student, T> filter, T value) {
        return students.stream()
                .filter(student -> filter.apply(student).equals(value))
                .sorted(COMPARE_BY_NAME);
    }

    private <T> List<Student> filterToList(Collection<Student> students, Function<Student, T> filter, T value) {
        return streamFilter(students, filter, value).toList();
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .toList();
    }

    private Stream<Group> getStreamGroups(Stream<Student> students) {
        return students
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream()
                .map(el -> new Group(el.getKey(), el.getValue()));
    }

    private List<Group> sortGroups(Collection<Student> students, Comparator<Student> comparator) {
        return getStreamGroups(students.stream().sorted(comparator))
                .sorted(COMPARE_BY_GROUP_NAME)
                .toList();
    }
}
