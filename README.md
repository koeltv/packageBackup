# Utility to backup windows packages

This program is used to back up your packages using the [winget](https://learn.microsoft.com/en-us/windows/package-manager/winget/) tool from Windows  
It make the process a bit easier, store the name of the packages that weren't found through winget and is able to find some more of them.  
It is used via command line like follows:

1. Install the utility via one of the installation options
2. Run it or extract it
3. Use it as-is by opening a terminal or add it to your PATH environment variable
4. Run it like so:

To save your packages:
```bash
./packageBackup.exe -s outputFilePath
./packageBackup.exe --save outputFilePath
```
All the packages that couldn't be found via `winget` will be listed in `outputFileName_unavailable`.
You will need to manually reinstall them. That is the case for most games.

You can also blacklist some packages by creating a `blacklist.txt` file containing a name filter on each line, like so:
```
first filter
second filter
...
last filter
```
A default one is included (excluding default Windows and Microsoft packages).

To load your packages from a file:
```bash
./packageBackup.exe -l inputFileName
./packageBackup.exe --load inputFileName
```

Icon from [Smashicons - Flaticon](https://www.flaticon.com/free-icons/backup)