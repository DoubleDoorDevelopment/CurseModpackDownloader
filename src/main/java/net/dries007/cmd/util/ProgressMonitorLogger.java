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

import net.dries007.cmd.Helper;
import net.lingala.zip4j.progress.ProgressMonitor;

import java.io.PrintStream;

/**
 * @author Dries007
 */
public class ProgressMonitorLogger implements Runnable
{
    private final PrintStream logger;
    private final ProgressMonitor pm;
    private final String zipping;

    public ProgressMonitorLogger(PrintStream logger, ProgressMonitor pm, String zipping)
    {
        this.logger = logger;
        this.pm = pm;
        this.zipping = zipping;
    }

    @Override
    public void run()
    {
        logger.println(zipping + " of input started...");

        long lastUpdate = 0L;
        while (pm.getState() == ProgressMonitor.STATE_BUSY)
        {
            if (System.currentTimeMillis() - lastUpdate > 5000)
            {
                logger.println(zipping + "... " + pm.getPercentDone() + "%");
                lastUpdate = System.currentTimeMillis();
            }
            Helper.sleep(100);
        }

        logger.println(zipping + " of input done.");
    }
}
