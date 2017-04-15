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

package net.dries007.cmd.util.manifest;

import java.util.List;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Minecraft
{
    public String version;
    public List<Modloader> modLoaders;

    @Override
    public String toString()
    {
        return "Minecraft{" +
                "version='" + version + '\'' +
                ", modLoaders=" + modLoaders +
                '}';
    }
}
