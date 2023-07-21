package info.kgeorgiy.ja.pleshanov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * The class provides implementation of the {@link info.kgeorgiy.java.advanced.implementor.JarImpler} interface
 *
 * @author Pleshanov Pavel
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 * @see info.kgeorgiy.java.advanced.implementor.ImplerException
 */
public class Implementor implements JarImpler {

    /**
     * The constructor for the {@code Implementor} class.
     */
    public Implementor() {
    }

    /**
     * The entry point for {@link Implementor}.
     * The main method checks whether the arguments are correct and calls the appropriate method.
     * The first argument is the -jar flag, which indicates that a .jar file must be created.
     * The second argument is the name of the class to be implemented.
     * The third argument is the output file name.
     *
     * @param args command line arguments: [-jar] classname output_file_name
     * @throws IllegalArgumentException if the input arguments are incorrect.
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 3 && (args.length != 4 || !args[1].equals("-jar")))
                || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Wrong format of input");
        }
        try {
            Implementor implementor = new Implementor();
            if (args.length == 3) {
                implementor.implement(Class.forName(args[1]), Path.of(args[2]));
            } else {
                implementor.implementJar(Class.forName(args[2]), Path.of(args[3]));
            }
        } catch (ClassNotFoundException e) {
            System.err.println("No such class: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * This method implements the given class or interface token and writes the generated code to a jar file.
     *
     * @param token   the class or interface token to be implemented.
     * @param jarFile the target jar file.
     * @throws ImplerException if an error occurs while implementing the token or creating the jar file.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path root = Path.of("");
        implement(token, root);
        Path classPath = getFullPath(token, root, ".class");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try {
            if (compiler.run(null, null, null, "-cp",
                    root.getFileName() + File.pathSeparator +
                            Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()),
                    getFullPath(token, root, ".java").toString()) != 0) {
                throw new ImplerException("Error in compiling files");
            }
        } catch (URISyntaxException e) {
            throw new ImplerException(e.getMessage());
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (var stream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            stream.putNextEntry(new ZipEntry(classPath.toString().replace(File.separatorChar, '/')));
            Files.copy(classPath, stream);
        } catch (IOException e) {
            throw new ImplerException("Error in writing to jar " + e.getMessage());
        }
    }

    /**
     * This method implements the given class or interface token and writes the generated code to a .java file.
     *
     * @param token the class or interface token to be implemented.
     * @param root  the root directory where the generated .java file should be saved.
     * @throws ImplerException if an error occurs while implementing the token or writing the code to file.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isArray() || token == Enum.class ||
                Modifier.isPrivate(token.getModifiers()) || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Impossible to extend or implement class");
        }

        Path pathFile = getFullPath(token, root, ".java");
        try {
            Files.createDirectories(pathFile.getParent());
            try (var writer = Files.newBufferedWriter(pathFile)) {
                writePackage(token.getPackageName(), writer);
                writeClassHeader(token, writer);
                if (!token.isInterface()) {
                    writeConstructors(token, writer);
                }
                writeMethods(token, writer);
                write("}", writer);
            }
        } catch (IOException e) {
            throw new ImplerException("Error occurred while writing the code in file");
        }
    }

    /**
     * Returns the full file path for the generated implementation of the given class or interface token
     *
     * @param token     the class or interface token for which the full path is being generated.
     * @param root      the root directory where the generated file will be saved.
     * @param extension the file extension for the generated file.
     * @return the full file path for the generated implementation of the given token.
     */
    private static Path getFullPath(Class<?> token, Path root, String extension) {
        return root.resolve(token.getPackageName().replace('.', File.separatorChar)).
                resolve(getClassName(token) + extension);
    }

    /**
     * Returns the name for the implementation class.
     * The implementation class name is obtained by appending the string "Impl" to the simple name of the token class.
     *
     * @param token the class or interface token for which the implementation class name is being generated.
     * @return the name of the implementation class.
     */
    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Converts a given string to Unicode encoding format
     *
     * @param s the string to be converted to Unicode encoding format.
     * @return the Unicode-encoded string.
     */
    private String toUnicode(String s) {
        StringBuilder builder = new StringBuilder();
        for (char c : s.toCharArray()) {
            builder.append(c >= 128 ? String.format("\\u%04x", (int) c) : c);
        }
        return builder.toString();
    }

