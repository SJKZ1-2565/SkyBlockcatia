package stevekung.mods.indicatia.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.renderer.entity.layers.LayerArrow;
import stevekung.mods.indicatia.utils.RenderUtils;

@Mixin(LayerArrow.class)
public abstract class LayerArrowMixin
{
    @Redirect(method = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/RenderHelper.disableStandardItemLighting()V"))
    private void disableLighting()
    {
        RenderUtils.disableLighting();
    }

    @Redirect(method = "doRenderLayer(Lnet/minecraft/entity/EntityLivingBase;FFFFFFF)V", at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/RenderHelper.enableStandardItemLighting()V"))
    private void enableLighting()
    {
        RenderUtils.enableLighting();
    }
}