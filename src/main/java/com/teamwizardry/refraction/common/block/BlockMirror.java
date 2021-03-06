package com.teamwizardry.refraction.common.block;

import com.teamwizardry.librarianlib.features.base.block.BlockModContainer;
import com.teamwizardry.librarianlib.features.math.Matrix4;
import com.teamwizardry.librarianlib.features.utilities.client.TooltipHelper;
import com.teamwizardry.refraction.api.Constants;
import com.teamwizardry.refraction.api.IPrecision;
import com.teamwizardry.refraction.api.beam.Beam;
import com.teamwizardry.refraction.api.beam.ILightSink;
import com.teamwizardry.refraction.api.raytrace.ILaserTrace;
import com.teamwizardry.refraction.client.render.RenderMirror;
import com.teamwizardry.refraction.common.item.ItemScrewDriver;
import com.teamwizardry.refraction.common.tile.TileMirror;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created by LordSaad44
 */
public class BlockMirror extends BlockModContainer implements ILaserTrace, IPrecision, ILightSink {

	public BlockMirror() {
		super("mirror", Material.IRON);
		setHardness(1F);
		setSoundType(SoundType.METAL);
	}

	@SideOnly(Side.CLIENT)
	public void initModel() {
		ClientRegistry.bindTileEntitySpecialRenderer(TileMirror.class, new RenderMirror());
	}

	private TileMirror getTE(World world, BlockPos pos) {
		return (TileMirror) world.getTileEntity(pos);
	}

	@Override
	public boolean handleBeam(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Beam beam) {
		getTE(world, pos).handle(beam);
		return true;
	}

	@Override
	public float getRotX(World worldIn, BlockPos pos) {
		return getTE(worldIn, pos).getRotX();
	}

	@Override
	public void setRotX(World worldIn, BlockPos pos, float x) {
		getTE(worldIn, pos).setRotX(x);
	}

	@Override
	public float getRotY(World worldIn, BlockPos pos) {
		return getTE(worldIn, pos).getRotY();
	}

	@Override
	public void setRotY(World worldIn, BlockPos pos, float y) {
		getTE(worldIn, pos).setRotY(y);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		TooltipHelper.addToTooltip(tooltip, "simple_name." + Constants.MOD_ID + ":" + getRegistryName().getResourcePath());
	}

	@Override
	public void onNeighborChange(IBlockAccess worldIn, BlockPos pos, BlockPos neighbor) {
		TileMirror mirror = getTE((World) worldIn, pos);
		if (mirror == null) return;

		if (mirror.isPowered()) {
			if (!((World) worldIn).isBlockPowered(pos) || ((World) worldIn).isBlockIndirectlyGettingPowered(pos) == 0) {
				mirror.setPowered(false);
			}
		} else {
			if (((World) worldIn).isBlockPowered(pos) || ((World) worldIn).isBlockIndirectlyGettingPowered(pos) > 0)
				mirror.setPowered(true);
		}
	}

	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		if (!worldIn.isRemote) {
			TileMirror mirror = getTE(worldIn, pos);
			if (mirror == null) return;
			if (mirror.isPowered() && !worldIn.isBlockPowered(pos)) {
				mirror.setPowered(false);
			}
		}
	}

	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
		return layer == BlockRenderLayer.CUTOUT;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState blockState) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public RayTraceResult collisionRayTraceLaser(@Nonnull IBlockState blockState, @Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull Vec3d startRaw, @Nonnull Vec3d endRaw) {
		double p = 1.0 / 16.0;

		AxisAlignedBB aabb = new AxisAlignedBB(p, 0, p, 1 - p, p, 1 - p).offset(-0.5, -p / 2, -0.5);


		RayTraceResult superResult = super.collisionRayTrace(blockState, worldIn, pos, startRaw, endRaw);

		TileMirror tile = (TileMirror) worldIn.getTileEntity(pos);
		if (tile == null) return null;

		Vec3d start = startRaw.subtract((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
		Vec3d end = endRaw.subtract((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());

		start = start.subtract(0.5, 0.5, 0.5);
		end = end.subtract(0.5, 0.5, 0.5);

		Matrix4 matrix = new Matrix4();
		matrix.rotate(-Math.toRadians(tile.getRotX()), new Vec3d(1, 0, 0));
		matrix.rotate(-Math.toRadians(tile.getRotY()), new Vec3d(0, 1, 0));

		Matrix4 inverse = new Matrix4();
		inverse.rotate(Math.toRadians(tile.getRotY()), new Vec3d(0, 1, 0));
		inverse.rotate(Math.toRadians(tile.getRotX()), new Vec3d(1, 0, 0));

		start = matrix.apply(start);
		end = matrix.apply(end);
		RayTraceResult result = aabb.calculateIntercept(start, end);
		if (result == null)
			return null;
		Vec3d a = result.hitVec;

		a = inverse.apply(a);
		a = a.addVector(0.5, 0.5, 0.5);


		return new RayTraceResult(a.add(new Vec3d(pos)), superResult == null ? EnumFacing.UP : superResult.sideHit, pos);
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		EnumFacing facing = EnumFacing.getFacingFromVector((float) placer.getLook(0).xCoord, (float) placer.getLook(0).yCoord, (float) placer.getLook(0).zCoord);
		TileMirror mirror = getTE(worldIn, pos);

		float x = 0, y = 0;

		switch (facing) {
			case WEST: {
				x = -90;
				y = 270;
				break;
			}
			case EAST: {
				x = 270;
				y = 90;
				break;
			}
			case NORTH: {
				x = 90;
				y = 0;
				break;
			}
			case SOUTH: {
				x = 90;
				y = 180;
				break;
			}
		}

		mirror.rotDestX = x;
		mirror.rotDestY = y;
		mirror.rotPrevX = x;
		mirror.rotPrevY = y;
		mirror.rotXPowered = x;
		mirror.rotYPowered = y;
		mirror.rotXUnpowered = x;
		mirror.rotYUnpowered = y;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState iBlockState) {
		return new TileMirror();
	}

	@Override
	public boolean isToolEffective(String type, IBlockState state) {
		return super.isToolEffective(type, state) || Objects.equals(type, ItemScrewDriver.SCREWDRIVER_TOOL_CLASS);
	}
}
