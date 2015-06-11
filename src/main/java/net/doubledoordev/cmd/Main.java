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

import com.beust.jcommander.JCommander;
import net.lingala.zip4j.exception.ZipException;

import java.io.IOException;

/**
 * @author Dries007
 */
public class Main
{
    private Main()
    {

    }

    public static void main(String[] args) throws IOException, ZipException
    {
        CurseModpackDownloader curseModpackDownloader = new CurseModpackDownloader();
        JCommander commander = new JCommander(curseModpackDownloader, args);

        if (curseModpackDownloader.help)
        {
            commander.usage();
//            System.out.println();
//            System.out.println("List of parameters:     [name, ...]: description    [default value]");
//            System.out.println("-------------------");
//            for (ParameterDescription description : commander.getParameters())
//            {
//                System.out.println(description.getNames() + ": " + description.getDescription() + "    [" + description.getDefault() + "]");
//            }
//            System.out.println();
            System.out.println("License:");
            System.out.println("--------");
            System.out.println("CurseModpackDownloader");
            System.out.println("Copyright (C) 2015  Dries007 & Double Door Development");
            System.out.println();
            System.out.println("This program is free software: you can redistribute it and/or modify");
            System.out.println("it under the terms of the GNU General Public License as published by");
            System.out.println("the Free Software Foundation, either version 3 of the License, or");
            System.out.println("(at your option) any later version.");
            System.out.println();
            System.out.println("This program is distributed in the hope that it will be useful,");
            System.out.println("but WITHOUT ANY WARRANTY; without even the implied warranty of");
            System.out.println("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
            System.out.println("GNU General Public License for more details.");
            System.out.println();
            System.out.println("You should have received a copy of the GNU General Public License");
            System.out.println("along with this program.  If not, see <http://www.gnu.org/licenses/>.");
            return;
        }

        curseModpackDownloader.run();
    }
}
