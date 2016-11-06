package com.teamwizardry.refraction.common.tile;

import com.teamwizardry.librarianlib.client.fx.particle.ParticleBuilder;
import com.teamwizardry.librarianlib.client.fx.particle.ParticleSpawner;
import com.teamwizardry.librarianlib.client.fx.particle.functions.InterpFadeInOut;
import com.teamwizardry.librarianlib.common.base.block.TileMod;
import com.teamwizardry.librarianlib.common.util.autoregister.TileRegister;
import com.teamwizardry.librarianlib.common.util.math.interpolate.StaticInterp;
import com.teamwizardry.refraction.Refraction;
import com.teamwizardry.refraction.api.Constants;
import com.teamwizardry.refraction.api.PosUtils;
import com.teamwizardry.refraction.api.soundmanager.IConditionalSoundEmitter;
import com.teamwizardry.refraction.common.light.Beam;
import com.teamwizardry.refraction.common.light.ILightSource;
import com.teamwizardry.refraction.common.light.ReflectionTracker;
import net.minecraft.block.BlockDirectional;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by LordSaad44
 */
@TileRegister(id = "laser")
public class TileLaser extends TileMod implements ILightSource, ITickable, IConditionalSoundEmitter {

	public ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return null;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (stack.getItem() == Items.GLOWSTONE_DUST) return super.insertItem(slot, stack, simulate);
			else return stack;
		}
	};
	private int tick = 0;

	public TileLaser() {
	}

	@Override
	public void readCustomNBT(NBTTagCompound compound) {
		inventory.deserializeNBT(compound.getCompoundTag("inventory"));
	}

	@Override
	public void writeCustomNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());
	}

	@Override
	public boolean getUseFastSync() {
		return false;
	}

	@Override
	public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, net.minecraft.util.EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, net.minecraft.util.EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) inventory : super.getCapability(capability, facing);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		ReflectionTracker.getInstance(worldObj).addSource(this);
	}

	@Override
	public void generateBeam() {
		if (inventory.getStackInSlot(0) != null && inventory.getStackInSlot(0).stackSize > 0) {
			Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			EnumFacing face = worldObj.getBlockState(pos).getValue(BlockDirectional.FACING);
			Vec3d vec = PosUtils.getVecFromFacing(face);
			Color color = new Color(255, 255, 255, Constants.GLOWSTONE_ALPHA);
			new Beam(worldObj, center, vec, color).spawn();

			if (tick < 50) tick++;
			else {
				tick = 0;
				inventory.getStackInSlot(0).stackSize--;
				markDirty();
			}
		}
	}

	@Override
	public void update() {
		if (inventory.getStackInSlot(0) != null && inventory.getStackInSlot(0).stackSize > 0) {
			ParticleBuilder glitter = new ParticleBuilder(ThreadLocalRandom.current().nextInt(20, 30));
			glitter.setScale((float) ThreadLocalRandom.current().nextDouble(0.5, 1));
			glitter.setAlpha((float) ThreadLocalRandom.current().nextDouble(0.3, 0.7));
			glitter.setRender(new ResourceLocation(Refraction.MOD_ID, "particles/glow"));
			glitter.setAlphaFunction(new InterpFadeInOut(0.1f, 1.0f));
			EnumFacing face = worldObj.getBlockState(pos).getValue(BlockDirectional.FACING);
			Vec3d facingVec = PosUtils.getVecFromFacing(face).scale(1.0 / 3.0);
			Vec3d center = new Vec3d(pos).addVector(0.5, 0.5, 0.5).add(facingVec);
			glitter.setMotion(facingVec.scale(1.0 / 50.0));
			ParticleSpawner.spawn(glitter, worldObj, new StaticInterp<>(center), 2);
		}
	}

	@Override
	public boolean shouldEmit() {
		return inventory.getStackInSlot(0) != null && inventory.getStackInSlot(0).stackSize > 0;
	}
}
