# One in the Chamber
[![](https://jitpack.io/v/Despical/OITC.svg)](https://jitpack.io/#Despical/OITC)
[![](https://img.shields.io/badge/JavaDocs-latest-lime.svg)](https://javadoc.jitpack.io/com/github/Despical/OITC/latest/javadoc/index.html)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/Despical/OITC/OITC%20Build)

One in the Chamber is an old Minecraft mini game. Each player is equipped with a sword, bow and arrow.
The arrows do one hit one kill damage and if player missed the shot they can't receive a new arrow until die
or killing other players. When a player reaches 25 points the game ends and winner is the player who reached first.

## Documentation
More information can be found on the [wiki page](https://github.com/Despical/OITC/wiki).
The [Javadoc](https://javadoc.jitpack.io/com/github/Despical/OITC/latest/javadoc/index.html) can be browsed.
Also you can join our small [Discord community](https://www.discord.gg/rVkaGmyszE) to get support and news early.

## Donations
You like the OITC? Then [donate](https://www.patreon.com/despical) back me to support the development.

## Using One in the Chamber API
The project isn't in the Central Repository yet, so specifying a repository is needed.<br>
To add this project as a dependency to your project, add the following to your pom.xml:

### Maven dependency

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.Despical</groupId>
    <artifactId>OITC</artifactId>
    <version>2.0.2</version>
    <scope>compile</scope>
</dependency>
```

### Gradle dependency
```
repositories {
    maven { url 'https://jitpack.io' }
}
```
```
dependencies {
    compileOnly group: "com.github.Despical", name: "OITC", version: "2.0.2";
}
```

## Contributing

I accept Pull Requests via GitHub. There are some guidelines which will make applying PRs easier for me:
+ Ensure you didn't use spaces! Please use tabs for indentation.
+ Respect the code style.
+ Do not increase the version numbers in any examples files and the README.md to the new version that this Pull Request would represent.
+ Create minimal diffs - disable on save actions like reformat source code or organize imports. If you feel the source code should be reformatted create a separate PR for this change.

You can learn more about contributing via GitHub in [contribution guidelines](CONTRIBUTING.md).

## Translations
We are supporting multiple languages such as English, Turkish and German for now.<br>
If you want to help us with translating take a look at our [language repository](https://github.com/Despical/LocaleStorage).

## License
This code is under [GPL-3.0 License](http://www.gnu.org/licenses/gpl-3.0.html)

See the [LICENSE](https://github.com/Despical/OITC/blob/master/LICENSE) file for required notices and attributions.

## Building from source
If you want to build this project from source code, run the following from Git Bash:
```
git clone https://www.github.com/Despical/OITC.git && cd OITC
mvn clean package -Dmaven.javadoc.skip=true (this option does not allow to generate docs and make compiling faster)
```
Also don't forget to install Maven before building.