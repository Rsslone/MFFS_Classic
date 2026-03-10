<p align="center">
    <img src="https://raw.githubusercontent.com/BuiltBrokenModding/MFFS_Classic/9bd18609f2dd87c20bd2fefba639254a425afbe4/src/main/resources/logo.png" alt="Logo" width="50%">
</p>

## About

**Modular Force Field System** (or MFFS) is a mod that adds force fields, high tech machinery and defensive measures to Minecraft.
Ever tired of nuclear explosions blowing up your house or want to keep people out of your secret bases?
**May the force (fields) be with you!**

This is a backport of MFFS v3.0+ which itself is a rewrite inspired by ThunderDark's MFFS mod. The foundation and reference for this version was the 1.21/1.20 Forge/NeoForge versions.

This backport heavily utilized AI tools, but was thoroughly tested, and in some cases where the reference code used features that do not exist in minecraft 1.12.2, additional effort and features were added.

## Usage

MFFS uses Fortron as its main power source for force fields. It contains a series of machines focused on converting
energy to Forton for the use
in generating force fields. The primary machine is the Force Field Projector which is powered by Fortron Capacitors
(storage) and Coercion Derivers (generators).

The projector can be modified with various cards and upgrades that improve its stats. These upgrades include several
field shapes, size scaling,
position offset, and utility modules. For example, the upgrade to shock attacks, kill monsters, remove blocks, and
protect tiles.

## Differences
- Lighting -- The upstream mod uses the updated lighting system to defer updates, on 1.12.2 this system does not exist, so we had to reduce the load by more smartly placing lights

### Contributing

Contributions are welcome, feel free to submit a pull request.

### Credits

Block highlighting render code - [DarkKronicle's BetterBlockOutline renderer](https://github.com/DarkKronicle/BetterBlockOutline)

#### Past Developers

**Project Lead Developer** - Calclavia  
**Code** - Thutmose, Briman  
**Art** - Comply_cat_Ed, Sweet Walrus, mousecop, mr_hazard  
**Original mod by** Thunderdark  

## TemplateDevEnv

Utilizes Cleanroom's template workspace for modding Minecraft 1.12.2. Licensed under MIT, it is made for public use.

Runs on **Java 25**, **Gradle 9.2.1** + **[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) 2.0.2** + **Forge 14.23.5.2847**.

With **coremod and mixin support** that is easy to configure.
