package v.akfz.aff;

import v.akfz.aslib.datagen.fabric.mod.FabricModJsonData;
import v.akfz.aslib.datagen.fabric.mod.GenerateFabricModJson;
import v.akfz.aslib.datagen.forge.modstoml.GenerateModsToml;
import v.akfz.aslib.datagen.forge.modstoml.ModsTomlData;
import v.akfz.aslib.datagen.forge.packmcmeta.GeneratePackMcmeta;
import v.akfz.aslib.datagen.forge.packmcmeta.PackMcmetaData;
import v.akfz.aslib.datagen.lang.GenerateLang;
import v.akfz.aslib.datagen.lang.LangData;
import v.akfz.db.annotation.DontCompile;

@DontCompile
public class InitializerDataGen {
    public static void main(String[] args) {
        new GenerateFabricModJson(new FabricModJsonData().mixin("aff.mixins.json").entrypoint("v.akfz.aff.AFFramework_fabric")).run("common");
        new GenerateModsToml(new ModsTomlData()).run("common");
        new GeneratePackMcmeta(new PackMcmetaData()).run("common");

        new GenerateLang().addLangs(
            new LangData("aff","en_us")
                    .add("aff.gun.tooltip.error", "§cGun config not found: %s")
                    .add("aff.magazine.full", "§cMagazine is full!")
                    .add("aff.magazine.incompatible", "§cIncompatible ammo!")
                    .add("aff.magazine.tooltip.type", "§7Type: §f%s")
                    .add("aff.ammo.tooltip.caliber", "§7Caliber: §f%smm")
                    .add("aff.ammo.tooltip.velocity", "§7Velocity: §f%s m/s")
                    .add("aff.ammo.tooltip.mass", "§7Mass: §f%sg")
                    .add("aff.ammo.tooltip.damage", "§7Damage mod: §f×%s")
                    .add("aff.ammo.tooltip.penetration", "§7Penetration mod: §f×%s")
                    .add("aff.magazine.empty","§cMagazine is empty!"),
            new LangData("aff", "ru_ru")
                    .add("aff.magazine.full", "§cМагазин полон!")
                    .add("aff.magazine.incompatible", "§cНе подходящий патрон")
                    .add("aff.magazine.tooltip.type", "§7Тип патрона: §f%s")
                    .add("aff.gun.tooltip.error", "§cНевозможно найти конфиг оружия %s")
                    .add("aff.ammo.tooltip.caliber", "§7Калибр: §f%sмм")
                    .add("aff.ammo.tooltip.velocity", "§7Скорость: §f%s м/с")
                    .add("aff.ammo.tooltip.mass", "§7Масса: §f%sг")
                    .add("aff.ammo.tooltip.damage", "§7Мод. урона: §f×%s")
                    .add("aff.ammo.tooltip.penetration", "§7Мод. пробития: §f×%s")
                    .add("aff.magazine.empty","§cМагазин пуст")
        ).run("common");
    }
}
