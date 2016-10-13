package com.teamwizardry.refraction.common.tile;

import com.teamwizardry.refraction.api.Utils;
import com.teamwizardry.refraction.common.light.Beam;
import com.teamwizardry.refraction.common.light.IBeamHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

import java.awt.*;

/**
 * Created by Saad on 9/11/2016.
 */
public class TileSpectrometer extends TileEntity implements IBeamHandler, ITickable {

	public Color maxColor = Color.BLACK, currentColor = Color.BLACK;
	public int maxTransparency = 0, currentTransparency = 0;
	private IBlockState state;
	private Beam[] beams = new Beam[]{};
	private int tick = 0;

	public TileSpectrometer() {
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey("max_color"))
			maxColor = new Color(compound.getInteger("max_color"));
		if (compound.hasKey("current_color"))
			currentColor = new Color(compound.getInteger("current_color"));
		if (compound.hasKey("max_transparency"))
			maxTransparency = compound.getInteger("max_transparency");
		if (compound.hasKey("current_transparency"))
			currentTransparency = compound.getInteger("current_transparency");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound = super.writeToNBT(compound);
		compound.setInteger("max_color", maxColor.getRGB());
		compound.setInteger("current_color", currentColor.getRGB());
		compound.setInteger("max_transparency", maxTransparency);
		compound.setInteger("current_transparency", currentTransparency);
		return compound;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new SPacketUpdateTileEntity(pos, 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		super.onDataPacket(net, packet);
		readFromNBT(packet.getNbtCompound());

		state = worldObj.getBlockState(pos);
		worldObj.notifyBlockUpdate(pos, state, state, 3);
	}

	@Override
	public void handle(Beam... beams) {
		this.beams = beams;
		tick = 1;
	}

	@Override
	public void update() {
		if (worldObj.isRemote) return;
		if (tick < 10) tick++;
		else {
			tick = 0;
			beams = null;
			maxColor = new Color(0, 0, 0);
			maxTransparency = 0;
			worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
		}

		if (currentColor.getRGB() != maxColor.getRGB() || currentTransparency != maxTransparency) {
			currentColor = Utils.mixColors(currentColor, maxColor, 0.9);
			double inverse_percent = 1.0 - 0.9;
			double transparency = currentTransparency * 0.9 + maxTransparency * inverse_percent;
			currentTransparency = (int) transparency;
			worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
		}

		if (beams == null || beams.length == 0) return;

		int red = 0;
		int green = 0;
		int blue = 0;
		int alpha = 0;

		for (Beam beam : beams) {
			Color color = beam.color;
			red += color.getRed();
			green += color.getGreen();
			blue += color.getBlue();
			alpha += color.getAlpha();
		}
		red = Math.min(red / beams.length, 255);
		green = Math.min(green / beams.length, 255);
		blue = Math.min(blue / beams.length, 255);

		float[] hsbvals = Color.RGBtoHSB(red, green, blue, null);
		Color color = new Color(Color.HSBtoRGB(hsbvals[0], hsbvals[1], 1));
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(alpha, 255));

		if (color.getRGB() == maxColor.getRGB()) return;
		markDirty();
		this.maxColor = color;
		this.maxTransparency = color.getAlpha();
		worldObj.notifyBlockUpdate(pos, worldObj.getBlockState(pos), worldObj.getBlockState(pos), 3);
	}
}