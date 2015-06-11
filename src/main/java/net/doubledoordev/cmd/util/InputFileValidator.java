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

package net.doubledoordev.cmd.util;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Dries007
 */
public class InputFileValidator implements IValueValidator<File>
{
    @Override
    public void validate(String name, File value) throws ParameterException
    {
        //if (!value.exists()) throw new ParameterException(new FileNotFoundException(value.toString()));
        //if (!value.isFile()) throw new ParameterException(value.toString() + " is not a file.");
        String extention = FilenameUtils.getExtension(value.getName());
        if (extention.equalsIgnoreCase("json") || extention.equalsIgnoreCase("zip")) return;
        throw new ParameterException(value.toString() + " is not a .json or .zip file.");
    }
}
