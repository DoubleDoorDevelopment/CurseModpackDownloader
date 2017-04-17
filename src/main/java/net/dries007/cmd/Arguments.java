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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Arguments
{
    public final Client client = new Client();
    public final Server server = new Server();

    @Parameter(names = {"--help", "-h", "-?"}, description = "Display this text.", help = true)
    public boolean help = false;

    @Parameter(names = {"-i", "--input"}, description = "Input file/URL (CurseForge modpack zip)", required = true)
    public String input;

    @Parameter(names = {"-o", "--output"}, description = "Output folder or zip file.", required = true)
    public File output;

    @Parameter(names = {"-d", "--delete"}, description = "If output is folder, delete the contents of any root folder specified in the pack.")
    public boolean delete = false;

    @Parameter(names = {"-w", "--override"}, description = "If output is folder, don't check to see if folders specified in the pack are empty. If output is a zip, override if it exists.")
    public boolean override = false;

    @Parameter(names = {"-j", "--threads"}, description = "Number of parallel downloaders to use.")
    public int threads = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"-q", "--quiet"}, description = "Quiet. Don't output status information, make no log.")
    public boolean quiet = false;

    @Parameter(names = {"--tmp"}, description = "Use a specific tmp folder. Must exist. Defaults to 'java.io.tmpdir'")
    public File tmp = new File(System.getProperty("java.io.tmpdir"));

    @Parameter(names = {"--no-forge"}, description = "Skip forge download (and install on servers).")
    public boolean noForge = false;

    @Parameter(names = {"--keep-tmp"}, description = "Keep the tmp folder after the program has exited.")
    public boolean keepTmp = false;

    @Parameter(names = {"--root-zip"}, description = "Put the pack in the root of the zip. (Only useful when output is zip)")
    public boolean rootZip = false;

    @Parameter(names = {"--name"}, description = "Override the name. (Only useful when MultiMC or zipping without --root-zip)")
    public String name;

    @Parameter(names = {"-m", "--magic"}, description = "Use magic to try and download all mods. Uses external API hosted by Dries007.")
    public boolean magic = false;

    public boolean isClient;
    public boolean zipOutput;
    public boolean isInputURL;
    private boolean validated;

    /**
     * Validate and set flags isClient, zipOutput, ...
     *
     * @param command 'client' or 'server', normally supplied from jCommander's getParsedCommand().
     */
    public void validate(String command)
    {
        if (command == null) throw new ParameterException("You must used a command.");

        if (delete) override = true;

        try
        {
            input = Helper.getFinalURL(new URL(input).toString());
            if (!FilenameUtils.getExtension(input).toLowerCase().equals("zip"))
            {
                throw new ParameterException("Input URL does not lead to 'zip' file.");
            }
            isInputURL = true;
        }
        catch (IOException e)
        {
            input = new File(input).getAbsolutePath();
        }

        output = output.getAbsoluteFile();
        tmp = tmp.getAbsoluteFile();

        if (threads < 1)
        {
            throw new ParameterException("You can't have less than 1 download thread.");
        }

        // Check output
        if (output.exists())
        {
            if (output.isDirectory())
            {
                //noinspection ConstantConditions
                if (output.list().length != 0 && !override)
                {
                    throw new ParameterException("The output folder is not empty and no delete/override allowed.");
                }
            }
            else if (output.isFile())
            {
                if (!override)
                {
                    throw new ParameterException("The output file exists and no delete/override allowed.");
                }
                if (!FilenameUtils.getExtension(output.getName()).equals("zip"))
                {
                    throw new ParameterException("The output file is not a zip file.");
                }
                zipOutput = true;
                //noinspection ResultOfMethodCallIgnored
                output.getParentFile().mkdirs();
            }
            else
            {
                throw new ParameterException("The output exits, but is not a file or folder.");
            }
        }
        else // output doesn't exist
        {
            String ext = FilenameUtils.getExtension(output.getName()).toLowerCase();
            switch (ext)
            {
                case "":
                    //noinspection ResultOfMethodCallIgnored
                    output.mkdirs();
                    if (!output.exists()) throw new ParameterException("Could not create output dir.");
                    break;
                case "zip":
                    zipOutput = true;
                    //noinspection ResultOfMethodCallIgnored
                    output.getParentFile().mkdirs();
                    if (!output.getParentFile().exists())
                        throw new ParameterException("Could not create output's parent dir.");
                    break;
                default:
                    throw new ParameterException("The output is not a folder or a zip file.");
            }
        }

        // Command specific stuff
        isClient = command.equals("client");
        if (isClient)
        {
            server.eula = false;
        }
        else
        {
            client.multimc = false;
        }

        this.validated = true;
    }

    public boolean isValidated()
    {
        return validated;
    }

    @Parameters(commandNames = "client", commandDescription = "Create a client side pack.")
    public static class Client
    {
        @Parameter(names = {"--multimc"}, description = "Create a MultiMC instance.")
        public boolean multimc;

        private Client() {}
    }

    @Parameters(commandNames = "server", commandDescription = "Create a server side pack.")
    public static class Server
    {
        @Parameter(names = {"--eula"}, description = "Include a EULA file. Only use if _you_ are the end user, and you agree with the Mojang EULA!")
        public boolean eula = false;

        private Server() {}
    }
}
