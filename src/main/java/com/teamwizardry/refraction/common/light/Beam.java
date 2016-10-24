package com.teamwizardry.refraction.common.light;

import com.teamwizardry.librarianlib.common.network.PacketHandler;
import com.teamwizardry.refraction.api.Effect;
import com.teamwizardry.refraction.api.Effect.EffectType;
import com.teamwizardry.refraction.client.render.RenderLaserUtil;
import com.teamwizardry.refraction.common.network.PacketLaserFX;
import com.teamwizardry.refraction.common.raytrace.EntityTrace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Beam {

	public Vec3d initLoc;
	public Vec3d slope;
	public Vec3d finalLoc;
	public Color color;
	public World world;
	public Effect effect;
	public boolean enableEffect = true, ignoreEntities = false;
	public RayTraceResult trace;
	private ArrayList<BlockPos> lastTouchedBlocks = new ArrayList<>();
	private BlockPos lastTouchedBlock = null;

	public Beam(World world, Vec3d initLoc, Vec3d slope, Color color) {
		this.world = world;
		this.initLoc = initLoc;
		this.slope = slope;
		this.finalLoc = slope.normalize().scale(128).add(initLoc);
		this.color = color;
	}

	public Beam(World world, double initX, double initY, double initZ, double slopeX, double slopeY, double slopeZ, Color color) {
		this(world, new Vec3d(initX, initY, initZ), new Vec3d(slopeX, slopeY, slopeZ), color);
	}

	public Beam(World world, Vec3d initLoc, Vec3d dir, float red, float green, float blue, float alpha) {
		this(world, initLoc, dir, new Color(red, green, blue, alpha));
	}

	public Beam(World world, double initX, double initY, double initZ, double slopeX, double slopeY, double slopeZ, float red, float green, float blue, float alpha) {
		this(world, initX, initY, initZ, slopeX, slopeY, slopeZ, new Color(red, green, blue, alpha));
	}

	public Beam createSimilarBeam() {
		return createSimilarBeam(initLoc, finalLoc);
	}

	public Beam createSimilarBeam(Vec3d slope) {
		return createSimilarBeam(finalLoc, slope);
	}

	public Beam createSimilarBeam(Vec3d init, Vec3d dir) {
		return new Beam(world, init, dir, color).setIgnoreEntities(ignoreEntities).setEnableEffect(enableEffect).setLastTouchedBlocks(lastTouchedBlocks).setLastTouchedBlock(lastTouchedBlock);
	}

	public Beam setSlope(Vec3d slope) {
		this.slope = slope;
		this.finalLoc = slope.normalize().scale(128).add(initLoc);
		return this;
	}

	public Beam setLastTouchedBlock(BlockPos lastTouchedBlock) {
		this.lastTouchedBlock = lastTouchedBlock;
		return this;
	}

	public Beam setColor(Color color) {
		this.color = color;
		return this;
	}

	public Beam setIgnoreEntities(boolean ignoreEntities) {
		this.ignoreEntities = ignoreEntities;
		return this;
	}

	public Beam setEnableEffect(boolean enableEffect) {
		this.enableEffect = enableEffect;
		return this;
	}

	public Beam setInitLoc(Vec3d initLoc) {
		this.initLoc = initLoc;
		this.finalLoc = slope.normalize().scale(128).add(initLoc);
		return this;
	}

	public Beam setEffect(Effect effect) {
		this.effect = effect;
		return this;
	}

	public Beam setLastTouchedBlocks(ArrayList<BlockPos> lastTouchedBlocks) {
		this.lastTouchedBlocks = lastTouchedBlocks;
		return this;
	}

	public void spawn() {
		if (world == null) return;
		if (world.isRemote) return;
		Effect effect = EffectTracker.getEffect(this);
		if (effect != null) {
			if (effect.getCooldown() == 0) this.effect = effect;
			else if (ThreadLocalRandom.current().nextInt(0, effect.getCooldown()) == 0) this.effect = effect;
		} else this.effect = EffectTracker.getEffect(this);


		if (!ignoreEntities) {
			if (effect != null) {
				if (effect.getType() == EffectType.BEAM)
					trace = EntityTrace.cast(world, initLoc, slope, 128, true);
				else trace = EntityTrace.cast(world, initLoc, slope, 128, false);
			} else trace = EntityTrace.cast(world, initLoc, slope, 128, false);
		} else trace = EntityTrace.cast(world, initLoc, slope, 128, true);
		if (trace == null) return;
		if (trace.hitVec == null) return;
		this.finalLoc = trace.hitVec;

		/*if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
			EnumFacing facing = Utils.getCollisionSide(this);
			if (facing != null) {
				BlockPos pos = new BlockPos(finalLoc).offset(facing);
				if (lastTouchedBlocks.size() < 3) lastTouchedBlocks.add(pos);
				else {
					int i = Collections.frequency(lastTouchedBlocks, pos);
					if (i >= 3) return;
					else lastTouchedBlocks.remove(0).add(pos);
				}
			}
		}*/

		/*if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos pos = trace.getBlockPos();
			if (lastTouchedBlock == null) lastTouchedBlock = pos;
			else if (lastTouchedBlock.getX() == pos.getX()
					&& lastTouchedBlock.getY() == pos.getY()
					&& lastTouchedBlock.getZ() == pos.getZ()) return;
			else lastTouchedBlock = pos;
		}*/

		if (finalLoc.distanceTo(initLoc) <= 1 / 8) return;

		if (enableEffect && trace.typeOfHit == RayTraceResult.Type.ENTITY) {
			if (effect != null) {
				if (effect.getType() == EffectType.SINGLE) EffectTracker.addEffect(world, trace.hitVec, effect);
				else if (effect.getType() == EffectType.BEAM) EffectTracker.addEffect(world, this);
			}
		} else if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
			try {
				if (enableEffect && effect != null && effect.getType() == EffectType.BEAM)
					EffectTracker.addEffect(world, this);
				TileEntity tile = world.getTileEntity(trace.getBlockPos());
				if (tile instanceof IBeamHandler)
					ReflectionTracker.getInstance(world).recieveBeam((IBeamHandler) tile, this);
				else if (enableEffect) {
					BlockPos pos = trace.getBlockPos();
					if (effect != null && effect.getType() == EffectType.SINGLE)
						EffectTracker.addEffect(world, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), effect);
				}
			} catch (NullPointerException ignored) // Don't really care about these NPEs, all they mean is that the BlockPos is outside the world height limit.
			{
			}
		} else if (enableEffect && trace.typeOfHit == RayTraceResult.Type.MISS) {
			if (effect != null && effect.getType() == EffectType.BEAM)
				EffectTracker.addEffect(world, this);
		}

		PacketHandler.INSTANCE.getNetwork().sendToAllAround(new PacketLaserFX(initLoc, finalLoc, color), new NetworkRegistry.TargetPoint(world.provider.getDimension(), initLoc.xCoord, initLoc.yCoord, initLoc.zCoord, 256));
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Beam))
			return false;
		Beam beam = (Beam) other;
		float[] self = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		float[] others = Color.RGBtoHSB(beam.color.getRed(), beam.color.getBlue(), beam.color.getGreen(), null);
		return initLoc.equals(beam.initLoc) && finalLoc.equals(beam.finalLoc) && self[0] == others[0] && self[1] == others[1] && self[2] == others[2] && color.getAlpha() == beam.color.getAlpha();
	}

	@Override
	public int hashCode() {
		float[] self = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		int start = initLoc.hashCode();
		int end = finalLoc.hashCode();
		long h = Double.doubleToLongBits(self[0]);
		long s = Double.doubleToLongBits(self[1]);
		long v = Double.doubleToLongBits(self[2]);
		long a = Double.doubleToLongBits(this.color.getAlpha());
		int color = (int) (h ^ h >>> 32);
		color = 31 * color + (int) (s ^ s >>> 32);
		color = 31 * color + (int) (v ^ v >>> 32);
		color = 31 * color + (int) (a ^ a >>> 32);

		int hash = start;
		hash = 31 * start + end;
		hash = 31 * end + color;
		return hash;
	}

	public void drawBeam() {
		RenderLaserUtil.renderLaser(color, initLoc, finalLoc);
	}
}
