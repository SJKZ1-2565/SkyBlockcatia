package stevekung.mods.indicatia.handler;

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ClientBlockBreakEvent extends Event
{
    private final World world;
    private final BlockPos pos;

    public ClientBlockBreakEvent(World world, BlockPos pos)
    {
        this.pos = pos;
        this.world = world;
    }

    public World getWorld()
    {
        return this.world;
    }

    public BlockPos getPos()
    {
        return this.pos;
    }
}