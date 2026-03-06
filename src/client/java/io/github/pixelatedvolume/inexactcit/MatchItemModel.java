package io.github.pixelatedvolume.inexactcit;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.properties.select
    .SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select
    .SelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.renderer.item.properties.select
    .ComponentContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


/**
 * Implements the 'match' model type, which selects a conditional item model
 * based on a regular expression predicate.
 */
public class MatchItemModel<T> implements ItemModel {

    /* Property being tested */
    private final SelectItemModelProperty<T> property;
    /* The match arms of the model */
    private final List<Pair<Pattern, ItemModel>> arms;
    /* Default model */
    private final ItemModel fallback;

    /**
     * Constructs new MatchItemModel.
     * @param   property    The property that will be used for matching
       @param   arms        Assoc list containing pairs of test regexps with
                            corresponding models.
       @param   fallback    Model to use if no match arm is applicable
     */
    public MatchItemModel(SelectItemModelProperty<T> property,
                          List<Pair<Pattern, ItemModel>> arms,
                          ItemModel fallback)
    {
        this.property = property;
        this.arms     = arms;
        this.fallback = fallback;
    }

    /**
     * Called to resolve the model.
     * @param   renderState     Unused
     * @param   items           ItemStack of the item being processed
     * @param   resolver        Needed to get the test predicate
     * @param   displayContext  Needed to get the test predicate
     * @param   level           Needed to get the test predicate
     * @param   owner           Needed to get the test predicate
     * @param   i               Unknown
     */
    @Override
    public void update(ItemStackRenderState   renderState,
                       ItemStack              items,
                       ItemModelResolver      resolver,
                       ItemDisplayContext     displayContext,
                       @Nullable ClientLevel  level,
                       @Nullable ItemOwner    owner,
                       int                    i)
    {
        ItemModel model = this.fallback;
        LivingEntity ownerAsLE = owner == null? null : owner.asLivingEntity();
        T value = this.property.get(items,
                                    level,
                                    ownerAsLE,
                                    i,
                                    displayContext);
        if (value != null) {
            /* This may eventually need a more sophisticated test */
            String testValue = value.toString();
            for (Pair<Pattern, ItemModel> arm : this.arms) {
                if (arm.getFirst().matcher(testValue).matches()) {
                    model = arm.getSecond();
                    break;
                }
            }
        }
        model.update(renderState,
                     items,
                     resolver,
                     displayContext,
                     level,
                     owner,
                     i);
    }



    /**
     * Unbaked (i.e. unoptimized) class that contains the information needed
     * to construct a MatchItemModel.
     */
    public static class Unbaked implements ItemModel.Unbaked {
        
        /* Name used in the "type" field to identify a MatchItemModel */
        public static final ResourceLocation TYPE_ID =
            ResourceLocation.fromNamespaceAndPath(InexactCITMod.MOD_ID,
                                                  "match");

        private final UnbakedSwitch<?> unbakedSwitch;
        private final Optional<ItemModel.Unbaked> fallback;

        /**
         * Codec for handling the client item JSON
         */
        public static final MapCodec<Unbaked> MAP_CODEC =
            RecordCodecBuilder.mapCodec((instance) -> {
                    return instance
                        .group(UnbakedSwitch
                               .MAP_CODEC
                               .forGetter((unbaked) ->
                                   unbaked.unbakedSwitch),
                               ItemModels
                               .CODEC
                               .optionalFieldOf("fallback")
                               .forGetter((Unbaked unbaked) ->
                                          unbaked.fallback)
                               ).apply(instance, Unbaked::new);
                });

        /**
         * Constructs a new MatchItemModel.Unbaked
         * @param   unbakedSwitch   Object representing the switch cases as
                                    read in from the JSON
         * @param   fallback        Unbaked version of the fallback ItemModel
         */
        public Unbaked(UnbakedSwitch<?>            unbakedSwitch,
                       Optional<ItemModel.Unbaked> fallback)
        {
            this.unbakedSwitch = unbakedSwitch;
            this.fallback = fallback;
        }

        /**
         * Obligatory for interface, not sure what happens here
         */
        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        /**
         * Recursively bakes submodels in the given context.
         * @param   context The BakingContext for the operation
         */
        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            ItemModel bakedFallback = this.fallback
                .map((model) -> model.bake(context))
                .orElse(context.missingItemModel());
            return this.unbakedSwitch.bake(context, bakedFallback);
        }

