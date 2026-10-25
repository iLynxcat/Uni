# Uni: A unified custom Minecraft server

A unified source architecture for an end-to-end customizable Minecraft server.
NMS source can be patched, Bukkit/Paper source can be patched, and custom plugins
and libraries can all be imported and used.

## Structure

**UniSrc** is a custom server based on Paper that contains deep functionality tweaks to make custom gameplay changes possible. It consists of two parts: _unisrc-api_, for plugins to link against, and _unisrc-server_, the server software implementation itself.

**plugins**, including _UniCore_, that provide extra server functionality atop _unisrc-api_. These are Paper or Bukkit plugins in the truest sense.

**libraries** (in **libs**) are shared code modules that can be consumed by and shared between plugins.

## Important Commands

```shell
./gradlew applyAllPatches     # prepare the custom server from patches
./gradlew build               # build all plugins, libs, and UniSrc bundler
./gradlew createPaperclipJar  # build a UniSrc launcher (copyright-safe to distribute)
./gradlew createBundlerJar    # build a UniSrc bundler (standalone runnable jar, contains Mojang code!)
./gradlew runServer           # run local server
```

## Making Changes

When working on any plugins or libraries, the setup's pretty standard - make your changes, commit, rebuild, run server as you wish.

When modifying the custom server (UniSrc) it's a bit more involved.

To change Minecraft code, edit within _unisrc-server/src/minecraft/java_. Once you've made your change, create a commit within that repository, then run the Gradle task _rebuildMinecraftPatches_ from the project root. This will create a patch file you can then commit to this main repository.

To change Paper code, edit within _paper-server_ or _paper-api_, and follow the commit-to-patch workflow described above except replace _rebuildMinecraftPatches_ with _rebuildPaperPatches_. Check `./gradlew tasks` for more task details; there are plenty that come with Paper's dev tools (Paperclip)!
