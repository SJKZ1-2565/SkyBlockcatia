package stevekung.mods.indicatia.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.MathHelper;
import stevekung.mods.indicatia.config.ConfigManagerIN;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin
{
    @Shadow
    @Final
    @Mutable
    private Minecraft mc;

    @Inject(method = "renderItemInFirstPerson(F)V", at =
        {
                @At(value = "INVOKE", target = "net/minecraft/client/renderer/ItemRenderer.transformFirstPersonItem(FF)V", shift = At.Shift.AFTER, ordinal = 0),
                @At(value = "INVOKE", target = "net/minecraft/client/renderer/ItemRenderer.transformFirstPersonItem(FF)V", shift = At.Shift.AFTER, ordinal = 1),
                @At(value = "INVOKE", target = "net/minecraft/client/renderer/ItemRenderer.transformFirstPersonItem(FF)V", shift = At.Shift.AFTER, ordinal = 2),
                @At(value = "INVOKE", target = "net/minecraft/client/renderer/ItemRenderer.transformFirstPersonItem(FF)V", shift = At.Shift.AFTER, ordinal = 3)
        })
    private void onItemUse(float partialTicks, CallbackInfo info)
    {
        if (ConfigManagerIN.enableBlockhitAnimation)
        {
            AbstractClientPlayer player = this.mc.thePlayer;
            float swingProgress = player.getSwingProgress(partialTicks);
            float f = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
            float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
            GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        }
    }
}