        /**
         * Recursively resolves dependencies among submodels using the given
         * resolver.
         * @param   resolver    The Resolver for the operation
         */
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.unbakedSwitch.resolveDependencies(resolver);
            this.fallback.ifPresent((model) ->
                                    model.resolveDependencies(resolver));
        }
    }



    /**
     * Class that represents a single arm of the match specification.
     * @param   regexSource A String that will be turned into a regular
     *                      expression
     * @param   model       The model used when the corresponding regular
     *                      expression matches
     */
    public static record SwitchCase(String            regexSource,
                                    ItemModel.Unbaked model)
    {
        /**
         * Tests if a regular expression can be made out of the string and
         * returns a success with it if so.  Helper for the SwitchCase codec.
         */
        private static DataResult<String> validateRegex(String regexString) {
            try {
                Pattern.compile(regexString);
                return DataResult.success(regexString);
            } catch (PatternSyntaxException e) {
                return DataResult.error(() -> "Invalid regex pattern: "
                                              + e.getMessage());
            }
        }

        /* Codec for handling match arms */
        public static final Codec<SwitchCase> CODEC =
            RecordCodecBuilder.create((instance) -> {
                    return instance
                   .group(Codec
                          .STRING
                          .fieldOf("regex")
                          .flatXmap((regexString) -> validateRegex(regexString),
                                    DataResult::success)
                          .forGetter(SwitchCase::regexSource),
                          ItemModels.CODEC.fieldOf("model")
                          .forGetter(SwitchCase::model))
                   .apply(instance, SwitchCase::new);
                });
    }



    /**
     * Class that represents a whole match specification; made of collected
     * match arms.
     * @param   property    Selector for the property of the item that will
     *                      be tested
     * @param   arms        List of SwitchCases; the arms of the match
     */
    public static record UnbakedSwitch<T>(SelectItemModelProperty<T> property,
                                          List<SwitchCase>           arms)
    {
        /**
         * Gets the data component type of an UnbakedSwitch's test property.
         * Helper method for creating UnbakedSwitch codec.
         */
        private static DataComponentType<?>
            getComponentType(UnbakedSwitch<?> unbaked)
        {
            return ((ComponentContents<?>) unbaked.property())
                   .componentType();
        }

        /**
         * Creates an UnbakedSwitch from a property and cases.  Helper method
         * for creating UnbakedSwitch codec.
         */
        private static UnbakedSwitch<?>
            createSwitch(DataComponentType<?> componentType,
                         List<SwitchCase>     cases)
        {
            var property = new ComponentContents<>(componentType);
            return new UnbakedSwitch<>(property, cases);
        }

        /**
         * Creates a codec for the UnbakedSwitch.
         */
        private static MapCodec<UnbakedSwitch<?>>
            createSwitchCodec()
        {
            /* Codec for the component field (lore, custom_name, etc.) */
            final Codec<DataComponentType<?>> componentTypeCodec =
                BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec();
            /* Get the field name of the component for matching */
            var componentField =
                componentTypeCodec
                .fieldOf("component")
                .forGetter((UnbakedSwitch<?> unbaked) ->
                           UnbakedSwitch.getComponentType(unbaked));
            /* Get the set of match cases */
            var casesField =
                SwitchCase
                .CODEC
                .listOf()
                .fieldOf("cases")
                .forGetter((UnbakedSwitch<?> unbaked) ->
                           unbaked.arms());
            /* Make the final codec */
            return RecordCodecBuilder
                  .mapCodec(inst ->
                            inst.group(componentField,
                                       casesField)
                                .apply(inst, UnbakedSwitch::createSwitch));
        }

        /* Codec for handling switches */
        public static final MapCodec<UnbakedSwitch<?>> MAP_CODEC =
            SelectItemModelProperties
            .CODEC
            .dispatchMap("property",
                         (unbaked) -> unbaked.property().type(),
                         (type) -> UnbakedSwitch.createSwitchCodec());
                          
                                 
        /**
         * Produces a baked MatchItemModel made from of the recursively
         * baked arms of the match specification and the baked fallback model
         */
        public ItemModel bake(ItemModel.BakingContext context,
                              ItemModel               fallback)
        {
            List<Pair<Pattern, ItemModel>> compiledArms =
                new ArrayList<>();
            for (SwitchCase arm : this.arms) {
                try {
                    Pattern pattern =
                        Pattern.compile(arm.regexSource);
                    ItemModel bakedModel =
                        arm.model().bake(context);
                    compiledArms.add(Pair.of(pattern, bakedModel));
                } catch (PatternSyntaxException e) {
                    /* This shouldn't ever happpen, all regex are validated
                     * during reading */
                    InexactCITMod.LOGGER.error("Invalid pattern "
                                               + arm.regexSource.toString());
                }
            }
            return new MatchItemModel<T>(this.property,
                                        compiledArms,
                                        fallback);
        }

        /**
         * Calls resolveDependencies on the models in the arms of the match
         * @param   resolver    Resolver to use for the operation
         */
        public void resolveDependencies
            (ResolvableModel.Resolver resolver)
        {
            this.arms.forEach((switchCase) ->
                              switchCase
                              .model
                              .resolveDependencies(resolver));
        }
    }
}
