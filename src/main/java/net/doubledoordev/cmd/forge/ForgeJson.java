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

import java.util.List;
import java.util.Map;

/**
 * @author Dries007
 */
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
}
