package com.stevekung.skyblockcatia.mixin.renderer.entity.player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.stevekung.skyblockcatia.gui.screen.SkyBlockAPIViewerScreen;
import com.stevekung.skyblockcatia.renderer.GlowingSteveLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.PlayerModelPart;

@Mixin(PlayerRenderer.class)
public abstract class MixinPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    MixinPlayerRenderer()
    {
        super(null, null, 0);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;Z)V", at = @At("RETURN"))
    private void init(EntityRenderDispatcher renderManager, boolean useSmallArms, CallbackInfo info)
    {
        this.addLayer(new GlowingSteveLayer((PlayerRenderer) (Object) this));
    }

    @Redirect(method = "setModelProperties", at = @At(value = "INVOKE", target = "net/minecraft/client/player/AbstractClientPlayer.isModelPartShown(Lnet/minecraft/world/entity/player/PlayerModelPart;)Z"))
    private boolean fixSecondLayer(AbstractClientPlayer clientPlayer, PlayerModelPart part)
    {
        return SkyBlockAPIViewerScreen.renderSecondLayer || clientPlayer.isModelPartShown(part);
    }
}