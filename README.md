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


Installation
------------
Add the mod's JAR to your Fabric mods folder.


Use
---
This mod adds the `inexactcit:match` client item type; it is analogous to the
built-in `select` type.  Match client item models use the same fields as
Select, particularly "property", "component", "cases", and "fallback".

The following is an example of a Match client item model.  It replaces all
wooden hoes whose names include the word "Quarterstaff" with the model at
`pixelatedvolume:item/quarterstaff`.

        { "model":
          { "type": "inexactcit:match"
          , "property": "minecraft:component"
          , "component": "minecraft:lore"
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

This feature is intended for matching metadata, so the "property" field will
typically be `minecraft:component`.  The "component" field should be set to
the namespaced ID for the component that will be tested by the condition, e.g.
`minecraft:lore`, `minecraft:custom_name`.

Behavior when "property" is not `minecraft:component` is as yet untested.

The "cases" field must contain a list of objects.  Each object is to contain
a "regex" field and corresponding "model" field.  These must be, respectively,
a (Java-flavored) regular expression and a model definition.  When the regular
expression matches against the contents of the specified "component" field,
the corresponding model is used for the item.

The "fallback" field gives a model that is used when none of the "case"
objects match, same as the standard Select.

To give one item multiple replacement rules that use different components
(e.g. one set of rules for `lore` and one set for `custom_name`), set the
fallback model to a new Match client item model that selects the second
component.  This is untested.
