package phoupraw.mcmod.permanent_status_effect.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import phoupraw.mcmod.permanent_status_effect.PSE;

import java.util.function.Consumer;

@Mixin(SuspiciousStewItem.class)
abstract class MSuspiciousStewItem extends Item {
    @Shadow
    private static void forEachEffect(ItemStack stew, Consumer<StatusEffectInstance> effectConsumer) {
    }
    public MSuspiciousStewItem(Settings settings) {
        super(settings);
    }
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;<init>(Lnet/minecraft/item/Item$Settings;)V"))
    private static Settings setMaxCount(Settings settings) {
        //if (!PSE.CONFIG.instance().enable) {return settings;}
        return settings.maxCount(PSE.CONFIG.instance().stewMaxStack);
    }
    /**
     @author Phoupraw
     @reason
     */
    @Overwrite
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack itemStack = super.finishUsing(stack.copy(), world, user);
        forEachEffect(stack, user::addStatusEffect);
        if (!(user instanceof PlayerEntity player)) {
            return itemStack;
        }
        return ItemUsage.exchangeStack(stack, player, Items.BOWL.getDefaultStack());
    }
    //@Inject(method = "finishUsing",at = @At("RETURN"),cancellable = true)
    //private void consumeStack(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
    //
    //}
}
