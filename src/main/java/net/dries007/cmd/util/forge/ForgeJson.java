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
import java.util.Map;

/**
 * @author Dries007
 */
@SuppressWarnings("WeakerAccess")
public class ForgeJson
{
    public String adfocus;
    public String artifact;
    public String homepage;
    public String name;
    public String webpath;
    public Map<String, List<Integer>> branches;
    public Map<String, List<Integer>> mcversion;
    public Map<String, ForgeBuild> number;
    public Map<String, Integer> promos;

    @Override
    public String toString()
    {
        return "ForgeJson{" +
                "adfocus='" + adfocus + '\'' +
                ", artifact='" + artifact + '\'' +
                ", homepage='" + homepage + '\'' +
                ", name='" + name + '\'' +
                ", webpath='" + webpath + '\'' +
                ", branches=" + branches +
                ", mcversion=" + mcversion +
                ", number=" + number +
                ", promos=" + promos +
                '}';
    }
}
