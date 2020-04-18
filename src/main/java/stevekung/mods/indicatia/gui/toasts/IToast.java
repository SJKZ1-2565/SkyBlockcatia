package stevekung.mods.indicatia.gui.toasts;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

public interface IToast<T>
{
    Object NO_TOKEN = new Object();
    IToast.Visibility draw(GuiToast toastGui, long delta);

    default Object getType()
    {
        return NO_TOKEN;
    }

    default boolean equal(T toast)
    {
        return true;
    }

    public static enum Visibility
    {
        SHOW("indicatia:ui.toast.in"),
        HIDE("indicatia:ui.toast.out");

        private final String sound;

        private Visibility(String sound)
        {
            this.sound = sound;
        }

        public void playSound(SoundHandler handler)
        {
            handler.playSound(new PositionedSoundRecord(new ResourceLocation(this.sound), 1.0F, 1.0F, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
        }
    }
}