package stevekung.mods.indicatia.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import stevekung.mods.indicatia.config.ConfigManagerIN;
import stevekung.mods.indicatia.utils.ColorUtils;
import stevekung.mods.indicatia.utils.RenderUtils;

@Mixin(GuiMultiplayer.class)
public abstract class GuiMultiplayerMixin extends GuiScreen
{
    @Inject(method = "drawScreen(IIF)V", at = @At("RETURN"))
    private void drawInfo(int mouseX, int mouseY, float partialTicks, CallbackInfo info)
    {
        if (ConfigManagerIN.enableCustomServerSelectionGui)
        {
            RenderUtils.disableLighting();
            String info1 = "Press <SHIFT> for";
            String info2 = "server version info";
            this.mc.fontRendererObj.drawString(info1, 4, 3, ColorUtils.hexToRgb("#17F9DB"), true);
            this.mc.fontRendererObj.drawString(info2, 4, 3 + this.mc.fontRendererObj.FONT_HEIGHT + 1, ColorUtils.hexToRgb("#17F9DB"), true);
            RenderUtils.enableLighting();
        }
    }
}