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

/**
 * @author Dries007
 */
public class ModpackException extends Exception
{
    public ModpackException()
    {
    }

    public ModpackException(String message)
    {
        super(message);
    }

    public ModpackException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ModpackException(Throwable cause)
    {
        super(cause);
    }

    public ModpackException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static class ManifestInvalidException extends ModpackException
    {
        public ManifestInvalidException(String message)
        {
            super(message);
        }
    }
}