    /**
     * Writes the specified package name to BufferedWriter.
     *
     * @param packageName the name of the package to be written
     * @param writer      the BufferedWriter object to write the package name to
     * @throws IOException if an I/O error occurs while writing the package name
     */
    private void writePackage(String packageName, BufferedWriter writer) throws IOException {
        if (!packageName.isEmpty()) {
            write(String.format("%s %s;\n", "package", packageName), writer);
        }
    }

    /**
     * Writes the class header to the specified BufferedWriter,
     * including the class declaration, class name, and any interfaces or superclasses that the class extends or implements.
     *
     * @param token  the class or interface token for which the header will be written
     * @param writer the BufferedWriter to which the header will be written
     * @throws IOException if there is an error writing to the specified writer
     */
    private void writeClassHeader(Class<?> token, BufferedWriter writer) throws IOException {
        write(String.format("%s %s %s %s {\n", "public class", getClassName(token),
                (token.isInterface() ? "implements" : "extends"), token.getCanonicalName()), writer);
    }

    /**
     * Writes the constructors of a given class to a BufferedWriter.
     * Constructors that are private are filtered out, and if all constructors are private, an ImplerException is thrown.
     * The method uses the writeExecutable method to write each constructor to the BufferedWriter.
     *
     * @param token  the Class object representing the class whose constructors are being written
     * @param writer the BufferedWriter object to which the constructors are being written
     * @throws IOException     if an I/O error occurs while writing to the BufferedWriter
     * @throws ImplerException if all constructors are private
     */
    private void writeConstructors(Class<?> token, BufferedWriter writer) throws IOException, ImplerException {
        var constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(e -> !Modifier.isPrivate(e.getModifiers()))
                .toList();
        if (constructors.isEmpty()) {
            throw new ImplerException("Error: all constructors are private");
        }
        for (var constr : constructors) {
            writeExecutable(constr, writer, getClassName(token));
        }
    }

    /**
     * Writes the abstract methods of the given class and its superclasses to a BufferedWriter.
     * The method uses the writeExecutable method to write each abstract method to the BufferedWriter.
     *
     * @param token  The class to inspect for abstract methods.
     * @param writer The BufferedWriter to write the abstract methods to.
     * @throws IOException If an I/O error occurs while writing to the writer.
     */
    private void writeMethods(Class<?> token, BufferedWriter writer) throws IOException {
        Set<CustomMethod> abstractMethods = new HashSet<>();
        findAbstractMethods(token.getMethods(), abstractMethods);
        while (token != null) {
            findAbstractMethods(token.getDeclaredMethods(), abstractMethods);
            token = token.getSuperclass();
        }
        for (var methodWrapper : abstractMethods) {
            Method method = methodWrapper.method;
            writeExecutable(method, writer, method.getName());
        }
    }

    /**
     * Writes an executable to a given buffered writer, in the form of a string representation
     * that includes its modifiers, return type, name, arguments and exceptions.
     *
     * @param exec     the executable object to be written, either a method or a constructor
     * @param writer   the buffered writer to which the executable will be written
     * @param execName the name of the executable
     * @throws IOException if there is an error while writing to the buffered writer
     */
    private void writeExecutable(Executable exec, BufferedWriter writer, String execName) throws IOException {
        write(String.format("%s %s %s%s %s {\n %s;\n}\n",
                getModifiers(exec), getReturnType(exec), execName,
                getArguments(exec, true), getExceptions(exec),
                (exec instanceof Method) ? "return " + getDefaultValue((Method) exec)
                        : "super " + getArguments(exec, false)), writer);
    }

