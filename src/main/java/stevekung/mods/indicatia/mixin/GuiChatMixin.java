package stevekung.mods.indicatia.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import stevekung.mods.indicatia.utils.GuiChatRegistry;
import stevekung.mods.indicatia.utils.IGuiChat;

@Mixin(GuiChat.class)
public abstract class GuiChatMixin extends GuiScreen
{
    @Inject(method = "initGui()V", at = @At("RETURN"))
    private void initGui(CallbackInfo info)
    {
        GuiChatRegistry.getGuiChatList().forEach(gui -> gui.initGui(this.buttonList, this.width, this.height));
    }

    @Inject(method = "drawScreen(IIF)V", at = @At("RETURN"))
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo info)
    {
        GuiChatRegistry.getGuiChatList().forEach(gui -> gui.drawScreen(this.buttonList, mouseX, mouseY, partialTicks));
    }

    @Inject(method = "updateScreen()V", at = @At("RETURN"))
    private void updateScreen(CallbackInfo info)
    {
        GuiChatRegistry.getGuiChatList().forEach(gui -> gui.updateScreen(this.buttonList, this.width, this.height));
    }

    @Inject(method = "onGuiClosed()V", at = @At("RETURN"))
    private void onGuiClosed(CallbackInfo info)
    {
        GuiChatRegistry.getGuiChatList().forEach(IGuiChat::onGuiClosed);
    }

    @Inject(method = "handleMouseInput()V", at = @At("RETURN"))
    private void handleMouseInput(CallbackInfo info) throws IOException
    {
        GuiChatRegistry.getGuiChatList().forEach(gui -> gui.handleMouseInput(this.width, this.height));
    }

    @Override
    public void sendChatMessage(String msg)
    {
        if (!msg.startsWith("/"))
        {
            for (IGuiChat chat : GuiChatRegistry.getGuiChatList())
            {
                this.sendChatMessage(chat.sendChatMessage(msg), true);
            }
        }
        else
        {
            super.sendChatMessage(msg);
        }
    }
}