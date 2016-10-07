package com.teamwizardry.refraction.common.proxy;

import com.teamwizardry.librarianlib.common.network.PacketHandler;
import com.teamwizardry.refraction.Refraction;
import com.teamwizardry.refraction.client.gui.GuiHandler;
import com.teamwizardry.refraction.common.core.CatChaseHandler;
import com.teamwizardry.refraction.common.core.DispenserScrewDriverBehavior;
import com.teamwizardry.refraction.common.network.PacketLaserFX;
import com.teamwizardry.refraction.init.*;
import net.minecraft.block.BlockDispenser;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

/**
 * Created by LordSaad44
 */
public class CommonProxy {


	public void preInit(FMLPreInitializationEvent event) {
		CatChaseHandler.INSTANCE.getClass(); // load the class
		ModSounds.init();
		ModBlocks.init();
		ModItems.init();
		ModEntities.init();
		ModEffects.init();

		NetworkRegistry.INSTANCE.registerGuiHandler(Refraction.instance, new GuiHandler());
		PacketHandler.register(PacketLaserFX.class, Side.CLIENT);
	}

	public void init(FMLInitializationEvent event) {
		CraftingRecipes.init();
		AssemblyRecipes.init();
	}

	public void postInit(FMLPostInitializationEvent event) {
		BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ModItems.SCREW_DRIVER, new DispenserScrewDriverBehavior());
	}

	public boolean isClient() {
		return false;
	}

	public MinecraftServer getServer() {
		return FMLServerHandler.instance().getServer();
	}
}
