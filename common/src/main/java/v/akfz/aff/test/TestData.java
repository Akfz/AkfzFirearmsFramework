package v.akfz.aff.test;

import v.akfz.aff.bullet.BulletConfig;
import v.akfz.aff.data.gun.GunData;
import v.akfz.aff.data.magazine.MagazineData;
import v.akfz.aff.data.magazine.MagazineSetting;
import v.akfz.aff.gun.FireMode;
import v.akfz.aff.gun.ammo.AmmoType;

import java.util.List;

public class TestData {

	// ==================== ОРУЖИЕ ====================

	// Автомат: AUTO + SINGLE, магазинная система
	public static final GunData TEST_AK47 = new GunData(
			"test:test_ak47",
			"item.test.test_ak47",
			List.of("test:ak_magazine", "test:ak_drum_magazine"),
			List.of(),
			-1,
			600,
			true,
			2.0,
			0.5,
			-1,
			new FireMode[]{FireMode.AUTO, FireMode.SINGLE},
			-1,
			MagazineSetting.ALLOWBOTH
	);

	// Снайперская винтовка: BOLT, магазин 5, высокий урон
	public static final GunData TEST_MOSIN = new GunData(
			"test:test_mosin",
			"item.test.test_mosin",
			List.of("test:mosin_clip"),
			List.of(),
			-1,
			40,                      // очень низкий темп (болт)
			true,
			4.5,                     // сильная отдача
			0.2,
			-1,
			new FireMode[]{FireMode.BOLT},
			-1,
			MagazineSetting.MAGAZINE
	);

	// Двустволка: BREAK, без магазина (прямая зарядка)
	public static final GunData TEST_SHOTGUN = new GunData(
			"test:test_shotgun",
			"item.test.test_shotgun",
			List.of(),
			List.of("test:12gauge"),
			2,                       // 2 ствола = вместимость
			80,
			false,                   // нет патронника, зарядка напрямую
			3.5,
			1.5,
			-1,
			new FireMode[]{FireMode.BOLT},
			-1,
			MagazineSetting.AMMO
	);

	// Пистолет: SINGLE, магазин 8
	public static final GunData TEST_MAKAROV = new GunData(
			"test:test_makarov",
			"item.test.test_makarov",
			List.of("test:makarov_mag"),
			List.of(),
			-1,
			300,
			true,
			1.0,
			0.4,
			-1,
			new FireMode[]{FireMode.SINGLE},
			-1,
			MagazineSetting.MAGAZINE
	);

	// Штурмовая винтовка: BURST (3) + SINGLE + AUTO
	public static final GunData TEST_M16 = new GunData(
			"test:test_m16",
			"item.test.test_m16",
			List.of("test:m16_mag"),
			List.of(),
			-1,
			800,
			true,
			1.5,
			0.3,
			-1,
			new FireMode[]{FireMode.BURST, FireMode.SINGLE, FireMode.AUTO},
			3,                       // очередь из 3
			MagazineSetting.MAGAZINE
	);

	// Пулемёт: AUTO, лента 100
	public static final GunData TEST_PKM = new GunData(
			"test:test_pkm",
			"item.test.test_pkm",
			List.of("test:pkm_belt"),
			List.of(),
			-1,
			650,
			true,
			3.0,
			1.0,
			-1,
			new FireMode[]{FireMode.AUTO},
			-1,
			MagazineSetting.MAGAZINE
	);

	// Пистолет-пулемёт: AUTO, дисковый магазин 71, высокий темп
	public static final GunData TEST_PPSH = new GunData(
			"test:test_ppsh",
			"item.test.test_ppsh",
			List.of("test:ppsh_drum"),
			List.of(),
			-1,
			1000,                    // высокий темп
			true,
			1.8,
			0.8,
			-1,
			new FireMode[]{FireMode.AUTO, FireMode.SINGLE},
			-1,
			MagazineSetting.MAGAZINE
	);

