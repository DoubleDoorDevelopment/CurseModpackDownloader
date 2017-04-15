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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dries007.cmd.util.forge.ForgeFile;
import net.dries007.cmd.util.forge.ForgeFileJson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Helper
{
    public static final String NAME = "CurseModpackDownloader";
    public static final Arguments ARGUMENTS = new Arguments();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeHierarchyAdapter(ForgeFile.class, new ForgeFileJson())
            .create();

    public static final int MAX_REDIRECTS = 10;

    private Helper() {}

    public static String getFileURL(String projectName, int fileId) throws IOException
    {
        return getFinalURL("https://minecraft.curseforge.com/projects/" + projectName + "/files/" + fileId + "/download");
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
            HttpURLConnection con = null;
            try
            {
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setInstanceFollowRedirects(false);
                con.connect();
                if (con.getHeaderField("Location") == null)
                {
                    return url.replace("?cookieTest=1", "");
                }
                url = con.getHeaderField("Location");
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
}
