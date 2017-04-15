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

import java.util.List;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class ForgeBuild
{
    public String branch;
    public int build;
    public String mcversion;
    public int modified;
    public String version;
    public List<ForgeFile> files;

    @Override
    public String toString()
    {
        return "ForgeBuild{" +
                "branch='" + branch + '\'' +
                ", build=" + build +
                ", mcversion='" + mcversion + '\'' +
                ", modified=" + modified +
                ", version='" + version + '\'' +
                ", files=" + files +
                '}';
    }
}
