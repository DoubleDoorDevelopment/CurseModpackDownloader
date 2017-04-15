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

import net.dries007.cmd.util.forge.ForgeBuild;

import java.util.List;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class Manifest
{
    public Minecraft minecraft;
    public String manifestType;
    public String manifestVersion;
    public String name;
    public String version;
    public String author;
    public List<CurseFile> files;
    public String overrides;

    public ForgeBuild forgeBuild;

    @Override
    public String toString()
    {
        return "Manifest{" +
                "minecraft=" + minecraft +
                ", manifestType='" + manifestType + '\'' +
                ", manifestVersion='" + manifestVersion + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", files=" + files +
                ", overrides='" + overrides + '\'' +
                ", forgeBuild='" + forgeBuild + '\'' +
                '}';
    }
}
