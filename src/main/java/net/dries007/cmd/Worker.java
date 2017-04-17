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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dries007.cmd.util.DeleteOnExit;
import net.dries007.cmd.util.ModpackException;
import net.dries007.cmd.util.ProgressMonitorLogger;
import net.dries007.cmd.util.forge.ForgeBuild;
import net.dries007.cmd.util.forge.ForgeFile;
import net.dries007.cmd.util.forge.ForgeJson;
import net.dries007.cmd.util.manifest.CurseFile;
import net.dries007.cmd.util.manifest.Manifest;
import net.dries007.cmd.util.manifest.Modloader;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dries007
 */
public class Worker implements Runnable
{
    private static final String URL_FORGE_MAVEN = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/";
    private final static String URL_FORGE_JSON = URL_FORGE_MAVEN + "json";
    private final static String URL_MAGIC = "https://cursemeta.dries007.net/";

    private final Arguments arguments;
    private final File tmp;
    private final File tmpUnzip;
    private final File tmpDownload;
    private final File tmpOut;

    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    private final AtomicBoolean done = new AtomicBoolean(false);

    // Strange situations handling
    private final Queue<CurseFile> failedToDownload = new ConcurrentLinkedQueue<>();
    private final List<String> nonForgeModloaders = new ArrayList<>();

    // For use outside of the CLI
    private PrintStream logger = System.out;
    private Throwable error;

    // Only set after the appropriate stage is done
    private Manifest manifest;
    private String name;

    @SuppressWarnings({"ResultOfMethodCallIgnored", "WeakerAccess"})
    public Worker(Arguments arguments)
    {
        this.arguments = arguments;

        if (!arguments.isValidated())
        {
            throw new IllegalArgumentException("Arguments where not validated!");
        }

        int i = 0;
        File tmp;
        do
        {
            tmp = new File(arguments.tmp, Helper.NAME + "_" + (i++));
        }
        while (tmp.exists());

        this.tmp = tmp;
        tmp.mkdirs();

        tmpUnzip = new File(tmp, "unzip");
        tmpUnzip.mkdir();

        tmpDownload = new File(tmp, "download");
        tmpDownload.mkdir();

        tmpOut = new File(tmp, "out");
        tmpOut.mkdir();

        if (!arguments.keepTmp)
        {
            DeleteOnExit.add(tmp);
        }

        if (!tmp.isDirectory() || !tmpUnzip.isDirectory() || !tmpDownload.isDirectory() || !tmpOut.isDirectory())
        {
            throw new IllegalArgumentException("The tmp directories couldn't be created: " + tmp);
        }
    }

