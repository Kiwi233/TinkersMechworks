package tmechworks;

import tmechworks.network.packet.PacketPipeline;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.MinecraftForge;
import tmechworks.client.SignalTetherWorldOverlayRenderer;
import tmechworks.common.CommonProxy;
import tmechworks.common.MechContent;
import tmechworks.lib.ConfigCore;
import tmechworks.lib.Repo;
import tmechworks.lib.TMechworksRegistry;
import tmechworks.lib.multiblock.MultiblockEventHandler;
import tmechworks.lib.util.TabTools;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = Repo.modId, name = Repo.modName, version = Repo.modVer, dependencies = "required-after:TConstruct;required-after:Mantle")
public class TMechworks {

    // Shared mod logger
    public static final Logger logger = LogManager.getLogger("TMechworks");

    /* Instance of this mod, used for grabbing prototype fields */
    @Instance("TMechworks")
    public static TMechworks instance;
    /* Proxies for sides, used for graphics processing */
    @SidedProxy(clientSide = "tmechworks.client.ClientProxy", serverSide = "tmechworks.common.CommonProxy")
    public static CommonProxy proxy;
    
    public static final PacketPipeline packetPipeline = new PacketPipeline();

    public TMechworks ()
    {
        //logger.setParent(FMLCommonHandler.instance().getFMLLogger());
    }

    @EventHandler
    public void preInit (FMLPreInitializationEvent event)
    {
        ConfigCore.loadConfig(new Configuration(event.getSuggestedConfigurationFile()));

        
        TMechworksRegistry.Mechworks = new TabTools("TMechworks");

        content = new MechContent();

        proxy.registerRenderer();
        proxy.registerTickHandler();
        
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);
        
        MinecraftForge.EVENT_BUS.register(new MultiblockEventHandler());
    }

    @EventHandler
    public void init (FMLInitializationEvent event)
    {
    	packetPipeline.initalise();
        if (event.getSide() == Side.CLIENT)
        {
        	MinecraftForge.EVENT_BUS.register(new SignalTetherWorldOverlayRenderer());
        }
    }

    @EventHandler
    public void postInit (FMLPostInitializationEvent evt)
    {
    	packetPipeline.postInitialise();
        content.postInit();
        proxy.postInit();

    }

    public static MechContent content;
}
