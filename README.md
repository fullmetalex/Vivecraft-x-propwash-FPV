# Propwash VR — Vivecraft × Propwash FPV

Support de la réalité virtuelle pour [Propwash FPV](https://github.com/LardTom/propwash-fpv) avec
[Vivecraft](https://github.com/Vivecraft/VivecraftMod). Mod client pour **Minecraft 26.3**, **Fabric** et **NeoForge**.

*English summary at the bottom.*

## Ce que fait le mod

Sans ce mod, Vivecraft place lui‑même la caméra de chaque œil sur la tête du joueur : en VR, la vue FPV de
Propwash ne s'affiche jamais, on reste dans son corps pendant que la drone vole. Propwash VR corrige ça :

- **Vue FPV dans le casque**, dès que vous volez en FPV (lunettes portées) ou que vous regardez le canal vidéo d'un
  autre pilote. Deux modes :
  - **Lunettes** (par défaut) : comme de vraies lunettes FPV. L'image de la caméra de la drone est un écran plat,
    fixé devant les yeux, entouré d'un cadre noir. Tourner la tête ne bouge pas l'image, les deux yeux voient la même
    image (une caméra FPV est mono). Toute l'image de la caméra (souvent 120–150°) tient dans l'écran virtuel, dont la
    taille se règle (70° par défaut).
  - **Immersif** : les yeux sont à la place de la caméra de la drone et la tête regarde autour, en stéréo. Très
    intense, réservé aux pilotes qui ont le pied marin en VR. Une touche recentre la vue.
- **OSD et effets vidéo** : l'OSD Betaflight de Propwash (le HUD) est posé exactement sur l'image des lunettes ;
  la latence, le bruit analogique et les coupures numériques de Propwash restent actifs.
- **Les manettes VR comme radiocommande** : pendant le pilotage en VR, si aucune vraie radio ni manette de jeu n'est
  branchée, les sticks des manettes VR deviennent les sticks de la radio (mode 2 par défaut, modes 1 à 4 au choix).
  Pendant ce temps, Vivecraft ne fait plus tourner le joueur avec le stick droit, les gâchettes ne téléportent plus et
  les grips ne changent plus d'objet en main.
- **Vol à vue (sans lunettes)** : en VR, c'est votre tête qui suit la drone ; Propwash ne force plus la direction du
  regard.
- Le **miroir du bureau** (mode « première personne » de Vivecraft) montre l'image FPV : pratique pour streamer.
- Le corps du pilote reste visible (avec sa tête) quand la drone le filme.

Une vraie radio (EdgeTX/OpenTX en mode joystick USB) ou une manette de jeu continue de marcher comme d'habitude :
elle a la priorité sur les manettes VR.

## Commandes avec les manettes VR

Valables avec les liaisons par défaut de Vivecraft pour les manettes à joystick (Quest/Rift Touch, Index,
Windows Mixed Reality). Les manettes Vive (pavés tactiles) n'ont pas de stick analogique : utilisez une radio ou une
manette de jeu.

| Manette VR | Fonction radio |
|---|---|
| Stick gauche | gaz (moitié haute du stick) + lacet (mode 2) |
| Stick droit | tangage + roulis (mode 2) |
| Grip droit | armer / désarmer (bascule) |
| Grip gauche | mode Angle (bascule, actif au départ) |
| Gâchette droite | retournement après crash (maintenir) |
| Gâchette gauche | GPS Rescue (bascule) |
| Clic stick gauche | bipeur (bascule) |
| A (utiliser) | arrêter de piloter, comme le clic droit |

En mode gaucher de Vivecraft, les sticks sont inversés pour rester sur les mêmes sticks physiques.

## Raccourcis

Trois touches dans la catégorie « Propwash VR » des contrôles, non attribuées par défaut. Elles apparaissent aussi
dans le jeu d'actions « Mod » de Vivecraft, donc on peut les mettre sur un bouton de manette dans l'éditeur de
liaisons SteamVR :

- **Basculer vue lunettes / immersive**
- **Recentrer la vue immersive**
- **FPV dans le casque oui/non**

## Configuration

Fichier `config/propwash_vr.json` (créé au premier lancement) :

| Clé | Défaut | Rôle |
|---|---|---|
| `fpvInHeadset` | `true` | afficher la caméra de la drone dans le casque pendant le FPV |
| `viewMode` | `"GOGGLES"` | `"GOGGLES"` (lunettes) ou `"IMMERSIVE"` |
| `screenSizeDegrees` | `70` | largeur de l'écran des lunettes en degrés (20–140) ; largeur de l'OSD en mode immersif |
| `gogglesFrame` | `true` | cadre noir autour de l'écran des lunettes |
| `osd` | `true` | afficher l'OSD/HUD pendant le FPV |
| `immersiveStereo` | `1.0` | écart des yeux en mode immersif (1 = votre écart réel, 0 = mono) |
| `radio` | `"AUTO"` | `"AUTO"` (manettes VR sauf si une radio est branchée), `"ALWAYS"` ou `"OFF"` |
| `stickMode` | `2` | mode des sticks RC, 1 à 4 |
| `lockTurning` | `true` | bloquer la rotation Vivecraft pendant que les manettes VR pilotent |

Les menus (pause, inventaire, clavier et menu radial de Vivecraft) repassent automatiquement à la vue normale, pour
pouvoir les viser avec les manettes.

## Installation

1. Minecraft 26.3 avec Fabric (+ Fabric API) ou NeoForge.
2. [Propwash FPV](https://modrinth.com/mod/propwash-fpv) 0.5.0 ou plus récent.
3. [Vivecraft](https://modrinth.com/mod/vivecraft) pour 26.3 (1.3.13 ou plus récent).
4. Le jar de Propwash VR qui correspond à votre loader (`propwash_vr-fabric-26.3-*.jar` ou
   `propwash_vr-neoforge-26.3-*.jar`).

Le mod est uniquement client : inutile de l'installer sur le serveur.

## Compiler

```sh
./gradlew build
```

Il faut Java 25 (Gradle le télécharge via la toolchain si besoin). Les jars sortent dans `fabric/build/libs` et
`neoforge/build/libs`. Le workflow GitHub Actions `Build` fait la même chose à chaque push et publie les jars en
artefact.

Propwash FPV et Vivecraft viennent du Maven de Modrinth (`gradle.properties` : `propwash_version`,
`vivecraft_version`). Si une coordonnée ne se résout pas (numéro de version différent sur Modrinth), déposez
simplement les deux jars dans `libs/` : ils sont alors utilisés à la place.

## Comment ça marche

- **Caméra** (`client/VrFpv`, `mixin/vivecraft/VRDataMixin`) : Vivecraft rend chaque œil depuis
  `VRData#getEye` des données « world render » ; position de la caméra, rotation de vue, culling, overlays et
  position d'écoute du son en dépendent. Pendant le FPV, les poses LEFT, RIGHT et CENTER sont remplacées par des poses
  construites à partir de la pose caméra de Propwash (`FpvView#cameraPose`, la même pose retardée par la latence vidéo
  que l'écran plat). La pose de la tête reste intacte : visée, menus et corps du pilote fonctionnent normalement.
- **Lunettes** (`mixin/vivecraft/VRRendererMixin`, `math/GogglesMath`) : la projection de chaque œil est multipliée
  par `œilDansTête⁻¹ · échelle(z, z, 1) · œilDansTête`, avec `z = tan(écran/2) / tan(FOV caméra/2)` : toute l'image
  caméra tient dans l'écran virtuel, centré sur l'avant de la tête, y compris sur les casques à écrans inclinés.
- **Overlays** (`client/GogglesOverlay`, `mixin/vivecraft/VREffectsHelperMixin`) : les mains, le viseur, le panneau
  HUD flottant, etc. de Vivecraft appartiennent au corps du pilote ; pendant le FPV ils sont remplacés par le cadre des
  lunettes et le HUD (OSD) posé sur l'image.
- **Radio** (`client/VrRadio`, `mixin/propwash/TransmitterInputMixin`) : une radio simulée de Propwash
  (`TransmitterInput#simulate`, profil manette de jeu) est alimentée à chaque image par les actions d'entrée de
  Vivecraft, juste avant que Propwash lise sa radio.
- **Vol à vue** (`mixin/propwash/LosViewMixin`) : en VR, Propwash ne tourne plus le regard vers la drone.

## État

Version 0.1.0. Le code a été écrit à partir des sources de Propwash FPV 0.5.0 et de la branche `Multiloader-26.3`
de Vivecraft. La partie mathématique est couverte par des tests unitaires ; le reste n'a pas encore été essayé en jeu
avec un casque. Retours bienvenus dans les issues.

---

## English summary

Propwash VR is a client-side companion mod that makes [Propwash FPV](https://github.com/LardTom/propwash-fpv) work
in VR with [Vivecraft](https://github.com/Vivecraft/VivecraftMod) (Minecraft 26.3, Fabric and NeoForge).

- **FPV in the headset** while flying FPV or spectating a video channel: *goggles* mode (default; a flat,
  head-locked virtual screen with a black frame, like real FPV goggles, the whole camera picture fitted into a
  configurable screen size) or *immersive* mode (your eyes at the drone camera, head tracking on top, stereo).
- **OSD and video effects** stay on: Propwash's HUD is laid over the goggles picture.
- **VR controllers as the RC transmitter** while piloting in VR if no real radio or gamepad is connected
  (thumbsticks = RC sticks, mode 2 by default; right grip arms, left grip toggles angle mode, right trigger flips
  over, left trigger toggles GPS rescue, left stick click toggles the beeper). Vivecraft's turning, teleport and hotbar
  switching are held back meanwhile.
- **Line of sight in VR**: your head follows the drone, Propwash no longer turns the view.
- Settings in `config/propwash_vr.json`; three unbound key mappings (view mode, re-center, FPV in headset on/off),
  also bindable to controller buttons through Vivecraft's "Mod" action set.

Build with `./gradlew build` (Java 25). Dependencies come from the Modrinth Maven; if a coordinate does not resolve,
put the Propwash FPV and Vivecraft jars into `libs/`. Status: written against the Propwash FPV 0.5.0 and Vivecraft
`Multiloader-26.3` sources, math covered by unit tests, not yet tested in game with a headset.

MIT License. Propwash FPV is by lardtom, Vivecraft by the Vivecraft team; this mod only links against them.
