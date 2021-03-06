package tmechworks.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import mantle.blocks.abstracts.InventoryLogic;
import tmechworks.lib.multiblock.MultiblockServerTickHandler;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;

public class CommonProxy implements IGuiHandler
{

    public static int drawbridgeID = 0;
    public static int advDrawbridgeID = 1;

    @Override
    public Object getServerGuiElement (int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        if (ID < 0)
            return null;

        if (ID < 100)
        {
            TileEntity tile = world.func_147438_o(x, y, z);
            if (tile != null && tile instanceof InventoryLogic)
            {
                return ((InventoryLogic) tile).getGuiContainer(player.inventory, world, x, y, z);
            }
        }
        return null;
    }

    @Override
    public Object getClientGuiElement (int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        return null;
    }
    
    public void registerTickHandler ()
    {
        new MultiblockServerTickHandler();
    }

    public void registerRenderer ()
    {
    }

    public void postInit ()
    {
    }
}