    /**
     * Does most of the actual work, in order:
     * - Unpack & parse manifest
     * - Unpack zip (threaded)
     * - Download all mods (threaded)
     * - Do sided stuff (download & install forge if required; make multimc instance file)
     * - Move/zip from tmp to output.
     */
    private void work() throws Throwable
    {
        if (arguments.isInputURL)
        {
            if (!arguments.quiet)
            {
                logger.println("Downloading pack from URL: " + arguments.input);
            }
            File input = new File(tmp, FilenameUtils.getName(arguments.input));
            FileUtils.copyURLToFile(new URL(arguments.input), input);
            arguments.input = input.getAbsolutePath();
        }
        ProgressMonitor unzip = doUnpack();

        if (!arguments.quiet)
        {
            logger.println("Total mod count: " + manifest.files.size());
        }

        // counter keeps track of download index for load sharing.
        final AtomicInteger counter = new AtomicInteger(0);
        // keep thread objects, so we can wait on them later.
        final Thread[] downloaders = new Thread[arguments.threads];
        for (int i = 0; i < arguments.threads; i++)
        {
            downloaders[i] = new Thread(new Downloader(counter), "Downloader-" + i);
            downloaders[i].setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                @Override
                public void uncaughtException(Thread t, Throwable e)
                {
                    error = e;
                }
            });
            downloaders[i].start();
        }

        // Forge related stuff
        if (!arguments.noForge)
        {
            String forgeVersion = getForgeVersion(manifest);
            if (forgeVersion != null)
            {
                ForgeJson forgeJson = downloadForgeJson();
                if (error != null) throw error;
                manifest.forgeBuild = resolveForgeBuild(forgeJson, forgeVersion);
                if (error != null) throw error;
                if (!arguments.client.multimc)
                {
                    File forge = downloadForgeInstaller(forgeJson);
                    if (error != null) throw error;
                    if (!arguments.isClient)
                    {
                        if (forge != null)
                        {
                            doForgeInstall(forge);
                            if (error != null) throw error;
                        }
                        else
                        {
                            nonForgeModloaders.add("forge-no-installer-" + forgeVersion);
                        }
                    }
                }
            }
        }

        // wait on all threads to finish
        while (unzip.getState() != ProgressMonitor.STATE_READY)
        {
            Helper.sleep(10);
        }
        if (error != null) throw error;
        if (unzip.getResult() != ProgressMonitor.RESULT_SUCCESS)
        {
            throw new IOException("Couldn't unzip the input...", unzip.getException());
        }
        for (Thread thread : downloaders)
        {
            thread.join();
        }
        if (error != null) throw error;
        doOutput();
    }

    private void doOutput() throws IOException, ZipException
    {
        File tmpOut = this.tmpOut;
        if (arguments.isClient)
        {
            if (arguments.client.multimc)
            {
                tmpOut = makeMultiMCInstance();
            }
        }
        else if (arguments.server.eula)
        {
            FileUtils.writeStringToFile(new File(tmpOut, "eula.txt"), "#Accepted via CurseModpackDownloader v1.x\n#https://account.mojang.com/documents/minecraft_eula\n#" + new Date().toString() + "\neula=true");
        }
        FileUtils.copyDirectory(tmpDownload, new File(tmpOut, "mods"));
        FileUtils.copyDirectory(new File(tmpUnzip, manifest.overrides), tmpOut);

        if (arguments.zipOutput)
        {
            doOutputZip();
        }
        else
        {
            doOutputFolder();
        }
    }

    private void doOutputFolder() throws IOException
    {
        File[] alreadyInOutput = arguments.output.listFiles();
        // Already checked by Argument's validate. If we got here, it's a folder so never null.
        //noinspection ConstantConditions
        if (alreadyInOutput.length == 0)
        {
            // Shortcut if the output folder is empty.
            FileUtils.copyDirectory(tmpOut, arguments.output);
            return;
        }
        //noinspection ConstantConditions
        for (File in : tmpOut.listFiles())
        {
            if (in.isDirectory())
            {
                File out = new File(arguments.output, in.getName());
                if (out.exists() && arguments.delete)
                {
                    FileUtils.deleteDirectory(out);
                }
                FileUtils.copyDirectory(in, out);
            }
            else
            {
                FileUtils.copyFileToDirectory(in, arguments.output);
            }
        }
    }

    private void doOutputZip() throws IOException, ZipException
    {
        // Argument's validate would have failed if we weren't allowed to override.
        if (arguments.output.exists())
        {
            if (!arguments.output.delete())
            {
                throw new IOException("Could not delete existing output zip " + arguments.output);
            }
        }

        boolean wrap = !(arguments.rootZip || arguments.client.multimc);

        File tmpOut = this.tmpOut;
        if (wrap)
        {
            tmpOut = new File(tmpOut.getParentFile(), name);
            if (!this.tmpOut.renameTo(tmpOut))
            {
                throw new IOException("Could not rename " + this.tmpOut + " to " + tmpOut);
            }
        }

        ZipFile outputZip = new ZipFile(arguments.output);
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        zipParameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        zipParameters.setIncludeRootFolder(wrap);

        outputZip.setRunInThread(true);
        outputZip.addFolder(tmpOut, zipParameters);

        if (!arguments.quiet)
        {
            new Thread(new ProgressMonitorLogger(logger, outputZip.getProgressMonitor(), "Zipping"), "zipping-monitor").start();
        }

        ProgressMonitor zip = outputZip.getProgressMonitor();

        // wait on all threads to finish
        while (zip.getState() != ProgressMonitor.STATE_READY)
        {
            Helper.sleep(10);
        }
        if (zip.getResult() != ProgressMonitor.RESULT_SUCCESS)
        {
            throw new IOException("Couldn't zip the output...", zip.getException());
        }
    }

    private void doForgeInstall(File forge) throws IOException, InterruptedException
    {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", forge.getName(), "--installServer");

        // Print out the cmd line
        if (!arguments.quiet)
        {
            StringBuilder joiner = new StringBuilder("Running forge installer with command: ");
            for (String cmd : processBuilder.command())
            {
                joiner.append(cmd).append(' ');
            }
            logger.println(joiner.toString());
        }

        // Start process in output folder & make sure all output is caught
        processBuilder.directory(tmpOut);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Log installer output
        if (!arguments.quiet)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null)
            {
                logger.println(line);
            }
        }

        // Wait for exit & delete installer on success
        if (process.waitFor() == 0)
        {
            if (forge.exists())
            {
                //noinspection ResultOfMethodCallIgnored
                forge.delete();
            }
        }
        else
        {
            logger.println("WARNING: Forge installer had non-zero exit. Possible errors installing Forge.");
        }
    }

    private ForgeBuild resolveForgeBuild(ForgeJson forgeJson, String forgeVersion)
    {
        if (forgeVersion.equalsIgnoreCase("forge-recommended") || forgeVersion.equalsIgnoreCase("forge-latest"))
        {
            return forgeJson.number.get(String.valueOf(forgeJson.promos.get(forgeVersion.replace("forge", manifest.manifestVersion))));
        }
        else
        {
            return forgeJson.number.get(forgeVersion.substring(forgeVersion.lastIndexOf('.') + 1));
        }
    }

    private ProgressMonitor doUnpack() throws ZipException, IOException, ModpackException
    {
        final File manifestFile = new File(tmpUnzip, "manifest.json");

        // Unpack input to temp folder, but do manifest first, so we can parse that ASAP
        final ZipFile zipFile = new ZipFile(arguments.input);
        FileHeader manifestHeader = zipFile.getFileHeader(manifestFile.getName());
        if (manifestHeader == null) throw new IOException("There is no manifest in the zip.");
        zipFile.extractFile(manifestHeader, tmpUnzip.getPath());

        final String manifestString = FileUtils.readFileToString(manifestFile);

        { // Quick check for version
            JsonObject root = new JsonParser().parse(manifestString).getAsJsonObject();
            if (root.getAsJsonPrimitive("manifestVersion").getAsInt() != 1)
            {
                throw new ModpackException.ManifestInvalidException("Version mismatch. Only '1' supported in this version.");
            }

            if (!root.getAsJsonPrimitive("manifestType").getAsString().equals("minecraftModpack"))
            {
                throw new ModpackException.ManifestInvalidException("Type must be 'minecraftModpack'");
            }
        }

        // unzip everything else, in a thread
        zipFile.setRunInThread(true);
        zipFile.extractAll(tmpUnzip.getPath());
        if (!arguments.quiet)
        {
            new Thread(new ProgressMonitorLogger(logger, zipFile.getProgressMonitor(), "Unzipping"), "unzipping-monitor").start();
        }

        // Do proper manifest parse, string is still from before, so we don't have to worry about the unzipping.
        manifest = Helper.GSON.fromJson(manifestString, Manifest.class);
        // Don't wanna start more threads than we have mods to download.
        arguments.threads = Math.min(manifest.files.size(), arguments.threads);
        name = arguments.name != null ? arguments.name : manifest.name;

        return zipFile.getProgressMonitor();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File makeMultiMCInstance() throws IOException
    {
        File instanceFolder = new File(tmpOut, name);
        instanceFolder.mkdir();

        StringBuilder sb = new StringBuilder("InstanceType=OneSix\nIntendedVersion=").append(manifest.minecraft.version).append("\nname=").append(name);
        if (manifest.forgeBuild != null)
        {
            sb.append("\nForgeVersion=").append(manifest.forgeBuild.mcversion).append('-').append(manifest.forgeBuild.version);
            if (manifest.forgeBuild.branch != null)
            {
                sb.append('-').append(manifest.forgeBuild.branch);
            }
        }
        FileUtils.writeStringToFile(new File(instanceFolder, "instance.cfg"), sb.append('\n').toString());
        File mcFolder = new File(instanceFolder, "minecraft");
        mcFolder.mkdir();
        return mcFolder;
    }

    private String getForgeVersion(Manifest manifest) throws ModpackException
    {
        String out = null;
        for (Modloader modloader : manifest.minecraft.modLoaders)
        {
            if (modloader.id.startsWith("forge-"))
            {
                if (out != null)
                {
                    throw new ModpackException("Multiple forge versions. Invalid pack.");
                }
                out = modloader.id.substring(modloader.id.indexOf('-') + 1);
                if (!arguments.quiet)
                {
                    logger.println("Found forge version: " + out);
                }
            }
            else
            {
                logger.println("WARNING: YOU NEED TO MANUALLY INSTALL THIS MODLOADER: " + modloader.id);
                nonForgeModloaders.add(modloader.id);
            }
        }
        return out;
    }

    private ForgeJson downloadForgeJson() throws IOException
    {
        if (!arguments.quiet)
        {
            logger.println("Getting forge version list json...");
        }
        try
        {
            return Helper.parseJson(URL_FORGE_JSON, ForgeJson.class);
        }
        catch (Exception e)
        {
            logger.println("ERROR Forge JSON download. Something random went wrong, you'll have to install forge yourself: " + e.getMessage());
        }
        return null;
    }

    private File downloadForgeInstaller(ForgeJson forgeJson) throws IOException
    {
        if (!arguments.quiet)
        {
            logger.println("Actual forge build: " + manifest.forgeBuild);
        }

        for (ForgeFile file : manifest.forgeBuild.files)
        {
            if (file.type.equalsIgnoreCase("installer"))
            {
                StringBuilder urlString = new StringBuilder(URL_FORGE_MAVEN);
                urlString.append(manifest.forgeBuild.mcversion).append('-').append(manifest.forgeBuild.version);
                if (manifest.forgeBuild.branch != null)
                {
                    urlString.append('-').append(manifest.forgeBuild.branch);
                }
                urlString.append('/').append(forgeJson.artifact).append('-').append(manifest.forgeBuild.mcversion).append('-').append(manifest.forgeBuild.version);
                if (manifest.forgeBuild.branch != null)
                {
                    urlString.append('-').append(manifest.forgeBuild.branch);
                }
                urlString.append('-').append(file.type).append('.').append(file.extention);
                URL url = new URL(urlString.toString());
                File installer = new File(tmpOut, FilenameUtils.getName(url.getFile()));
                if (!arguments.quiet)
                {
                    logger.println("Downloading forge installer " + installer.getName());
                }
                FileUtils.copyURLToFile(url, installer);
                return installer;
            }
        }

        logger.println("ERROR Forge version: " + manifest.forgeBuild + " has no installer");
        return null;
    }

    @Override
    public void run()
    {
        if (hasRun.get()) throw new IllegalStateException("Workers can only be run once.");
        hasRun.set(true);
        try
        {
            work();

            logger.flush();

            if (nonForgeModloaders.isEmpty())
            {
                logger.println("MODLOADERS OK");
            }
            else
            {
                logger.println("MODLOADERS MISSING:");
                for (String ml : nonForgeModloaders)
                {
                    logger.println(ml);
                }
            }

            if (failedToDownload.isEmpty())
            {
                logger.println("DOWNLOADS OK");
            }
            else
            {
                logger.println("DOWNLOADS MISSING:");
                for (CurseFile curseFile : failedToDownload)
                {
                    curseFile.file = null;
                    logger.println(curseFile);
                }
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            this.error = e;
        }
        done.set(true);
    }

    public Throwable getError()
    {
        return error;
    }

    public boolean hasRun()
    {
        return hasRun.get();
    }

    public boolean isDone()
    {
        return done.get();
    }

    public void setLogger(PrintStream logger)
    {
        if (hasRun.get()) throw new IllegalStateException("You can't change the logger while or after running.");
        this.logger = logger;
    }

    public List<CurseFile> getFailedMods()
    {
        if (!done.get()) throw new IllegalStateException("You can't get the list of failed mods before/while running.");
        return new ArrayList<>(failedToDownload);
    }

    public List<String> getNonForgeModsloaders()
    {
        if (!done.get())
            throw new IllegalStateException("You can't get the list of non Forge modloaders before/while running.");
        return new ArrayList<>(nonForgeModloaders);
    }

    private class Downloader implements Runnable
    {
        private final AtomicInteger counter;
        private final int max;

        Downloader(AtomicInteger counter)
        {
            this.counter = counter;
            this.max = manifest.files.size();
        }

        @Override
        public void run()
        {
            while (error == null)
            {
                int nextFile = counter.getAndIncrement();
                if (nextFile >= max) break; // work done
                CurseFile curseFile = manifest.files.get(nextFile);
                try
                {
                    if (arguments.magic)
                    {
                        try
                        {
                            JsonObject project = Helper.parseJson(URL_MAGIC + curseFile.projectID + ".json").getAsJsonObject();
                            JsonObject file = Helper.parseJson(URL_MAGIC + curseFile.projectID + "/" + curseFile.fileID + ".json").getAsJsonObject();

                            curseFile.projectName = project.get("Name").getAsString();
                            curseFile.fileName = file.get("FileNameOnDisk").getAsString();
                            String rawURL = file.get("DownloadURL").getAsString();
                            curseFile.url = FilenameUtils.getFullPath(rawURL) + URLEncoder.encode(FilenameUtils.getName(rawURL), "UTF-8").replace("+", "%20");
                            logger.println(rawURL);
                            logger.println(curseFile.url);
                        }
                        catch (IOException ignored)
                        {
                            if (!arguments.quiet)
                            {
                                logger.printf("Mod %3d: %10d %10d No magic. Trying CurseForge...\n", nextFile + 1, curseFile.projectID, curseFile.fileID);
                            }
                        }
                    }

                    if (curseFile.url == null)
                    {
                        curseFile.projectName = Helper.getProjectName(curseFile.projectID);
                        curseFile.url = Helper.getFileURL(curseFile.projectName, curseFile.fileID);
                        if (curseFile.url == null)
                        {
                            throw new IOException("File no longer available via CurseForge.");
                        }
                        curseFile.fileName = URLDecoder.decode(FilenameUtils.getName(curseFile.url), "UTF-8");
                    }

                    curseFile.file = new File(tmpDownload, curseFile.fileName);

                    if (!arguments.quiet)
                    {
                        logger.printf("Mod %3d: %10d %10d '%s' '%s' Url '%s'\n", nextFile + 1, curseFile.projectID, curseFile.fileID, curseFile.projectName, curseFile.fileName, curseFile.url);
                    }

                    FileUtils.copyURLToFile(new URL(curseFile.url), curseFile.file);
                }
                catch (IOException e)
                {
                    failedToDownload.add(curseFile);
                    logger.printf("Mod %3d: %10d %10d '%s' '%s' ERROR: %s (%s)\n", nextFile + 1, curseFile.projectID, curseFile.fileID, curseFile.projectName, curseFile.fileName, e.getClass().getName(), e.getMessage());
                }
            }
        }
    }
}
