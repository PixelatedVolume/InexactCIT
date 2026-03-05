INEXACT CIT
===========
Restoring pattern-based conditional item textures.


Purpose
-------
The Custom Item Textures (CIT) mod, originally part of McPatcher and then
Optifine, allowed the replacement of textures whose metadata matched inexact
patterns.  We now have near-equivalent item-texture replacement in standard
Minecraft with conditional client item types, though inexact matching is no
longer possible.  This mod adds CIT replacement with regular expressions to
the new standard conditional client item system.


Status
------
This mod is not yet finished.  Core functionality works, but behavior outside
of the documented `minecraft:component` use case has not been tested.   Edge
cases involving complex components or malformed regular expressions may not
be handled gracefully.


Compatibility and dependencies
------------------------------
- Minecraft: 1.21.8
- Loader: Fabric 0.136.1

Fabric API not required.


Installation
------------
Add the mod JAR to your Fabric mods folder.


Usage (Players)
---------------
This mod adds a feature to item models which can be used by resource pack
designers.  It is always active when the mod is installed.


Resource Pack Integration (Pack Authors)
----------------------------------------
The JSON files found at `assets/minecraft/item` are *item definitions*.
These are not necessarily the model file used to render an item (i.e. an
*item model*), which are found at `assets/minecraft/models/item`.

In an item definition file, the JSON object in the "model" field determines
which model file is used to render an item.  Typically, a single item type is
always rendered with the same model, so this object has its "type" set to
`minecraft:model`.  But different models can be used for the same item in
different contexts by using "model" objects with other types, such as
`minecraft:condition` and `minecraft:select`.

This mod adds the `inexactcit:match` item definition model type.  It is
similar to the built-in `select` type in that it contains a list of models
with associated conditions, and when one condition matches, its associated
model is used to render the item.  Unlike Select, the Match conditions are
potentially-inexact regular expressions.

Match item definition models use the same fields as Select, particularly
"property", "component", "cases", and "fallback".

#### Labeled Field Structure
        { "model":
          { "type": "inexactcit:match"
          , "property":  ...     # Probably always minecraft:component
          , "component": ...     # Name of component to target
          , "cases":
            [ { "regex": ...     # Regular expression for this case
              , "model": { ... } # Item def'n. model used when regexp matches
              }
            , ...                # Another case, as above
            , ...                # Any number of additional cases
            ]
          , "fallback": { ... }  # Item def'n. model used when no case matches
          }
        }

#### Required Fields
Regular expression matching is intended for text metadata, so the "property"
field will be `minecraft:component`.  Behavior when "property" is not
`minecraft:component` is as yet untested.

The "component" field should be set to the namespaced ID for the component
that will be tested by the condition, e.g. `minecraft:lore`,
`minecraft:custom_name`.

The "cases" field must contain a list of JSON objects.  Each object is to
contain a "regex" field and corresponding "model" field.  These must be a
(Java-flavored) regular expression and an item definition model, respectively.
When the regular expression matches against the contents of the component
named in the "component" field, the corresponding model is used for that item.

The "fallback" field gives a model that is used when none of the "case"
objects match, same as the standard Select.

#### Example
The following is an example of a Match client item model.  It replaces all
wooden hoes whose names include the word "Quarterstaff" with the model at
`pixelatedvolume:item/quarterstaff`.

        { "model":
          { "type": "inexactcit:match"
          , "property": "minecraft:component"
          , "component": "minecraft:custom_name"
          , "cases":
            [ { "model":
                { "model": "pixelatedvolume:item/quarterstaff"
                , "type": "minecraft:model"
                }
              , "regex": ".*Quarterstaff.*"
              }
            ]
          , "fallback":
            { "model": "minecraft:item/wooden_hoe"
            , "type": "minecraft:model"
            }
          }
          , "oversized_in_gui": true
        }

#### Text Comparison
The contents of the field being matched against is serialized with `toString`.
Text-component (JSON) features, section-code formatting, etc, is all pushed
into this comparison string, so there is no way of telling what might come
before or after the text that is displayed in-game, or what happens between
lines of multiline compontents.

#### Multiple Target Components
To give one item multiple replacement rules that use different components
(e.g. one set of rules for `lore` and one set for `custom_name`), set the
fallback model to a new Match client item model that selects the second
component.
