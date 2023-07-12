package info.kgeorgiy.ja.faizieva.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Class implementing {@link Impler} and {@link JarImpler}
 * @author faizieva
 */
public class Implementor implements Impler, JarImpler {
    /**
     * Constant string that has line separator of the system
     */
    private static final String LINE_SEP = System.lineSeparator();
    /**
     * Constant string that has doubled line separator
     */
    private static final String LINE_SEP_DOUBLE = LINE_SEP + LINE_SEP;
    /**
     * Constant string that has separator for file path
     */
    private static final String FILE_SEP = File.separator;
    /**
     * Constant string that has space with comma
     */
    private static final String COMMA = ", ";
    /**
     * Constant string that space
     */
    private static final String SPACE = " ";

    /**
     * Return a path where implementation of token will be held and create directory where implementation of token is located
     * @param token of implemented {@link Class}
     * @param rootDirectory given path where implementation of token should be located
     * @return a path with name of generated class
     * @throws ImplerException in cases of incorrect @{param path} or impossibility to create directory
     */
    private Path getClassFilePath(final Class<?> token, final Path rootDirectory) throws ImplerException {
        final Path path;
        try {
            path = rootDirectory.resolve(token.getPackageName().replace('.', File.separatorChar))
                    .resolve(token.getSimpleName() + "Impl." + "java");
        } catch (final InvalidPathException e) {
            throw new ImplerException("Directory is invalid");
        }
        try {
            Files.createDirectories(path.getParent());
        } catch (final IOException e) {
            throw new ImplerException("Unable to create directory");
        }
        return path;
    }

    /**
     * Validate given {@param token} checking modifiers and type of token
     * @param token of implemented {@link Class}
     * @throws ImplerException if token is unsupported
     */
    private void validateToken(final Class<?> token) throws ImplerException {
        final int modifiers = token.getModifiers();
        if (token.isPrimitive() || token == Enum.class || Modifier.isPrivate(modifiers) || !token.isInterface()) {
            throw new ImplerException("Invalid token");
        }
    }

