package v.akfz.aff.test;

import net.minecraft.world.item.Item;
import v.akfz.aff.gun.registry.AmmoRegistry;
import v.akfz.aff.gun.registry.GunRegistry;
import v.akfz.aff.gun.registry.MagazineRegistry;
import v.akfz.aff.item.AmmoItemInstance;
import v.akfz.aff.item.MagazineItemInstance;
import v.akfz.aslib.annotation.RegisterModule;
import v.akfz.aslib.initializer.generator.GenerateRegistries;

@GenerateRegistries(modId = "test")
public class TestRegistries {

	// ===== ПАТРОНЫ =====

	@RegisterModule(id = "test:ammo_762x39")
	public static final Item AMMO_762X39 = new AmmoItemInstance(
			new Item.Properties().stacksTo(70),
			TestData.AMMO_762X39
	);

	@RegisterModule(id = "test:ammo_762x54r")
	public static final Item AMMO_762X54R = new AmmoItemInstance(
			new Item.Properties().stacksTo(70),
			TestData.AMMO_762X54R
	);

	@RegisterModule(id = "test:ammo_12gauge")
	public static final Item AMMO_12GAUGE = new AmmoItemInstance(
			new Item.Properties().stacksTo(16),
			TestData.AMMO_12GAUGE
	);

	@RegisterModule(id = "test:ammo_9x18")
	public static final Item AMMO_9X18 = new AmmoItemInstance(
			new Item.Properties().stacksTo(70),
			TestData.AMMO_9X18
	);

	@RegisterModule(id = "test:ammo_556x45")
	public static final Item AMMO_556X45 = new AmmoItemInstance(
			new Item.Properties().stacksTo(70),
			TestData.AMMO_556X45
	);

	@RegisterModule(id = "test:ammo_762x25")
	public static final Item AMMO_762X25 = new AmmoItemInstance(
			new Item.Properties().stacksTo(70),
			TestData.AMMO_762X25
	);

	// ===== МАГАЗИНЫ =====

	@RegisterModule(id = "test:ak_magazine")
	public static final Item AK_MAGAZINE = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.AK_MAGAZINE
	);

	@RegisterModule(id = "test:ak_drum_magazine")
	public static final Item AK_DRUM_MAGAZINE = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.AK_DRUM_MAGAZINE
	);

	@RegisterModule(id = "test:mosin_clip")
	public static final Item MOSIN_CLIP = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.MOSIN_CLIP
	);

	@RegisterModule(id = "test:makarov_mag")
	public static final Item MAKAROV_MAG = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.MAKAROV_MAG
	);

	@RegisterModule(id = "test:m16_mag")
	public static final Item M16_MAG = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.M16_MAG
	);

	@RegisterModule(id = "test:pkm_belt")
	public static final Item PKM_BELT = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.PKM_BELT
	);

	@RegisterModule(id = "test:ppsh_drum")
	public static final Item PPSH_DRUM = new MagazineItemInstance(
			new Item.Properties().stacksTo(1),
			TestData.PPSH_DRUM
	);

	// ===== ОРУЖИЕ =====

	@RegisterModule(id = "test:test_ak47")
	public static final TestGunItem TEST_AK47 = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_ak47"
	);

	@RegisterModule(id = "test:test_mosin")
	public static final TestGunItem TEST_MOSIN = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_mosin"
	);

	@RegisterModule(id = "test:test_shotgun")
	public static final TestGunItem TEST_SHOTGUN = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_shotgun"
	);

	@RegisterModule(id = "test:test_makarov")
	public static final TestGunItem TEST_MAKAROV = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_makarov"
	);

	@RegisterModule(id = "test:test_m16")
	public static final TestGunItem TEST_M16 = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_m16"
	);

	@RegisterModule(id = "test:test_pkm")
	public static final TestGunItem TEST_PKM = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_pkm"
	);

	@RegisterModule(id = "test:test_ppsh")
	public static final TestGunItem TEST_PPSH = new TestGunItem(
			new Item.Properties().stacksTo(1),
			"test:test_ppsh"
	);

	// ===== РЕГИСТРАЦИЯ =====

	static {
		// Патроны
		AmmoRegistry.register(AMMO_762X39);
		AmmoRegistry.register(AMMO_762X54R);
		AmmoRegistry.register(AMMO_12GAUGE);
		AmmoRegistry.register(AMMO_9X18);
		AmmoRegistry.register(AMMO_556X45);
		AmmoRegistry.register(AMMO_762X25);

		// Магазины
		MagazineRegistry.register(AK_MAGAZINE);
		MagazineRegistry.register(AK_DRUM_MAGAZINE);
		MagazineRegistry.register(MOSIN_CLIP);
		MagazineRegistry.register(MAKAROV_MAG);
		MagazineRegistry.register(M16_MAG);
		MagazineRegistry.register(PKM_BELT);
		MagazineRegistry.register(PPSH_DRUM);

		// Оружие
		GunRegistry.registerDefault(TestData.TEST_AK47);
		GunRegistry.registerDefault(TestData.TEST_MOSIN);
		GunRegistry.registerDefault(TestData.TEST_SHOTGUN);
		GunRegistry.registerDefault(TestData.TEST_MAKAROV);
		GunRegistry.registerDefault(TestData.TEST_M16);
		GunRegistry.registerDefault(TestData.TEST_PKM);
		GunRegistry.registerDefault(TestData.TEST_PPSH);
	}
}