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

package net.doubledoordev.cmd.forge;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * @author Dries007
 */
public class ForgeFileJson implements JsonSerializer<ForgeFile>, JsonDeserializer<ForgeFile>
{
    @Override
    public ForgeFile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        try
        {
            JsonArray array = json.getAsJsonArray();
            ForgeFile file = new ForgeFile();
            file.extention = array.get(0).getAsString();
            file.type = array.get(1).getAsString();
            file.md5 = array.get(2).getAsString();
            return file;
        }
        catch (Exception e)
        {
            throw new JsonParseException("Forge file format incorrect.", e);
        }
    }

    @Override
    public JsonElement serialize(ForgeFile src, Type typeOfSrc, JsonSerializationContext context)
    {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(src.extention));
        array.add(new JsonPrimitive(src.type));
        array.add(new JsonPrimitive(src.md5));
        return array;
    }
}
