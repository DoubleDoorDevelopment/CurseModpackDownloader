/*
 *     CurseModpackDownloader
 *     Copyright (C) 2015  Dries007 & Double Door Development
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.doubledoordev.cmd;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.doubledoordev.cmd.forge.ForgeBuild;
import net.doubledoordev.cmd.forge.ForgeFile;
import net.doubledoordev.cmd.forge.ForgeFileJson;
import net.doubledoordev.cmd.forge.ForgeJson;
import net.doubledoordev.cmd.manifest.CurseFile;
import net.doubledoordev.cmd.manifest.Manifest;
import net.doubledoordev.cmd.manifest.Modloader;
import net.doubledoordev.cmd.util.InputFileValidator;
import net.doubledoordev.cmd.util.OutputFileValidator;
import net.doubledoordev.cmd.util.PositiveIntegerValidator;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dries007
 */
public class CurseModpackDownloader
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeHierarchyAdapter(ForgeFile.class, new ForgeFileJson()).create();
    /**
     * Dummy variable, used in Main
     */
    @Parameter(names = {"-help", "-?"}, description = "Display help.", help = true)
    public boolean help = false;

    @Parameter(names = {"-server", "-s"}, description = "Install a server.")
    public boolean server = false;

    @Parameter(names = {"-eula"}, description = "Write a eula file, if you are installing a server. This indicates your agreement.")
    public boolean eula = false;

    @Parameter(names = {"-ignoreExistingMods", "-iem"}, description = "Ignore that the mods folder is not empty.")
    public boolean ignoreExistingMods = false;

    @Parameter(names = {"-input", "-i"}, description = "The curse zip or json file.", validateValueWith = InputFileValidator.class, arity = 1)
    public File input = new File("manifest.json");

    @Parameter(names = {"-output", "-o"}, description = "The output folder", validateValueWith = OutputFileValidator.class, arity = 1)
    public File output = new File(".");

    @Parameter(names = {"-threads", "-t"}, description = "# of parallel download threads", validateValueWith = PositiveIntegerValidator.class, arity = 1)
    public int threads = 4;

    public PrintStream logger = System.out;

    private boolean inputEqualsOutput;
    final AtomicInteger currentFile = new AtomicInteger();
    private Manifest manifest;
    private File modsFolder;
    private ForgeJson forgeJson;
    private Thread[] threadObjects;
    private String mcVersion;
    private String forgeVersion;
    private File manifestFile;
    private Process installerProcess;
    private File installerFile;

    public CurseModpackDownloader()
    {
    }

    public void run() throws IOException, ZipException
    {
        final long start = System.currentTimeMillis();
        inputCheck();

        manifestFile = new File(output, "manifest.json");
        inputEqualsOutput = manifestFile.getCanonicalPath().equals(input.getCanonicalPath());
        if (!inputEqualsOutput && manifestFile.exists()) manifestFile.delete();

        unpackOrMoveJson();
        startModDownloadThreads();

        mcVersion = manifest.minecraft.version;
        forgeVersion = getForgeVersion();

        if (forgeVersion != null)
        {
            downloadForgeJson();
            if (forgeJson != null)
            {
                installerFile = downloadForgeInstaller();
                if (installerFile != null && server)
                {
                    installerProcess = runForgeInstaller(installerFile);
                }
            }
        }
        else
        {
            URL mcServerJar = new URL("http://s3.amazonaws.com/Minecraft.Download/versions/" + mcVersion + "/minecraft_server." + mcVersion + ".jar");
            FileUtils.copyURLToFile(mcServerJar, new File(output, FilenameUtils.getName(mcServerJar.getFile())));
        }

        if (server && eula)
        {
            FileUtils.writeStringToFile(new File(output, "eula.txt"), "#No bullshit EULA file, courtesy of CurseModpackDownloader\n#https://account.mojang.com/documents/minecraft_eula\n#" + new Date().toString() + "\neula=true");
        }

        waitTillDone();
        writeModpackinfo();

        logger.println("Done downloading mods.");

        if (!server)
        {
            logger.println("You need to manually install the client, if applicable, the forge installer has already been downloaded to the output directory.");
        }
        long time = System.currentTimeMillis() - start;
        logger.println(String.format("Total time to completion: %.2f seconds", time / 1000.0));
    }

    private void writeModpackinfo()
    {
        PrintWriter pw = null;
        try
        {
            File info = new File(output, "CurseModpackDownloader.txt");
            if (info.exists()) info.delete();
            info.createNewFile();
            pw = new PrintWriter(info);

            pw.println("CurseModpackDownloader Modpack information file");
            pw.println("===============================================");
            pw.print("Downloaded date/time: ");
            pw.println(new Date().toString());
            pw.print("Minecraft verion: ");
            pw.println(manifest.minecraft.version);
            pw.print("Pack name: ");
            pw.println(manifest.name);
            pw.print("Pack version: ");
            pw.println(manifest.version);
            pw.print("Pack author: ");
            pw.println(manifest.author);
            pw.println("Modloaders:");
            pw.println("-----------");
            for (Modloader modloader : manifest.minecraft.modLoaders)
            {
                pw.print('\t');
                pw.print(modloader.id);
                pw.print("    Primary: ");
                pw.println(modloader.primary);
            }
            pw.println("Forge Mods: (project id -> project name    file id -> file name)");
            pw.println("----------------------------------------------------------------");
            for (CurseFile mod : manifest.files)
            {
                pw.print(String.format("%10d: %-40s %10d: %-50s", mod.projectID, mod.projectName, mod.fileID, mod.fileName));
                if (mod.required) pw.println(" Required.");
                else pw.println();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly(pw);
        }
    }

    private Process runForgeInstaller(File file) throws IOException
    {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", file.getName(), "--installServer");
        StringBuilder joiner = new StringBuilder("Running forge installer with command: ");
        for (String cmd : processBuilder.command()) joiner.append(cmd).append(' ');
        logger.println(joiner.toString());
        processBuilder.directory(output);
        processBuilder.inheritIO();
        return processBuilder.start();
    }

    private File downloadForgeInstaller() throws IOException
    {
        ForgeBuild forgeBuild;
        if (forgeVersion.equalsIgnoreCase("forge-recommended") || forgeVersion.equalsIgnoreCase("forge-latest")) forgeBuild = forgeJson.number.get(String.valueOf(forgeJson.promos.get(forgeVersion.replace("forge", manifest.manifestVersion))));
        else forgeBuild = forgeJson.number.get(forgeVersion.substring(forgeVersion.lastIndexOf('.') + 1));
        if (forgeBuild == null)
        {
            logger.println("======================================================================");
            logger.println("Something screwed up the forge installation. You will have to do it manually.");
            logger.println("Forge version: " + forgeVersion);
            logger.println("======================================================================");
            return null;
        }

        for (ForgeFile file : forgeBuild.files)
        {
            if (file.type.equalsIgnoreCase("installer"))
            {
                StringBuilder urlString = new StringBuilder("http://files.minecraftforge.net/maven/net/minecraftforge/forge/").append(forgeBuild.mcversion).append('-').append(forgeBuild.version);
                if (forgeBuild.branch != null) urlString.append('-').append(forgeBuild.branch);
                urlString.append('/').append(forgeJson.artifact).append('-').append(forgeBuild.mcversion).append('-').append(forgeBuild.version);
                if (forgeBuild.branch != null) urlString.append('-').append(forgeBuild.branch);
                urlString.append('-').append(file.type).append('.').append(file.extention);
                URL url = new URL(urlString.toString());
                File installer = new File(output, FilenameUtils.getName(url.getFile()));
                FileUtils.copyURLToFile(url, installer);
                return installer;
            }
        }

        logger.println("======================================================================");
        logger.println("This forge version has no installer. You will have to do it manually.");
        logger.println("Forge version: " + forgeVersion);
        logger.println("======================================================================");
        return null;
    }

    private void downloadForgeJson() throws IOException
    {
        InputStream input = new URL("http://files.minecraftforge.net/maven/net/minecraftforge/forge/json").openStream();
        try
        {
            forgeJson = GSON.fromJson(IOUtils.toString(input), ForgeJson.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }
        if (forgeJson == null)
        {
            logger.println("======================================================================");
            logger.println("Something screwed up the forge installation. You will have to do it manually.");
            logger.println("Forge version: " + forgeVersion);
            logger.println("======================================================================");
        }
    }

    private String getForgeVersion()
    {
        String out = null;
        for (Modloader modloader : manifest.minecraft.modLoaders)
        {
            if (modloader.id.startsWith("forge-"))
            {
                if (out != null)
                {
                    throw new RuntimeException("Multiple forge versions. Invalid pack.");
                }
                out = modloader.id.substring(modloader.id.indexOf('-') + 1);
            }
            else
            {
                logger.println("======================================================================");
                logger.println("WARNING: YOU NEED TO MANUALLY INSTALL THIS MODLOADER: " + modloader.id);
                logger.println("======================================================================");
            }
        }
        return out;
    }

    private void startModDownloadThreads()
    {
        logger.println("Starting " + threads + " downloader threads.");
        threadObjects = new Thread[threads];
        for (int i = 0; i < threads; i++)
        {
            threadObjects[i] = new Thread(new ModDownloader(), "ModDownloader-" + i);
            threadObjects[i].start();
        }
    }

    private void waitTillDone()
    {
        if (installerProcess != null)
        {
            try
            {
                installerProcess.waitFor();
                if (installerFile.exists()) installerFile.delete();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        boolean done;
        do
        {
            done = true;
            for (Thread thread : threadObjects)
            {
                if (thread.isAlive())
                {
                    done = false;
                    break;
                }
            }
            smallDelay();
        }
        while (!done);
    }

    private void unpackOrMoveJson() throws ZipException, IOException
    {
        String inputExtension = FilenameUtils.getExtension(input.getName());
        switch (inputExtension.toLowerCase())
        {
            case "zip":
                ZipFile zipFile = new ZipFile(input);
                FileHeader manifestHeader = zipFile.getFileHeader("manifest.json");
                if (manifestHeader == null) throw new IOException("There is no manifest in the zip. Corrupt zip?");
                zipFile.extractFile(manifestHeader, output.getPath());
                manifest = GSON.fromJson(FileUtils.readFileToString(manifestFile), Manifest.class);
                zipFile.setRunInThread(true);
                ProgressMonitor pm = zipFile.getProgressMonitor();
                zipFile.extractAll(output.getPath());
                long lastUpdate = 0L;
                while (pm.getState() == ProgressMonitor.STATE_BUSY)
                {
                    if (System.currentTimeMillis() - lastUpdate > 5000)
                    {
                        logger.println("Unzipping... " + pm.getPercentDone() + "%");
                        lastUpdate = System.currentTimeMillis();
                    }
                    smallDelay();
                }
                File overrides = new File(output, manifest.overrides);
                FileUtils.copyDirectory(overrides, output);
                FileUtils.deleteDirectory(overrides);
                break;
            case "json":
                if (!inputEqualsOutput) FileUtils.copyFile(input, manifestFile);
                manifest = GSON.fromJson(FileUtils.readFileToString(manifestFile), Manifest.class);
                break;
            default:
                throw new IOException("Input file needs to be a .json or .zip. " + input.toString() + " is neither.");
        }
    }

    private void inputCheck() throws IOException
    {
        if (!input.exists()) throw new FileNotFoundException(input.toString());

        if (!output.exists()) output.mkdirs();
        if (output.isFile()) throw new IOException("Output is not a folder: " + output);

        modsFolder = new File(output, "mods");
        if (modsFolder.exists())
        {
            if (modsFolder.isFile()) throw new IOException("Modfolder is a file?");
            if (modsFolder.list().length != 0 && !ignoreExistingMods) throw new IOException("Your mods folder has files in it, clear them out first!");
        }
        else modsFolder.mkdir();
    }

    private void smallDelay()
    {
        try
        {
            synchronized (this)
            {
                this.wait(100);
            }
        }
        catch (InterruptedException ignored)
        {

        }
    }

    private class ModDownloader implements Runnable
    {
        @Override
        public void run()
        {
            for (int j = currentFile.getAndIncrement(); j < manifest.files.size(); j = currentFile.getAndIncrement())
            {
                CurseFile curseFile = manifest.files.get(j);

                try
                {
                    logger.println("Getting mod #" + (j + 1) + " of " + manifest.files.size());

                    HttpURLConnection con = (HttpURLConnection) (new URL("http://minecraft.curseforge.com/mc-mods/" + curseFile.projectID).openConnection());
                    con.setInstanceFollowRedirects(true);
                    con.connect();
                    con.getResponseCode();
                    con.disconnect();
                    String projectURL = con.getURL().toExternalForm();
                    int i = projectURL.indexOf('?');
                    if (i != -1) projectURL = projectURL.substring(0, i);

                    curseFile.projectName = FilenameUtils.getName(projectURL);
                    curseFile.projectName = curseFile.projectName.substring(curseFile.projectName.indexOf('-') + 1);

                    con = (HttpURLConnection) (new URL(projectURL + "/files/" + curseFile.fileID + "/download").openConnection());
                    con.setInstanceFollowRedirects(true);
                    con.connect();
                    con.getResponseCode();
                    con.disconnect();
                    String filename = FilenameUtils.getName(con.getURL().getFile());
                    i = filename.indexOf('?');
                    if (i != -1) filename = filename.substring(0, i);

                    filename = URLDecoder.decode(filename, "UTF-8");

                    FileUtils.copyURLToFile(con.getURL(), new File(modsFolder, filename));

                    curseFile.fileName = filename;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
