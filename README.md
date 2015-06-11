CurseModpackDownloader
======================

This program can take a manifest.json or curse zip download and turn in into a full modpack.

If you wish to make a server, it automatically runs the forge installer, and can make the eula.txt file for you.

Otherwise it will just download the forge installer jar so all you have to do it run that and copy the mods/configs/scripts over.

Only tested and verified to work with 1.7.10 modpacks, but it should work fine with other MC versions.

No modloaders besides Forge (version > 7.8.0.684) are supported. Modpacks without Forge need some manual work.

This program is made in such a way that it should be easy to integrate into other (java) programs.

Another feature is the CurseModpackDownloader.txt file, that contains information about the pack in a human readable format.


Usage
-----

    java -jar CurseModpackDownloader-<version>.jar [options]

Options:

    -eula
        Write a eula file, if you are installing a server. This indicates your agreement.
        Default: false    
    -help, -?
        Display help.
        Default: false
    -ignoreExistingMods, -iem
        Ignore that the mods folder is not empty.
        Default: false
    -input, -i
        The curse zip or json file.
        Default: manifest.json
    -output, -o
        The output folder
        Default: . (current forlder)
    -server, -s
        Install a server.
        Default: false
    -threads, -t
        # of parallel download threads
        Default: 4

Example usage:

    java -jar CurseModpackDownloader-0.1.0.jar -i "Weasel UHS-1.3.13.zip" -o WUHS -server -eula -threads 10

Download
--------

[We use a Jenkins build bot, download the very latest build here.](http://www.doubledoordev.net/?p=projects)

[Builds are also available on out maven repository.](http://www.doubledoordev.net/maven/)

License
-------

If you want to integrate this program into another program, via the command line interface alone,
you have special permission to distribute an unmodified (downloaded from the official download location),
compiled jar along with your program without having to disclose the source of your program.

When in doubt about this statement, please contact us first.

CurseModpackDownloader

Copyright (C) 2015  Dries007 & Double Door Development

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.