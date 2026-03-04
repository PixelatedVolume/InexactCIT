package io.github.pixelatedvolume.inexactcit.mixin;

import io.github.pixelatedvolume.inexactcit.MatchItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModels.class)
public class ItemModelsMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void registerMatchModel(CallbackInfo ci) {
        ItemModels.ID_MAPPER.put(MatchItemModel.Unbaked.TYPE_ID,
                                 MatchItemModel.Unbaked.MAP_CODEC);
    }
}
