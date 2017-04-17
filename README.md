# CurseModpackDownloader

A Curse Modpack downloader.

Download pre-build releases from [here](https://jenkins.dries007.net/job/CurseModpackDownloader/).

## Usage

For up to date usage/help information, run the jar with `--help`.

### GUI

For a GUI: Do not use any arguments. (Disabled on headless systems.)

### Help / Usage information

Options indicated by `*` are required. You must also use either `client` or `server`.

<small>Last updated for v1.0.2 .</small>

```
Usage: CurseModpackDownloader [options] [command] [command options]
  Options:
    -d, --delete
      If output is folder, delete the contents of any root folder specified in the pack.
      Default: false
    --help, -h, -?
      Display this text.
  * -i, --input
      Input file/URL (CurseForge modpack zip)
    --keep-tmp
      Keep the tmp folder after the program has exited.
      Default: false
    -m, --magic
      Use magic to try and download all mods. Uses external API hosted by Dries007.
      Default: false
    --name
      Override the name. (Only useful when MultiMC or zipping without --root-zip)
    --no-forge
      Skip forge download (and install on servers).
      Default: false
  * -o, --output
      Output folder or zip file.
    -w, --override
      If output is folder, don't check to see if folders specified in the pack are empty. If output is a zip, override if it exists.
      Default: false
    -q, --quiet
      Quiet. Don't output status information, make no log.
      Default: false
    --root-zip
      Put the pack in the root of the zip. (Only useful when output is zip)
      Default: false
    -j, --threads
      Number of parallel downloaders to use.
      Default: <number of CPU cores>
    --tmp
      Use a specific tmp folder. Must exist. Defaults to 'java.io.tmpdir'
      Default: <system tmp folder>
  Commands:
    client      Create a client side pack.
      Usage: client [options]
        Options:
          --multimc
            Create a MultiMC instance.
            Default: false

    server      Create a server side pack.
      Usage: server [options]
        Options:
          --eula
            Include a EULA file. Only use if _you_ are the end user, and you agree with the Mojang EULA!
            Default: false
```

### Examples

This command will 'update' an existing server in the `TestModpackServer` server:
```
java -jar CurseModpackDownloader.jar --delete -i TestModpack.zip -o TestModpackServer server --eula
```
This command will create a MultiMC instance of the `TestModpack.zip` curse download in the instance folder (Linux):
```
java -jar CurseModpackDownloader.jar --delete -i TestModpack.zip -o ~/.local/share/multimc5/instances client --multimc
```

## TODO

- Download files that are archived.
    - Requires ugly API & CF login info
- Make a nice GUI
- Make modpack report
- Log to file

## Legal

[Copyright 2017 Dries007](src/main/java/net/dries007/cmd/LICENSE-HEADER)

Licenced under [European Union Public Licence V. 1.1](LICENSE).

- This program is not affiliated with, or endorsed by Curse, Twitch or Amazon.
- This program is not affiliated with, or endorsed by Minecraft, Mojang or Microsoft.
- All product names, logos, and brands are property of their respective owners. All company, product and service names used in this website are for identification purposes only. Use of these names, logos, and brands does not imply endorsement.
