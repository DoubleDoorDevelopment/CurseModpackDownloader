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

package net.dries007.cmd.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dries007
 */
public class DeleteOnExit
{
    private static final List<File> fileList = new ArrayList<>();
    private static Thread thread;

    public static void add(File file)
    {
        fileList.add(file);
        if (thread == null) init();
    }

    private static void init()
    {
        thread = new Thread(() ->
        {
            for (File file : fileList)
            {
                try
                {
                    FileUtils.deleteDirectory(file);
                }
                catch (IOException ignore)
                {
                    // meh
                }
            }
        }, "DeleteOnExitThread");
        Runtime.getRuntime().addShutdownHook(thread);
    }
}
