<h1 align="center">One in the Chamber</h1>

<div align="center">

[![](https://jitpack.io/v/Despical/OITC.svg)](https://jitpack.io/#Despical/OITC)
[![](https://img.shields.io/badge/JavaDocs-latest-lime.svg)](https://javadoc.jitpack.io/com/github/Despical/OITC/latest/javadoc/index.html)
[![Discord](https://img.shields.io/discord/719922452259668000.svg?color=lime&label=Discord)](https://discord.gg/rVkaGmyszE)
[![Support](https://img.shields.io/badge/Patreon-Support-lime.svg?logo=Patreon)](https://www.patreon.com/despical)

One in the Chamber is an old Minecraft minigame. Each player is equipped with a sword, bow and arrow.
The arrows do one hit one kill damage and if player missed the shot they can't receive a new arrow until they die
or killing other players. When a player reaches 25 points the game ends and winner is the player who reached first.

</div>

## Documentation
- [Wiki](https://github.com/Despical/OITC/wiki)
- [JavaDocs](https://javadoc.jitpack.io/com/github/Despical/OITC/latest/javadoc/index.html)
- [Discord Community](https://www.discord.gg/rVkaGmyszE)

## Donations
- [Patreon](https://www.patreon.com/despical)
- [Buy me a Coffe](https://www.buymeacoffee.com/despical)

## Using One in the Chamber API
The project isn't in the Central Repository yet, so specifying a repository is needed.<br>

<details>
<summary>Maven dependency</summary>

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
    <version>2.4.2</version>
    <scope>compile</scope>
</dependency>
```

</details>

<details>
<summary>Gradle dependency</summary>

```
repositories {
    maven { url 'https://jitpack.io' }
}
```
```
dependencies {
    compileOnly group: "com.github.Despical", name: "OITC", version: "2.4.2";
}
```
</details>

## Contributing

I accept Pull Requests via GitHub. There are some guidelines which will make applying PRs easier for me:
+ Ensure you didn't use spaces! Please use tabs for indentation.
+ Respect the code style.
+ Do not increase the version numbers in any examples files and the README.md to the new version that this Pull Request would represent.
+ Create minimal diffs - disable on save actions like reformat source code or organize imports. If you feel the source code should be reformatted create a separate PR for this change.

You can learn more about contributing via GitHub in [contribution guidelines](../CONTRIBUTING.md).

## Translations
We are supporting multiple languages such as English, Turkish and German for now.<br>
If you want to help us with translating take a look at our [language repository](https://github.com/Despical/LocaleStorage).

## License
This code is under [GPL-3.0 License](http://www.gnu.org/licenses/gpl-3.0.html)

See the [LICENSE](https://github.com/Despical/OITC/blob/master/LICENSE) file for required notices and attributions.

## Building from source
To build this project from source code, run the following from Git Bash:
```
git clone https://www.github.com/Despical/OITC.git && cd OITC
mvn clean package -Dmaven.javadoc.skip=true
```

> **Note** Don't forget to install Maven before building.
