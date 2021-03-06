package com.teamwizardry.refraction.common.item;

import com.teamwizardry.librarianlib.features.base.item.IItemColorProvider;
import com.teamwizardry.librarianlib.features.base.item.ItemMod;
import com.teamwizardry.librarianlib.features.helpers.ItemNBTHelper;
import com.teamwizardry.librarianlib.features.utilities.client.ColorUtils;
import com.teamwizardry.refraction.common.block.BlockFilter;
import com.teamwizardry.refraction.common.entity.EntityGrenade;
import com.teamwizardry.refraction.init.ModAchievements;
import kotlin.jvm.functions.Function2;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

import static net.minecraft.item.ItemBow.getArrowVelocity;

/**
 * Created by LordSaad.
 */
public class ItemGrenade extends ItemMod implements IItemColorProvider {

	public ItemGrenade() {
		super("grenade");
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft) {
		if (ItemNBTHelper.getInt(stack, "color", -1) == -1) return;
		if (entityLiving instanceof EntityPlayer) {
			EntityPlayer entityplayer = (EntityPlayer) entityLiving;
			boolean shouldRemoveStack = entityplayer.capabilities.isCreativeMode || EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack) > 0;

			int i = this.getMaxItemUseDuration(stack) - timeLeft;

			if (!stack.isEmpty() || shouldRemoveStack) {

				float f = getArrowVelocity(i);
				if ((double) f >= 0.5D) {
					boolean isCreativeMode = entityplayer.capabilities.isCreativeMode;

					if (!world.isRemote) {
						Color color = new Color(ItemNBTHelper.getInt(stack, "color", 0xFFFFFF), true);
						EntityGrenade entityGrenade = new EntityGrenade(world, color, entityplayer);
						entityGrenade.setPosition(entityplayer.posX, entityplayer.posY + entityplayer.eyeHeight, entityplayer.posZ);
						entityGrenade.setHeadingFromThrower(entityplayer, entityplayer.rotationPitch, entityplayer.rotationYaw, 0.0f, 1.5f, 1.0f);
						stack.damageItem(1, entityplayer);
						world.spawnEntity(entityGrenade);
						entityGrenade.velocityChanged = true;
					}

					world.playSound(null, entityplayer.posX, entityplayer.posY, entityplayer.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

					if (!isCreativeMode) {
						stack.setCount(stack.getCount() - 1);
						if (stack.isEmpty()) entityplayer.inventory.deleteStack(stack);
					}
				}
			}
		}
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, @Nonnull EnumHand hand) {
		ItemStack stack = playerIn.getHeldItem(hand);
		if (worldIn.isRemote && (Minecraft.getMinecraft().currentScreen != null))
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		else {
			playerIn.setActiveHand(hand);
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
	}

	@Nonnull
	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		if (ItemNBTHelper.getInt(stack, "color", -1) == -1) return EnumAction.NONE;
		return EnumAction.BOW;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 72000;
	}

	private Color getColor(ItemStack stack) {
		Color c = new Color(ItemNBTHelper.getInt(stack, "color", 0xFFFFFF));
		float[] comps = c.getRGBComponents(null);
		c = new Color(
				(float) Math.max(comps[0] - 0.12, 0),
				(float) Math.max(comps[1] - 0.12, 0),
				(float) Math.max(comps[2] - 0.12, 0));
		return ColorUtils.pulseColor(c);
	}

	@Override
	public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
		playerIn.addStat(ModAchievements.GRENADE);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item itemIn, CreativeTabs tab, NonNullList<ItemStack> subItems) {
		super.getSubItems(itemIn, tab, subItems);
		for (BlockFilter.EnumFilterType type : BlockFilter.EnumFilterType.values()) {
			ItemStack stack = new ItemStack(itemIn);
			ItemNBTHelper.setInt(stack, "color", type.color | 0xFF000000);
			subItems.add(stack);
		}
	}

	@Nullable
	@Override
	public Function2<ItemStack, Integer, Integer> getItemColorFunction() {
		return (stack, tintIndex) -> (tintIndex == 1 ? getColor(stack).getRGB() : 0xFFFFFF);
	}
}
