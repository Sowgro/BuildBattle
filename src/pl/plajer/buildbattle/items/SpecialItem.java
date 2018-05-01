package pl.plajer.buildbattle.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.plajer.buildbattle.handlers.ChatManager;
import pl.plajer.buildbattle.handlers.ConfigurationManager;
import pl.plajer.buildbattle.utils.ParticleEffect;
import pl.plajer.buildbattle.utils.Util;

import java.util.List;

/**
 * Created by Tom on 5/02/2016.
 */
public class SpecialItem {


    private Material material;
    private Byte data = null;
    private String[] lore;
    private String displayName;
    private ParticleEffect effect;
    private String permission;
    private boolean enabled = true;
    private Location location;
    private int slot;
    private String name;

    private SpecialItem(String name) {
        this.name = name;
    }

    public static void loadAll() {
        new SpecialItem("Leave").load();
    }

    private void load() {
        FileConfiguration config = ConfigurationManager.getConfig("SpecialItems");
        SpecialItem particleItem = new SpecialItem(name);
        particleItem.setData(config.getInt(name + ".data"));
        particleItem.setEnabled(config.getBoolean(name + ".enabled"));
        particleItem.setMaterial(org.bukkit.Material.getMaterial(config.getInt(name + ".material")));
        particleItem.setLore(config.getStringList(name + ".lore"));
        particleItem.setDisplayName(config.getString(name + ".displayname"));
        particleItem.setPermission(config.getString(name + ".permission"));
        particleItem.setSlot(config.getInt(name + ".slot"));
        SpecialItemManager.addEntityItem(name, particleItem);

    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getPermission() {
        return permission;
    }

    private void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setData(Byte data) {
        this.data = data;
    }

    public Material getMaterial() {
        return material;
    }

    private void setMaterial(Material material) {
        this.material = material;
    }

    private byte getData() {
        return data;
    }

    private void setData(Integer data) {
        this.data = data.byteValue();
    }

    public String[] getLore() {
        return lore;
    }

    private void setLore(List<String> lore) {

        this.lore = lore.toArray(new String[lore.size()]);
    }

    public void setLore(String[] lore) {
        this.lore = lore;
    }

    private String getDisplayName() {
        return ChatManager.formatMessage(displayName);
    }

    private void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ParticleEffect getEffect() {
        return effect;
    }

    public void setEffect(ParticleEffect effect) {
        this.effect = effect;
    }

    public int getSlot() {
        return slot;
    }

    private void setSlot(int slot) {
        this.slot = slot;
    }

    public ItemStack getItemStack() {
        ItemStack itemStack;
        if(data != null) {
            itemStack = new ItemStack(getMaterial(), 1, getData());
        } else {
            itemStack = new ItemStack(getMaterial());

        }
        Util.setItemNameAndLore(itemStack, ChatManager.formatMessage(this.getDisplayName()), lore);
        return itemStack;
    }


}