    /**
     * Finds all abstract methods in the given array and adds them to the specified set as instances of CustomMethod.
     *
     * @param methods the array of methods to search for abstract methods
     * @param set     the set to which the found abstract methods will be added
     */
    private void findAbstractMethods(Method[] methods, Set<CustomMethod> set) {
        Arrays.stream(methods)
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .map(CustomMethod::new)
                .collect(Collectors.toCollection(() -> set));
    }

    /**
     * Returns a string representation of the modifiers of the specified executable,
     * excluding the {@code abstract} and {@code transient} modifiers.
     *
     * @param exec the executable to get the modifiers of
     * @return a string representation of the modifiers of the specified executable, excluding {@code abstract} and {@code transient}
     */
    private String getModifiers(Executable exec) {
        return Modifier.toString(exec.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT);
    }

    /**
     * Returns the canonical name of the return type of the given Executable object.
     * If the Executable is an instance of Method, this method will return the canonical name of the return type of that method.
     * If the Executable is a constructor, an empty string will be returned.
     *
     * @param exec the Executable object for which to retrieve the return type
     * @return the canonical name of the return type of the given Executable, or an empty string if the Executable is a constructor
     */
    private String getReturnType(Executable exec) {
        return (exec instanceof Method method) ? method.getReturnType().getCanonicalName() : "";
    }

    /**
     * Returns a string representation of the arguments of the specified executable, in the format "(type1 arg1, type2 arg2, ...)".
     * The second parameter determines whether or not the type of each argument is included in the string.
     *
     * @param exec       the executable whose arguments are to be returned
     * @param isDeclared whether or not to include the type of each argument in the string
     * @return a string representation of the arguments of the specified executable
     */
    private String getArguments(Executable exec, boolean isDeclared) {
        return Arrays.stream(exec.getParameters())
                .map(arg -> String.format("%s %s",
                        isDeclared ? arg.getType().getCanonicalName() : "", arg.getName()))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Returns a string representing the exception types thrown by the specified executable.
     *
     * @param exec the executable whose exceptions are being retrieved
     * @return a string in format: "throws exc1, exc2 ..."
     */
    private String getExceptions(Executable exec) {
        return (exec.getExceptionTypes().length == 0) ? "" : Arrays.stream(exec.getExceptionTypes())
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", " throws ", ""));
    }

    /**
     * Returns the default value for the given method's return type.
     * If the return type is a primitive type, the appropriate default value is returned.
     * If the return type is not a primitive type, then null is returned.
     *
     * @param method the method whose return type's default value is to be determined.
     * @return the default value for the given method's return type.
     */
    private String getDefaultValue(Method method) {
        Class<?> token = method.getReturnType();
        if (!token.isPrimitive()) {
            return "null";
        } else if (token.equals(boolean.class)) {
            return "false";
        } else if (token.equals(void.class)) {
            return "";
        } else {
            return "0";
        }
    }

    /**
     * Writes the given string to the provided BufferedWriter after converting it to Unicode.
     *
     * @param s      the string to be written
     * @param writer the BufferedWriter object to which the string is to be written
     * @throws IOException if an I/O error occurs while writing to the BufferedWriter
     */
    private void write(final String s, BufferedWriter writer) throws IOException {
        writer.write(toUnicode(s));
    }

    /**
     * A wrapper over {@link Method}
     */
    private record CustomMethod(Method method) {

        /**
         * Returns the hash code for this CustomMethod object, which is computed based on the method's name and parameter types.
         *
         * @return the hash code for this CustomMethod object
         */
        @Override
        public int hashCode() {
            return Objects.hash(method.getName(), Arrays.hashCode(method.getParameterTypes()));
        }

        /**
         * Compares this CustomMethod object with the specified object for equality. Returns true if the specified object
         * is also a CustomMethod object with the same method name, return type, and parameter types.
         *
         * @param obj the object to compare with
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CustomMethod)) {
                return false;
            }
            Method otherMethod = ((CustomMethod) obj).method;
            return method.getName().equals(otherMethod.getName())
                    && method.getReturnType().equals(otherMethod.getReturnType())
                    && Arrays.equals(method.getParameterTypes(), otherMethod.getParameterTypes());
        }
    }
}
