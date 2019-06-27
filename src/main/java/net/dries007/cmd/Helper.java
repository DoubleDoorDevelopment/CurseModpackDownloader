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

import com.google.gson.*;
import net.dries007.cmd.util.forge.ForgeFile;
import net.dries007.cmd.util.forge.ForgeFileJson;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Helper
{
    public static final String NAME = "CurseModpackDownloader";
    public static final String URL_FORGE_MAVEN = "http://files.minecraftforge.net/maven/net/minecraftforge/forge/";
    public static final String URL_FORGE_JSON = URL_FORGE_MAVEN + "json";
    public static final String URL_MAGIC = "https://cursemeta.dries007.net/";
    public static final Pattern PATTERN_INPUT_CURSE_ID = Pattern.compile("^(\\d+)(?::(\\d+|-1|release|beta))?$");

    public static final Arguments ARGUMENTS = new Arguments();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeHierarchyAdapter(ForgeFile.class, new ForgeFileJson())
            .create();

    public static final int MAX_REDIRECTS = 10;
    public static final JsonParser JSON_PARSER = new JsonParser();

    private Helper() {}

    public static String getFileURL(String projectName, int fileId) throws IOException
    {
        String pre = "https://www.curseforge.com/minecraft/mc-mods/" + projectName + "/download/" + fileId + "/file";
        String post = getFinalURL(pre);
        return pre.equals(post) ? null : post;
    }

    public static String getProjectName(int projectId) throws IOException
    {
        String out = getFinalURL("https://minecraft.curseforge.com/projects/" + projectId);
        return out.substring(out.lastIndexOf('/') + 1);
    }

    public static String getFinalURL(String url) throws IOException
    {
        for (int i = 0; i < MAX_REDIRECTS; i++)
        {
            url = url.replace(" ", "%20");
            HttpURLConnection con = null;
            try
            {
                URL objURL = new URL(url);
                con = (HttpURLConnection) objURL.openConnection();
                con.setInstanceFollowRedirects(false);
                con.connect();
                int code = con.getResponseCode();
                String newUrl = null;
                if (code == 200) {
                    return url;
                }
                if (code >= 300 && code < 400) {
                    newUrl = con.getHeaderField("Location");
                }
                if (newUrl == null)
                {
                    url = url.replace("?cookieTest=1", "");
                } else if (newUrl.charAt(0) == '/') {
                    url = objURL.getProtocol()+"://"+objURL.getAuthority()+newUrl;
                } else {
                    url = newUrl;
                }
            }
            catch (IOException e)
            {
                return url.replace("?cookieTest=1", "");
            }
            finally
            {
                if (con != null) con.disconnect();
            }
        }
        throw new IOException("Redirect limit (" + MAX_REDIRECTS + ") exceeded on url: " + url);
    }

    public static void sleep(int timeout)
    {
        try
        {
            Thread.sleep(timeout);
        }
        catch (InterruptedException ignored)
        {

        }
    }

    public static <T> T parseJson(String url, Class<T> aClass) throws IOException, JsonParseException
    {
        InputStreamReader isr = null;
        try
        {
            isr = new InputStreamReader(new URL(getFinalURL(url)).openStream());
            return GSON.fromJson(isr, aClass);
        }
        finally
        {
            IOUtils.closeQuietly(isr);
        }
    }

    public static JsonElement parseJson(String url) throws IOException, JsonParseException
    {
        InputStreamReader isr = null;
        URL finalURL = new URL(getFinalURL(url));
        try
        {
            isr = new InputStreamReader(finalURL.openStream());
            return JSON_PARSER.parse(isr);
        }
        catch (Exception e)
        {
            System.err.println(finalURL);
            throw e;
        }
        finally
        {
            IOUtils.closeQuietly(isr);
        }
    }

    public static String parseIdBasedInput(String input) throws IOException
    {
        Matcher matcher = PATTERN_INPUT_CURSE_ID.matcher(input);

        if (!matcher.matches()) return input;

        int fileID = -1; // -1 or fileID
        String type = null; // null, release or beta
        int projectID = Integer.parseInt(matcher.group(1)); // required to be there, and it's always valid.

        if (matcher.groupCount() > 1)
        {
            try
            {
                fileID = Integer.parseInt(matcher.group(2));
            }
            catch (NumberFormatException e)
            {
                type = matcher.group(2);
            }
        }

        // now, if fileID wasn't a number (or it was -1)
        if (fileID == -1)
        {
            JsonObject root = Helper.parseJson(Helper.URL_MAGIC + projectID + ".json").getAsJsonObject();
            if (!root.get("PackageType").getAsString().equalsIgnoreCase("modpack"))
            {
                throw new IllegalArgumentException("ProjectID " + projectID + " isn't a modpack.");
            }
            if (type == null) // fileID was int, but must be -1, so pick default
            {
                fileID = root.get("DefaultFileId").getAsInt();
            }
            else // fileID wasn't an int, look in array to find matching latest version
            {
                for (JsonElement fileE : root.getAsJsonArray("GameVersionLatestFiles"))
                {
                    JsonObject file = fileE.getAsJsonObject();
                    if (file.get("FileType").getAsString().equalsIgnoreCase(type))
                    {
                        fileID = file.get("ProjectFileID").getAsInt();
                        break; // Gotcha
                    }
                }
                if (fileID == -1)
                {
                    throw new IllegalArgumentException("Could not pick a file based on latest files with type " + type);
                }
            }
        }

        // fetch actual URL
        JsonObject root = Helper.parseJson(Helper.URL_MAGIC + projectID + "/" + fileID + ".json").getAsJsonObject();
        input = root.get("DownloadURL").getAsString();

        return input;
    }
}
