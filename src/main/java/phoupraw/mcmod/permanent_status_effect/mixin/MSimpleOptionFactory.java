package phoupraw.mcmod.permanent_status_effect.mixin;

import dev.isxander.yacl3.api.OptionFlag;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.autogen.OptionAccess;
import dev.isxander.yacl3.config.v2.api.autogen.OptionFactory;
import dev.isxander.yacl3.config.v2.api.autogen.SimpleOptionFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import phoupraw.mcmod.permanent_status_effect.GameRestart;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

@Mixin(value = SimpleOptionFactory.class,remap = false)
abstract class MSimpleOptionFactory<A extends Annotation, T> implements OptionFactory<A, T> {
    @Inject(method = "flags", at = @At("RETURN"), cancellable = true)
    private void setFlags(A annotation, ConfigField<T> field, OptionAccess storage, CallbackInfoReturnable<Set<OptionFlag>> cir) {
        if (field.access().getAnnotation(GameRestart.class).isPresent()) {
            var set = new HashSet<>(cir.getReturnValue());
            set.add(OptionFlag.GAME_RESTART);
            cir.setReturnValue(set);
        }
    }
}
