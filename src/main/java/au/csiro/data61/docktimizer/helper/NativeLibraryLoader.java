package au.csiro.data61.docktimizer.helper;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * this is the loader for the native library LP_SOLVE or CPLEX if present
 * ONLY SUPPORTS UNIX SOFAR
 */
public abstract class NativeLibraryLoader {
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(NativeLibraryLoader.class);

    protected static boolean useCPLEX = false;

    /**
     * List of native libraries you put in src/main/resources
     */
    static final String[] NATIVE_LIB_FILENAMES = {
            "natives/libcplex1260.so",
            "natives/libcplex1260_x64.so",
            "natives/liblpsolve55.so",
            "natives/liblpsolve55j.so",
            "natives/liblpsolve55j_x64.so",
    };

    static {
        try {
            System.loadLibrary("cplex1260");

            LOG.info("Loading from classpath successful");
            useCPLEX = true;
        } catch (UnsatisfiedLinkError error) {
            LOG.info("try manual way");

            useCPLEX = extractNativeResources();
            if (!useCPLEX) {

                //use this if you using maven assembly plugin with
                //              <descriptorRef>
                //                jar - with -dependencies
                //              </descriptorRef>

                LOG.info("Trying loading from jar instead");
                useCPLEX = loadFromJarFiles("cplex1260"); //could work if CPLEX was packed in the maven build file
                if (!useCPLEX) {
                    //still not, try loading alternative solver
                    loadLibraryFromJarFiles("lpsolve55j_x64");
                    useCPLEX = false;
                }

            }
        } catch (Exception e) {
            LOG.error("Could not load library :(, using LP_Solve instead");
        }
    }

    private static boolean extractNativeResources() {
        boolean cplex = false;
        for (String filename : NATIVE_LIB_FILENAMES) {
            final InputStream in = NativeLibraryLoader.class.getClassLoader().getResourceAsStream(filename);
            if (in != null) {
                try {
                    LOG.info("Extracting " + filename);
                    File destination = File.createTempFile(filename, ".so");
                    FileUtils.copyInputStreamToFile(in, destination);

                    System.load(destination.getAbsolutePath());
                    if (filename.contains("cplex")) {
                        cplex = true;
                    }
                } catch (IOException e) {
                    LOG.error("Can't extract " + filename, e);
                    e.printStackTrace();
                }
            }
        }
        return cplex;
    }

