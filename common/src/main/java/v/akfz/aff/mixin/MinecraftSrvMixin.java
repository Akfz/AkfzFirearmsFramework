package v.akfz.aff.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.aff.ServerContext;
import v.akfz.aff.bullet.BulletManager;

@Mixin(MinecraftServer.class)
public class MinecraftSrvMixin {
	@Inject(method = "halt", at = @At("TAIL"))
	private void aslib$onShutdown(CallbackInfo ci) {
		BulletManager.getInstance().shutdown();
		ServerContext.set(null);
	}

	@Inject(method = "loadLevel", at = @At("TAIL"))
	private void aslib$onStart(CallbackInfo ci) {
		MinecraftServer server = (MinecraftServer) (Object) this;
		ServerContext.set(server);
		BulletManager.getInstance().start(server);
	}
}