	// ==================== ПАТРОНЫ ====================

	// 7.62×39 для АК
	public static final AmmoType AMMO_762X39 = new AmmoType(
			"test:762x39",
			"item.test.ammo.762x39",
			new BulletConfig(
					715.0,
					0.0079,
					0.002,
					0.3,
					120,
					14.0,
					false,
					0.004f,
					0.0076
			),
			1f, 1f, 1f
	);

	// 7.62×54R для Мосины (высокий урон, высокая скорость)
	public static final AmmoType AMMO_762X54R = new AmmoType(
			"test:762x54r",
			"item.test.ammo.762x54r",
			new BulletConfig(
					865.0,
					0.0096,
					0.0018,
					0.3,
					150,
					20.0,
					false,
					0.006f,
					0.0076
			),
			1f, 1f, 1f
	);

	// 12 калибр для дробовика (тяжёлый, медленный)
	public static final AmmoType AMMO_12GAUGE = new AmmoType(
			"test:12gauge",
			"item.test.ammo.12gauge",
			new BulletConfig(
					400.0,
					0.033,
					0.005,
					0.4,
					60,
					6.0,
					false,
					0.004f,
					0.0185           // 18.5 мм
			),
			1f, 1f, 1f
	);

	// 9×18 для Макарова
	public static final AmmoType AMMO_9X18 = new AmmoType(
			"test:9x18",
			"item.test.ammo.9x18",
			new BulletConfig(
					315.0,
					0.0061,
					0.003,
					0.35,
					80,
					8.0,
					false,
					0.015f,
					0.009
			),
			1f, 1f, 1f
	);

	// 5.56×45 для M16
	public static final AmmoType AMMO_556X45 = new AmmoType(
			"test:556x45",
			"item.test.ammo.556x45",
			new BulletConfig(
					975.0,
					0.004,
					0.0017,
					0.3,
					150,
					12.0,
					false,
					0.0035f,
					0.00556
			),
			1f, 1f, 1f
	);

	// 7.62×25 для ППШ
	public static final AmmoType AMMO_762X25 = new AmmoType(
			"test:762x25",
			"item.test.ammo.762x25",
			new BulletConfig(
					500.0,
					0.0055,
					0.0025,
					0.35,
					100,
					9.0,
					false,
					0.006f,
					0.00762
			),
			1f, 1f, 1f
	);

	// ==================== МАГАЗИНЫ ====================

	public static final MagazineData AK_MAGAZINE = new MagazineData(
			"test:ak_magazine",
			"item.test.ak_magazine",
			30,
			List.of("test:762x39")
	);

	public static final MagazineData AK_DRUM_MAGAZINE = new MagazineData(
			"test:ak_drum_magazine",
			"item.test.ak_drum_magazine",
			75,
			List.of("test:762x39")
	);

	// Снайперская обойма на 5
	public static final MagazineData MOSIN_CLIP = new MagazineData(
			"test:mosin_clip",
			"item.test.mosin_clip",
			5,
			List.of("test:762x54r")
	);

	// Пистолетный магазин на 8
	public static final MagazineData MAKAROV_MAG = new MagazineData(
			"test:makarov_mag",
			"item.test.makarov_mag",
			8,
			List.of("test:9x18")
	);

	// Магазин М16 на 30
	public static final MagazineData M16_MAG = new MagazineData(
			"test:m16_mag",
			"item.test.m16_mag",
			30,
			List.of("test:556x45")
	);

	// Лента пулемёта на 100
	public static final MagazineData PKM_BELT = new MagazineData(
			"test:pkm_belt",
			"item.test.pkm_belt",
			100,
			List.of("test:762x39")
	);

	// Дисковый магазин ППШ на 71
	public static final MagazineData PPSH_DRUM = new MagazineData(
			"test:ppsh_drum",
			"item.test.ppsh_drum",
			71,
			List.of("test:762x25")
	);
}