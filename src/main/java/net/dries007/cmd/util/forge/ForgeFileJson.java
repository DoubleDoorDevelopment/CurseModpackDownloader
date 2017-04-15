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

package net.dries007.cmd.util.forge;

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
