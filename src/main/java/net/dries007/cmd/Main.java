/*
 * Copyright 2017 Dries007
 *
 * Licensed under the EUPL, Version 1.1 only (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package net.dries007.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;

import java.io.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static net.dries007.cmd.Helper.ARGUMENTS;

/**
 * @author Dries007
 */
public class Main
{
    private static String dependencies;
    private static JCommander jCommander = new JCommander();

    static
    {
        try
        {
            Manifest manifest = new Manifest(Main.class.getResourceAsStream("/" + JarFile.MANIFEST_NAME));
            dependencies = manifest.getMainAttributes().getValue("Dependencies");
        }
        catch (IOException e)
        {
            System.err.println("Error: Could not open " + JarFile.MANIFEST_NAME + ". Please contact the distributor.");
            System.err.println("This software may be distributed against the terms of the License.");
            e.printStackTrace();
        }

        jCommander.setAllowParameterOverwriting(true);
        jCommander.setProgramName(Helper.NAME);
        jCommander.setColumnSize(Integer.MAX_VALUE);

        jCommander.addObject(ARGUMENTS);
        jCommander.addCommand(ARGUMENTS.client);
        jCommander.addCommand(ARGUMENTS.server);
    }

    public static void main(String[] args) throws FileNotFoundException
    {
        try
        {
            jCommander.parse(args);

            if (ARGUMENTS.help)
            {
                printHelp();
            }

            ARGUMENTS.validate(jCommander.getParsedCommand());
        }
        catch (ParameterException e)
        {
            System.out.println("ERROR: " + e.getMessage());
            printHelp();
        }

        Worker w = new Worker(ARGUMENTS);

        FileOutputStream fos = null;
        if (ARGUMENTS.log != null && !ARGUMENTS.quiet)
        {
            fos = new FileOutputStream(ARGUMENTS.log);
            w.setLogger(new PrintStream(new TeeOutputStream(System.out, fos)));
        }

        w.run();

        IOUtils.closeQuietly(fos);
    }

    private static void printHelp()
    {
        // Inform people who like GUIs
        System.out.println("For a GUI: Do not use any arguments.");

        // Print usage
        jCommander.usage();

        // Print dependencies
        if (dependencies != null)
        {
            System.out.println("Libraries required or included:");
            for (String dep : dependencies.split(","))
            {
                System.out.println("    " + dep);
            }
        }
        else
        {
            System.out.println("Error: No dependency information found.");
        }

        // Print License
        try
        {
            InputStream is = Main.class.getResourceAsStream("LICENSE-HEADER");
            IOUtils.copy(is, System.out);
            IOUtils.closeQuietly(is);
        }
        catch (IOException e)
        {
            System.err.println("Error: Could not open LICENSE-HEADER. Please contact the distributor.");
            System.err.println("This software may be distributed against the terms of the License.");
            e.printStackTrace();
        }

        Runtime.getRuntime().exit(0);
    }
}
