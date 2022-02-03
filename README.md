
# ModHulk by Benco11

**ModHulk** is an **automatic CurseForge mod downloader** to download all the mods from a text file (like a custom datapack mods list).
With ModHulk, you **don't have to download manually** all the mods. **ModHulk saves you a lot of time!**



## Documentation

### Functioning

The program can use Selenium and **only accepts CurseForge mods listed on a text file**.

The functioning of ModHulk is:

 - At the start, ModHulk **downloads the last Firefox web driver** if **selenium option is set to true**.

 - Next the program **reads the file** and for each line checks if the **string is corresponding to
a URL**, if yes, **add the link to the mods list**.

 - After that, for each mod, it uses the Java library **[CurseAPI](https://github.com/TheRandomLabs/CurseAPI)**
to **find the project using the mod name** and get all mod versions. Then the program only **retains the last version with the same Minecraft version** and **downloads it in the requested folder**.

 - It may occur that the library **CurseAPI can't provide the project with the name**. If **selenium option is set to true**, ModHulk will then
**try getting the project ID** by sending **a request to the CurseForge website using selenium**.
If it succeeds, the program will get the project using the ID and **download the appropriate version**.

 - At the exit of the program, the **web driver file stocked in temps file is deleted**.

*N.B. ModHulk only supports Firefox web driver for the moment*

### Installation and Compilation

You can find the compiled jar in **releases**, or compile the code with Gradle :

```bash
gradlew shadowJar
``` 

Then the compiled jar will be in ```build/libs```.


### Usage

You just need to launch the jar with ```java``` command, then the program will ask for **five inputs**:
the **source text file** path; the **destination folder** path; the **Minecraft version**;
**if you want to retry request with web driver selenium** when the program can't find a mod in CurseForge API **(highly recommended)**;
**the mod platform (Forge or Cauldron)**.

You can also provide these elements in the command's program arguments while keeping **the same order** and 
**separating elements** with an ```@```.


### Examples

_Example 1:_

Download in **_C:\mods\modpack uranus\\_** all **_forge_** mods of **_D:\mods.txt_** in **_1.16.5_** using **_selenium_**
```
Please enter the path of the mods' list file :
D:\mods.txt
Please enter the destination folder's path :
C:\mods\modpack uranus\
Please enter the minecraft target version :
1.16.5
Please enter if you want to use selenium firefox (your browser) when the program can't get mod informations :
true
Please enter Forge or Fabric :
Forge

Processing...
```

or

```bash
java -jar ModHulk.jar D:\mods.txt@C:\mods\modpack uranus\@1.16.5@true@Forge
```
```
Processing...
```

_Example 2 :_

Download in **_/home/download/portable mods_** all **_fabric_** mods of **/home/list** in **_1.18.1_** without using **_selenium_**
```
Please enter the path of the mods' list file :
/home/list
Please enter the destination folder's path :
/home/download/portable mods
Please enter the minecraft target version :
1.18.1
Please enter if you want to use selenium firefox (your browser) when the program can't get mod informations :
false
Please enter Forge or Fabric :
Fabric

Processing...
```

or

```bash
java -jar ModHulk.jar /home/list@/home/download/portable mods@1.18.1@false@Fabric
```
```
Processing...
```