    /**
     * @param libname to be loaded,
     */
    @Deprecated
    private static boolean loadFromJarFiles(String libname) {
        try {

            String fileSeparator = System.getProperty("file.separator");
            String pathSeparator = System.getProperty("path.separator");
            String classpath = System.getProperty("java.class.path");
            String qualifiedLibname = System.mapLibraryName(libname);

            LOG.info("Checking paths and filenames:");
            LOG.info("Full library name is: " + qualifiedLibname);

            // We must search for the library in all jar-files, because we don't know in which jar-file the solver came
            Pattern filename = Pattern.compile(".*jar");
            String[] files = classpath.split(pathSeparator);
            LOG.info(files.length + " files in classpath");
            for (String s : files) {
                LOG.info(s);
            }
            // Searching for the libraries in the jars with a regexp enables that the libraries can be stored in a subdirectory
            Pattern ziplibname = Pattern.compile(".*" + qualifiedLibname);

            InputStream in = null;
            for (String s : files) {
                if (!s.contains("with-dependencies")) //only take the jar file containing that so, i.e. LPSOLVESolverPack.jar
                    continue;
                // Looking up all the contents of every jar-file
                if (s != null) {
                    if (!filename.matcher(s).matches())
                        continue;
                    LOG.info("Checking " + s);
                    try {

                        ZipFile jarfile = new ZipFile(s);
                        Enumeration<? extends ZipEntry> e = jarfile.entries();

                        while (e.hasMoreElements() && in == null) {
                            ZipEntry ze = e.nextElement();

                            if (ziplibname.matcher(ze.getName()).matches()) {
                                LOG.info("FOUND!!!");

                                in = jarfile.getInputStream(ze);
                            }
                        }
                        LOG.info("Finished " + s);
                    } catch (Exception e) {
                        LOG.info("An " + e.getMessage() + " exception occured while trying to open as a ZIP-File.");
                    }

                    if (in != null) {
                        LOG.info("File found in " + s);
                        break;
                    }
                }
            }

            if (in == null) {
                LOG.info("Could not find required library: " + libname);
                return false;
            }

            // Okay, we found the library
            // Now we have to write it to the file-system somewhere convenient
            // The following locations are tried in this order:
            // - any directory in the library path
            // - the current working directory
            // (- directories containing jar-files) <-- not implemented yet!
            // (- user.home) <-- not requested so far
            // - the system temp-directory

            // Trying library path locations:
            String libpath = System.getProperty("java.library.path");
            LOG.info("java.library.path is: " + libpath);

            if (libpath != null && libpath.length() > 0) {
                LOG.info("java.library.path seems to contain valid paths");

                String[] paths = libpath.split(pathSeparator);
                for (String fullname : paths) {
                    fullname = fullname + fileSeparator + qualifiedLibname;
                    if (writeInStreamToFile(in, fullname)) {
                        LOG.info("Wrote library to library path: " + fullname);
                        System.loadLibrary(libname);
                        return true;
                    }
                }
            } else {
                LOG.info("java.library.path contains no valid paths");
            }

            // Now test the rest locations ...
            String[] paths = new String[2];
            paths[0] = System.getProperty("user.dir");
            paths[1] = System.getProperty("java.io.tmpdir");

            for (String fullname : paths) {
                fullname = fullname + fileSeparator + qualifiedLibname;
                if (writeInStreamToFile(in, fullname)) {
                    LOG.info("Wrote library to regular path: " + fullname);
                    System.load(fullname);
                    return true;
                }
            }
            // No more locations to write file
            return false;

        } catch (Exception e) {
            // Looking up jar-files failed
            System.err.println("An error occured while searching for the library in the solver module:");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Tries to write the input stream to the location specified in filename. If
     * copying was successful, the method returns true, otherwise it returns false.
     *
     * @param in       the input stream containing the source file
     * @param filename the destiny location the file is to be written to
     * @return <code>true</code> if copying was successful, <code>false</code> otherwise
     */
    private static boolean writeInStreamToFile(InputStream in, String filename) {
        try {
            File f = new File(filename);
            if (f.exists()) {
                return true;
            }
            // Opening an appropriate output stream and copy the library
            FileOutputStream fileout = new FileOutputStream(filename);
            BufferedOutputStream out = new BufferedOutputStream(fileout);

            byte[] buffer = new byte[1024];
            int len;

            while ((len = in.read(buffer)) >= 0)
                out.write(buffer, 0, len);

            in.close();
            out.close();

            LOG.info("Copied file successfully to: " + filename);

            return true;

        } catch (IOException e) {
            LOG.info("Failed to write file to: " + filename);
            return false;
        }
    }

    /**
     * @param libname to be loaded,
     *                Only works for LPSOLVE libraries
     */
    private static void loadLibraryFromJarFiles(String libname) {
        try {

            String fileSeparator = System.getProperty("file.separator");
            String pathSeparator = System.getProperty("path.separator");
            String classpath = System.getProperty("java.class.path");
            String qualifiedLibname = System.mapLibraryName(libname);

            System.out.println("Checking paths and filenames:");
            System.out.println("Full library name is: " + qualifiedLibname);

            // We must search for the library in all jar-files, because we don't know in which jar-file the solver came
            Pattern filename = Pattern.compile(".*jar");
            String[] files = classpath.split(pathSeparator);
            System.out.println(files.length + " files in classpath");
            for (String s : files) {
                System.out.println(s);
            }
            // Searching for the libraries in the jars with a regexp enables that the libraries can be stored in a subdirectory
            Pattern ziplibname = Pattern.compile(".*" + qualifiedLibname);

            InputStream in = null;
            for (String s : files) {
                if (!s.contains("LPSOLVE")) //only take the jar file containing that so, i.e. LPSOLVESolverPack.jar
                    continue;
                // Looking up all the contents of every jar-file
                if (s != null) {
                    if (!filename.matcher(s).matches())
                        continue;
                    System.out.println("Checking " + s);
                    try {

                        ZipFile jarfile = new ZipFile(s);
                        Enumeration<? extends ZipEntry> e = jarfile.entries();

                        while (e.hasMoreElements() && in == null) {
                            ZipEntry ze = e.nextElement();

                            //System.out.println("Current file is: " + ze.getName(), 2);
                            if (ziplibname.matcher(ze.getName()).matches()) {
                                System.out.println("FOUND!!!");

                                in = jarfile.getInputStream(ze);
                            }
                        }
                        System.out.println("Finished " + s);
                    } catch (Exception e) {
                        System.out.println("An " + e.getMessage() + " exception occured while trying to open as a ZIP-File.");
                    }

                    if (in != null) {
                        System.out.println("File found in " + s);
                        break;
                    }
                }
            }

            if (in == null) {
                System.out.println("Could not find required library: " + libname);
                return;
            }

            // Okay, we found the library
            // Now we have to write it to the file-system somewhere convenient
            // The following locations are tried in this order:
            // - any directory in the library path
            // - the current working directory
            // (- directories containing jar-files) <-- not implemented yet!
            // (- user.home) <-- not requested so far
            // - the system temp-directory

            // Trying library path locations:
            String libpath = System.getProperty("java.library.path");
            System.out.println("java.library.path is: " + libpath);

            if (libpath != null && libpath.length() > 0) {
                System.out.println("java.library.path seems to contain valid paths");

                String[] paths = libpath.split(pathSeparator);
                for (String fullname : paths) {
                    fullname = fullname + fileSeparator + qualifiedLibname;
                    if (writeInStreamToFile(in, fullname)) {
                        System.out.println("Wrote library to library path: " + fullname);
                        System.loadLibrary(libname);
                        return;
                    }
                }
            } else {
                System.out.println("java.library.path contains no valid paths");
            }

            // Now test the rest locations ...
            String[] paths = new String[2];
            paths[0] = System.getProperty("user.dir");
            paths[1] = System.getProperty("java.io.tmpdir");

            for (String fullname : paths) {
                fullname = fullname + fileSeparator + qualifiedLibname;
                if (writeInStreamToFile(in, fullname)) {
                    System.out.println("Wrote library to regular path: " + fullname);
                    System.load(fullname);
                    return;
                }
            }
            // No more locations to write file
            return;

        } catch (Exception e) {
            // Looking up jar-files failed
            System.err.println("An error occured while searching for the library in the solver module:");
            e.printStackTrace();
        }

    }



}
