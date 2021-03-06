package com.teamwizardry.refraction.api.book;

import com.google.gson.JsonElement;
import com.teamwizardry.librarianlib.features.gui.components.ComponentStack;
import com.teamwizardry.librarianlib.features.gui.components.ComponentVoid;
import com.teamwizardry.refraction.api.ConfigValues;
import com.teamwizardry.refraction.api.Utils;
import com.teamwizardry.refraction.client.gui.RightSidebar;
import com.teamwizardry.refraction.client.gui.tablet.ExtraSidebar;
import com.teamwizardry.refraction.client.gui.tablet.SubPage;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author LordSaad
 *         <p>
 *         This class will convert a JsonArray object into a formatted text component
 */
public class TextAdapter {

	public static int wrapLength = 200;
	public ArrayList<ExtraSidebar> extraSidebars = new ArrayList<>();
	private SubPage subPage;
	private int id;
	private JsonElement object;
	private Set<Item> items = new HashSet<>();

	public TextAdapter(@Nonnull SubPage subPage, int id, @Nonnull JsonElement object) {
		this.subPage = subPage;
		this.id = id;
		this.object = object;
	}

	public String convertLinesToString() {
		String text = "";
		for (JsonElement element : object.getAsJsonArray())
			if (element.isJsonPrimitive() && !element.getAsString().isEmpty()) {
				String s = element.getAsString();
				s = s.replace("[player]", Minecraft.getMinecraft().player.getDisplayNameString());
				s = s.replace("&", "§");
				TextComponentString componentString = new TextComponentString(s);
				s = componentString.getFormattedText();

				for (String word : s.split(" ")) {
					if (word.startsWith("[") && word.contains("]")) {
						if (word.contains("recipe:")) {
							String stackString = word.substring(word.indexOf("recipe:"), word.indexOf("]")).split("recipe:")[1];
							ItemStack stack = Utils.getStackFromString(stackString);
							if (stack != null) {
								s = s.replace(word, TextFormatting.RESET + "[" + TextFormatting.DARK_BLUE + stack.getDisplayName() + TextFormatting.RESET + "]");

								if (!items.contains(stack.getItem()) && Loader.isModLoaded("JEI")) {
									items.add(stack.getItem());
									ExtraSidebar extraSidebar = new ExtraSidebar(subPage, id++, null, ExtraSidebar.SidebarType.RECIPE);
									extraSidebar.title = stack.getDisplayName().length() > 17 ? stack.getDisplayName().substring(0, 17) + "..." : stack.getDisplayName();
									extraSidebar.contentComp = new ComponentVoid(0, 0);
									extraSidebar.slotcomp = new ComponentStack(RightSidebar.rightExtended.getWidth() - 16 - 8, 1);
									extraSidebar.slotcomp.getStack().setValue(stack);
									extraSidebars.add(extraSidebar.init());
								}
							}
						} else if (word.contains("config:")) {
							String configOption = word.substring(word.indexOf("config:"), word.indexOf("]")).split("config:")[1];
							String option = "";
							if (configOption.equals("solar_strength")) {
								option = ConfigValues.SOLAR_ALPHA + "";
							} else if (configOption.equals("glowstone_strength")) {
								option = ConfigValues.GLOWSTONE_ALPHA + "";
							}
							s = s.replace(word, TextFormatting.RESET + "[" + TextFormatting.DARK_BLUE + option + TextFormatting.RESET + "]");
						}
					}
				}
				text += s;
			} else text += "\n";
		return text;
	}
}