    /**
     * Implementation of method implement.
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException in case of impossibility to write.
     */
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        validateToken(token);
        final Path classPath = getClassFilePath(token, root);
        try (final BufferedWriter writer = Files.newBufferedWriter(classPath)) {
            writer.write(getInterface(token));
        } catch (final IOException e) {
            throw new ImplerException("Error opening or creating file");
        }
    }

    /**
     * Concatenates package of the token with heading and list of methods.
     * @param token type token to create implementation for.
     * @return String-result of the concatenation.
     */
    public static String getInterface(final Class<?> token) {
        return String.format("%s%n%n" +
                        "%s%n" +
                        "%s%n}",
                getPackage(token),
                getHeading(token),
                String.join(LINE_SEP_DOUBLE, getMethodList(token)));
    }

    /**
     * Checks if {@link Class} has package and returns it.
     * @param token type token to create implementation for.
     * @return String-package of the specified token.
     */
    private static String getPackage(final Class<?> token) {
        return token.getPackageName().equals("") ? "" : "package" + SPACE + token.getPackage().getName() + ";";
    }

    /**
     * Concatenates all parts of the method given into one String.
     * @param method the method to be implemented.
     * @return String that contains both heading and body of the method.
     */
    private static String getMethod(Method method) {
        return String.format("\t%s%n" +
                        "\t\treturn %s;" +
                        "\t}",
                getMethodHeading(method, String.join(SPACE, method.getReturnType().getCanonicalName(), method.getName())),
                getDefaultValue(method.getReturnType()));
    }

    /**
     * Create list of all methods of the {@link Class}
     * @param token type token to create implementation for.
     * @return List of methods.
     */
    private static List<String> getMethodList(final Class<?> token) {
        return Arrays.stream(token.getMethods()).map(Implementor::getMethod).collect(Collectors.toList());
    }

    /**
     * Concatenates name, parameters and exceptions of the method.
     * @param method specified method to get heading to.
     * @param name name of this method.
     * @return String-heading of the specified method.
     */

    private static String getMethodHeading(final Method method, final String name) {
        return String.join(SPACE,
                "public", name,
                "(" + getArguments(method.getParameters()) + ")", getExceptions(method.getExceptionTypes()), "{");
    }

    /**
     * Puts arguments of method to one string.
     * @param parameters an array of parameters.
     * @return String containing all the arguments.
     */
    private static String getArguments(final Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(parameter -> parameter.getType().getCanonicalName() + SPACE + parameter.getName())
                .collect(Collectors.joining(COMMA));
    }

    /**
     * Check if method throws any exceptions and returns concatenated string of exceptions.
     * @param exceptions an array of exceptionsTypes of the method.
     * @return String containing exceptions that may be thrown.
     */
    private static String getExceptions(final Class<?>[] exceptions) {
        return exceptions.length == 0 ? "" :
                "throws " + Arrays.stream(exceptions).map(Class::getCanonicalName).collect(Collectors.joining(COMMA));
    }

    /**
     * Concatenated modifiers, name and implemented interfaces.
     * @param token specified {@link Class} to get heading to.
     * @return String-heading of the specified {@link Class}.
     */
    private static String getHeading(final Class<?> token) {
        return String.join(
                SPACE,
                "public", "class", token.getSimpleName() + "Impl", "implements", token.getCanonicalName(), "{");
    }

    /**
     * Defines default value of method.
     * @param returnValue return type of the method.
     * @return String containing default value.
     */
    private static String getDefaultValue(final Class<?> returnValue) {
        if (!returnValue.isPrimitive()) {
            return "null";
        } else if (returnValue.equals(boolean.class)) {
            return "false";
        } else if (returnValue.equals(void.class)) {
            return "";
        }
        return "0";
    }


    /**
     * Produces code for the interface given.
     * @param args program arguments.
     */
    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        if (args.length < 2 || args[0] == null) {
            System.out.println("Wrong args");
            System.exit(0);
        }
        try {
            implementor.implement(Class.forName(args[0]), Path.of(args[1]));
        } catch (ImplerException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Implementation of implementJar that creates .jar of implemented {@link Class}.
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if one of methods throws it.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path temp = createDirectory(jarFile);
        implement(token, temp);
        final String file = String.join(FILE_SEP, temp.toString(), pathOfToken(token, FILE_SEP, "java"));
        compile(temp, token, file);
        createJar(jarFile, token, temp);
    }

    /**
     * Creates a temporary directory.
     * @param root place to create directory near to.
     * @return Temporary path to parent directory with suffix "jar".
     * @throws ImplerException if unable to create directory.
     */
    private static Path createDirectory(Path root) throws ImplerException {
        try {
            return Files.createTempDirectory(root.getParent(), "jar");
        } catch (IOException e) {
            throw new ImplerException("Enable to create directory " + e.getMessage());
        }
    }

    /**
     * Puts ClassPath of the specified {@link Class} to String.
     * @param token {@link Class} to get ClassPath of.
     * @return String of ClassPath.
     * @throws ImplerException if unable to get URI of {@link Class}.
     */
    private static String getClassPath(Class<?> token) throws ImplerException {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Enable to get Uri of file");
        }
    }

    /**
     * Compilation of the implementation from given {@link Class} token.
     * @param root temporary directory with .class files.
     * @param token implemented {@link Class} token.
     * @param file name of implemented class.
     * @throws ImplerException if compilation exit code was not 0.
     */
    private static void compile(Path root, Class<?> token, String file) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath = root + File.pathSeparator + getClassPath(token);
        final int exitCode = compiler.run(null, null, null, file, "-encoding", "UTF-8", "-cp", classpath);
        if (exitCode != 0) {
            throw new ImplerException("Enable to compile, compiler exit code: " + exitCode);
        }
    }

    /**
     * Creates .jar file.
     * @param jarFile path where .jar file will be created.
     * @param token implemented {@link Class} token.
     * @param temp directory with .class files.
     * @throws ImplerException if unable to write in jar.
     */
    private static void createJar(Path jarFile, Class<?> token, Path temp) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String classFileName = pathOfToken(token, "/","class");
            out.putNextEntry(new ZipEntry(classFileName));
            Files.copy(temp.resolve(classFileName), out);
        } catch (IOException e) {
            throw new ImplerException("Enable to write into jar " + e.getMessage());
        }
    }

    /**
     * Creates name for specified token from package and puts it to String.
     * @param token implemented {@link Class} token.
     * @param delimiter delimiter for path.
     * @param extension of the file
     * @return String-relative path of {@link Class} implementation of {@link Class} token.
     */
    private static String pathOfToken(Class<?> token, String delimiter, String extension) {
        return token.getPackageName().replace(".", delimiter) + delimiter + token.getSimpleName() + "Impl." + extension;
    }

}
