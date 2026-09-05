package v.akfz.aff.physics.cpp;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class NativeBallisticsLoader {
	private static boolean loaded;

	static {
		try {
			loadNativeLibrary();
			loaded = true;
		} catch (Exception e) {
			System.err.println("[AFF_NATIVE] Native library 'aff_physics' not found. Falling back to Java physics.");
			loaded = false;
		}
	}

	private static void loadNativeLibrary() throws Exception {
		String os = System.getProperty("os.name").toLowerCase();
		String ext;
		String prefix = "";

		if (os.contains("win")) {
			ext = "dll";
		} else if (os.contains("mac")) {
			ext = "dylib";
			prefix = "lib";
		} else {
			ext = "so";
			prefix = "lib";
		}

		String fileName = prefix + "aff_physics." + ext;
		String resourcePath = "/native/" + fileName;

		InputStream in = NativeBallisticsLoader.class.getResourceAsStream(resourcePath);
		if (in == null) {
			throw new RuntimeException("Native library resource not found: " + resourcePath);
		}

		File tempFile = File.createTempFile("aff_physics_", "." + ext);
		tempFile.deleteOnExit();

		Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		in.close();

		System.load(tempFile.getAbsolutePath());

		loaded = true;
	}

	public static boolean isLoaded() {
		return loaded;
	}
}