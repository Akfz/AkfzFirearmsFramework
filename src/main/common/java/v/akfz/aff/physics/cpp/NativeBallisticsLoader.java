package v.akfz.aff.physics.cpp;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class NativeBallisticsLoader {

	private static boolean loaded = false;
	private static String lastError = null;

	private NativeBallisticsLoader() {}

	public static boolean load() {
		if (loaded) return true;
		try {
			loadLibrary();
			loaded = true;
			return true;
		} catch (Exception e) {
			lastError = e.getMessage();
			System.err.println("[JNIHELPER] Failed to load native library ' aff_physics ': " + lastError);
			return false;
		}
	}

	public static boolean isLoaded() { return loaded; }
	public static String getLastError() { return lastError; }

	private static void loadLibrary() throws Exception {
		String osName = System.getProperty("os.name").toLowerCase();
		String archName = System.getProperty("os.arch").toLowerCase();

		String osId = detectOsId(osName);
		String archId = detectArchId(archName);
		String ext = getExtension(osId);
		String prefix = getPrefix(osId);

		String fileName = prefix + "aff_physics" + "." + ext;
		String resourcePath = "/native/" + osId + "-" + archId + "/" + fileName;

		InputStream in = NativeBallisticsLoader.class.getResourceAsStream(resourcePath);
		if (in == null) {
			throw new RuntimeException(
					"Native library resource not found: " + resourcePath +
							" (detected platform: " + osId + "-" + archId + ")"
			);
		}

		File tempFile = File.createTempFile("aff_physics" + "_", "." + ext);
		tempFile.deleteOnExit();
		Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		in.close();

		tempFile.setExecutable(true);
		System.load(tempFile.getAbsolutePath());
	}

	private static String detectOsId(String osName) {
		if (osName.contains("win")) return "windows";
		if (osName.contains("mac") || osName.contains("darwin")) return "macos";
		if (osName.contains("freebsd")) return "freebsd";
		if (osName.contains("nux") || osName.contains("nix")) {
			return Files.exists(Paths.get("/etc/alpine-release")) ? "linux-musl" : "linux";
		}
		throw new UnsupportedOperationException("Unsupported OS: " + osName);
	}

	private static String detectArchId(String arch) {
		return switch (arch) {
			case "amd64","x86_64" -> "x86_64";
			case "aarch64","arm64" -> "arm64";
			case "x86","i386","i486","i586","i686" -> "x86";
			case "riscv64" -> "riscv64";
			case "ppc64le" -> "ppc64le";
			default -> throw new UnsupportedOperationException("Unsupported architecture: " + arch);
		};
	}

	private static String getExtension(String osId) {
		return switch (osId) {
			case "windows" -> "dll";
			case "macos" -> "dylib";
			default -> "so";
		};
	}

	private static String getPrefix(String osId) {
		return "windows".equals(osId) ? "" : "lib";
	}
}