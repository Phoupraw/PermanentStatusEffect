@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package phoupraw.mcmod.permanent_status_effect

import com.google.common.base.Suppliers
import com.google.common.collect.Multimap
import com.tiviacz.travelersbackpack.TravelersBackpack
import com.tiviacz.travelersbackpack.component.ComponentUtils
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.impl.serializer.GsonConfigSerializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.command.argument.RegistryEntryArgumentType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.PotionItem
import net.minecraft.item.SuspiciousStewItem
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.potion.PotionUtil
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import phoupraw.mcmod.linked.minecraft.id
import phoupraw.mcmod.linked.misc.*
import phoupraw.mcmod.linked.transfer.*
import phoupraw.mcmod.linked.transfer.item.NbtSimpleInventory
import phoupraw.mcmod.permanent_status_effect.mixin.AStatusEffectInstance
import zabi.minecraft.extraalchemy.items.ModItems
import zabi.minecraft.extraalchemy.items.PotionBagItem

object PSE : ModInitializer {
    const val ID = "permanent_status_effect"
    private inline fun <reified T> config(suffix: String): ConfigClassHandler<T> {
        return ConfigClassHandler.createBuilder(T::class.java)
          .id(Identifier(ID, "config$suffix"))
          .serializer {
              GsonConfigSerializer.Builder(it)
                .setJson5(true)
                .setPath(Fabric.configDir.resolve("$ID$suffix.cfg.json5"))
                .build()
          }
          .build()
          .apply { load() }
    }
    @JvmField
    val CONFIG = config<PSEConfig>("")
    @JvmField
    val STORAGE = MutableEvent<(entity: LivingEntity, inventory: MutableCollection<ContainerItemContext>) -> Unit> { callbacks ->
        { entity, inventory ->
            for (callback in callbacks) {
                callback(entity, inventory)
            }
        }
    }
    @Deprecated(message = "大改", replaceWith = ReplaceWith(expression = "STORAGE"))
    @JvmField
    val INVENTORIES = MutableEvent<(entity: LivingEntity, storages: Multimap<String, Iterable<StorageView<ItemVariant>>>) -> Unit> { callbacks ->
        { entity, storages ->
            for (callback in callbacks) {
                callback(entity, storages)
            }
        }
    }
    @JvmField
    val EFFECTS = MutableEvent<(context: ContainerItemContext, entity: LivingEntity, stats: DefaultedMap<StatusEffect, DefaultedMap<Int, Double>>) -> Boolean> { callbacks ->
        { context, entity, stats ->
            for (callback in callbacks) {
                callback(context, entity, stats)
            }
            false
        }
    }
    /**
     * 返回`null`表示继续执行回调；返回非`null`表示不继续执行后续回调。一般来说，返回`null`即可。
     */
    @Deprecated(message = "大改", replaceWith = ReplaceWith(expression = "EFFECTS"))
    @JvmField
    val EFFECT_LOOKUP = GenericApiLookup<Item, PSE?, Triple<LivingEntity, ContainerItemContext, DefaultedMap<StatusEffect, DefaultedMap<Int, Double>>>>("$ID:effect")
    fun addEffects(inventory: SlottedStorage<ItemVariant>, entity: LivingEntity, stats: DefaultedMap<StatusEffect, DefaultedMap<Int, Double>>) {
        addEffects(inventory.nonEmptyViews().map { ContainerItemContext(it) },entity,stats)
    }
    fun addEffects(inventory: Iterable<ContainerItemContext>, entity: LivingEntity, stats: DefaultedMap<StatusEffect, DefaultedMap<Int, Double>>) {
        for (slot in inventory) {
            if (slot.empty) continue
            EFFECTS.call(slot, entity, stats)
        }
    }

    override fun onInitialize() {
        STORAGE.register { entity, inventory ->
            if (entity !is PlayerEntity) return@register
            val playerS = PlayerInventoryStorage(entity)
            if (CONFIG.instance().mainInventory) {
                for (view in playerS.nonEmptyViews()) {
                    inventory += RemainderContainerItemContext(view, playerS)
                }
            }
            if (CONFIG.instance().enderChest) {
                val s = InventoryStorage(entity.enderChestInventory)
                for (view in s.nonEmptyViews()) {
                    inventory += RemainderContainerItemContext(view, playerS)
                }
            }
        }
        EFFECTS.register("blacklist") { context, entity, stats ->
            val item = context.itemVariant.item
            val matched = run {
                for (sId in CONFIG.instance().blacklist) {
                    if (!sId.startsWith('#')) {
                        val id = Identifier.tryParse(sId) ?: continue
                        if (Registries.ITEM.get(id) == item) {
                            return@run true
                        } else {
                            continue
                        }
                    }
                    val id = Identifier.tryParse(sId.substring(1)) ?: continue
                    val tagKey = TagKey.of(RegistryKeys.ITEM, id)
                    @Suppress("DEPRECATION")
                    if (item.registryEntry.isIn(tagKey)) return@run true
                }
                false
            }
            CONFIG.instance().whitelist == matched
        }
        EFFECTS.addKeyOrder("blacklist", MutableEvent.DEFAULT_KEY)
        EFFECTS.register { context, entity, stats ->
            if (context.itemVariant.item !is PotionItem) return@register false
            for (instance in PotionUtil.getPotionEffects(context.itemVariant.nbt)) {
                stats[instance.effectType][instance.amplifier] += if (instance.isInfinite) Double.POSITIVE_INFINITY else instance.duration.toDouble() * context.amount
            }
            false
        }
        EFFECTS.register { context, entity, stats ->
            if (context.itemVariant.item !is SuspiciousStewItem) return@register false
            val nbt = context.itemVariant.nbt ?: return@register false
            if (!nbt.contains("Effects", NbtElement.LIST_TYPE.toInt())) return@register false
            val nbtEffects = nbt.getList("Effects", NbtElement.COMPOUND_TYPE.toInt())
            for (nbtEffect in nbtEffects) {
                if (nbtEffect !is NbtCompound) continue
                val effect = StatusEffect.byRawId(nbtEffect.getInt("EffectId")) ?: continue
                val duration = if (nbtEffect.contains("EffectDuration", NbtElement.NUMBER_TYPE.toInt())) nbtEffect.getInt("EffectDuration") else 160
                stats[effect][0] += if (duration == -1) Double.POSITIVE_INFINITY else duration.toDouble() * context.amount
            }
            false
        }
        EFFECTS.register { context, entity, stats ->
            context.itemVariant.item.foodComponent?.apply {
                for (pair in statusEffects) {
                    val instance = pair.first
                    stats[instance.effectType][instance.amplifier] += if (instance.isInfinite) Double.POSITIVE_INFINITY else instance.duration.toDouble() * context.amount * pair.second
                }
            }
            false
        }
        EFFECTS.register{context, entity, stats ->
            if ((context.itemVariant.item as? BlockItem)?.block is ShulkerBoxBlock) {
                addEffects(InventoryStorage(NbtSimpleInventory(context.itemVariant.copyOrCreateNbt())),entity, stats)
            }
            false
        }
        EFFECTS.addKeyOrder(MutableEvent.DEFAULT_KEY, "remove")
        EFFECTS.register("remove") { context, entity, stats ->
            if (!CONFIG.instance().harmful) {
                stats.keys.removeWhen { it.category == StatusEffectCategory.HARMFUL }
            }
            false
        }
        EFFECTS.register("remove") { context, entity, stats ->
            if (!CONFIG.instance().instant) {
                stats.keys.removeWhen { it.isInstant }
            }
            false
        }
        EFFECTS.register("remove") { context, entity, stats ->
            stats.keys.removeWhen { "$ID:disabled/${it.id}" in entity.commandTags }
            false
        }
        //INVENTORIES.register { entity, storages ->
        //    if (entity !is PlayerEntity) return@register
        //    if (CONFIG.instance().mainInventory) {
        //        storages.put("minecraft", PlayerInventoryStorage.of(entity).nonEmptyViews())
        //    }
        //    if (CONFIG.instance().enderChest) {
        //        storages.put("minecraft", InventoryStorage(entity.enderChestInventory))
        //    }
        //}
        //EFFECT_LOOKUP.preliminary.addKeyOrder("blacklist", MutableEvent.DEFAULT_KEY)
        //EFFECT_LOOKUP.preliminary.register("blacklist") { item, _ ->
        //    val matched = run {
        //        for (sId in CONFIG.instance().blacklist) {
        //            if (!sId.startsWith('#')) {
        //                val id = Identifier.tryParse(sId) ?: continue
        //                if (Registries.ITEM.get(id) == item) {
        //                    return@run true
        //                } else {
        //                    continue
        //                }
        //            }
        //            val id = Identifier.tryParse(sId.substring(1)) ?: continue
        //            val tagKey = TagKey.of(RegistryKeys.ITEM, id)
        //            @Suppress("DEPRECATION")
        //            if (item.registryEntry.isIn(tagKey)) return@run true
        //        }
        //        false
        //    }
        //    if (CONFIG.instance().whitelist == matched) null else PSE
        //}
        //EFFECT_LOOKUP.fallback.register { item, (_, context, effects) ->
        //    if (item !is PotionItem) return@register null
        //    for (instance in PotionUtil.getPotionEffects(context.itemVariant.nbt)) {
        //        effects[instance.effectType][instance.amplifier] += if (instance.isInfinite) Double.POSITIVE_INFINITY else instance.duration.toDouble() * context.amount
        //    }
        //    null
        //}
        //EFFECT_LOOKUP.fallback.register { item, (_, context, effects) ->
        //    if (item !is SuspiciousStewItem) return@register null
        //    val nbt = context.itemVariant.nbt
        //    if (nbt == null || !nbt.contains("Effects", NbtElement.LIST_TYPE.toInt())) return@register null
        //    val nbtEffects = nbt.getList("Effects", NbtElement.COMPOUND_TYPE.toInt())
        //    for (nbtEffect in nbtEffects) {
        //        if (nbtEffect !is NbtCompound) continue
        //        val effect = StatusEffect.byRawId(nbtEffect.getInt("EffectId")) ?: continue
        //        val duration = if (nbtEffect.contains("EffectDuration", NbtElement.NUMBER_TYPE.toInt())) nbtEffect.getInt("EffectDuration") else 160
        //        effects[effect][0] += if (duration == -1) Double.POSITIVE_INFINITY else duration.toDouble() * context.amount
        //    }
        //    null
        //}
        //EFFECT_LOOKUP.fallback.register { _, (_, context, effects) ->
        //    context.itemVariant.item.foodComponent?.apply {
        //        for (pair in statusEffects) {
        //            val instance = pair.first
        //            effects[instance.effectType][instance.amplifier] += if (instance.isInfinite) Double.POSITIVE_INFINITY else instance.duration.toDouble() * context.amount * pair.second
        //        }
        //    }
        //    null
        //}
        //EFFECT_LOOKUP.fallback.addKeyOrder(MutableEvent.DEFAULT_KEY, "remove")

        ServerTickEvents.START_WORLD_TICK.register { world ->
            for (entity in world.iterateEntities()) {
                if (entity !is LivingEntity) continue
                val stats: DefaultedMap<StatusEffect, DefaultedMap<Int, Double>> = DefaultedMap.supplier(HashMap()) { DefaultedMap(HashMap(), 0.0) }
                if (CONFIG.instance().enable) {
                    val inventory = mutableListOf<ContainerItemContext>()
                    STORAGE.call(entity, inventory)
                    addEffects(inventory, entity, stats)
                    //val seen: MutableSet<StorageView<ItemVariant>> = mutableSetOf()
                    //val storages = MultimapBuilder.linkedHashKeys().arrayListValues().build<String, Iterable<StorageView<ItemVariant>>>()
                    //INVENTORIES.call(entity, storages)
                    //for (views in storages.values()) {
                    //    for (view in views) {
                    //        if (view in seen || view.resourceBlank) continue
                    //        val resource = view.resource
                    //        val amount = view.simulateExtract()
                    //        if (amount <= 0) continue
                    //        EFFECT_LOOKUP(resource.item, Triple(entity, ContainerItemContext(resource, amount), stats))
                    //        seen += view
                    //    }
                    //}
                    //val queue: Queue<StorageView<ItemVariant>> = storages.values().flatMapTo(ArrayDeque()) { it }
                    //while (queue.isNotEmpty()) {
                    //    val view = queue.poll()
                    //    if (view in seen || view.resourceBlank) continue
                    //    val resource = view.resource
                    //    val amount = view.simulateExtract()
                    //    if (amount <= 0) continue
                    //    EFFECT_LOOKUP(resource.item, Triple(entity, ContainerItemContext(resource, amount), effects))
                    //    seen += view
                    //    ItemStorages.ITEM(resource.item, ContainerItemContext(resource, amount))?.also { queue += it }
                    //}
                }
                val duration = CONFIG.instance().duration.let { if (it < 0) Double.POSITIVE_INFINITY else 20 * 60 * it }
                stats.entries.removeWhen { (_, map) ->
                    map.values.removeWhen { it < duration }
                    map.isEmpty()
                }
                val infs = stats.mapValues { (_, map) -> map.keys.max() }
                for (instance in entity.statusEffects) {
                    instance.run {
                        if (!isInfinite || amplifier == infs[effectType]) return@run
                        val tag = "$ID:active/${effectType.id}"
                        if (tag !in entity.commandTags) return@run
                        (instance as AStatusEffectInstance).setDuration(0)
                        entity.removeScoreboardTag(tag)
                    }
                }
                infs.forEach { (effect, amplifier) ->
                    entity.addStatusEffect(StatusEffectInstance(effect, StatusEffectInstance.INFINITE, amplifier, CONFIG.instance().ambient, CONFIG.instance().showParticles, CONFIG.instance().showIcon), entity)
                    entity.addCommandTag("$ID:active/${effect.id}")
                }
            }
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, _ ->
            dispatcher.register(CommandManager.literal(ID)
              .then(CommandManager.literal("disable")
                .then(CommandManager.argument("effect", RegistryEntryArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT))
                  .executes {
                      val effect = RegistryEntryArgumentType.getStatusEffect(it, "effect").value()
                      val entity = it.source.entity ?: run {
                          it.source.sendError(Text.translatable("permissions.requires.entity"));
                          return@executes 0
                      }
                      if (!entity.addCommandTag("$ID:disabled/${effect.id}")) {
                          it.source.sendError(Text.translatable("command.permanent_status_effect.disable.failure", effect.name))
                          return@executes 0
                      }
                      it.source.sendFeedback(Suppliers.ofInstance(Text.translatable("command.permanent_status_effect.disable.succeed", effect.name)), false)
                      1
                  })
              )
              .then(CommandManager.literal("enable")
                .then(CommandManager.argument("effect", RegistryEntryArgumentType.registryEntry(registryAccess, RegistryKeys.STATUS_EFFECT))
                  .executes {
                      val effect = RegistryEntryArgumentType.getStatusEffect(it, "effect").value()
                      val entity = it.source.entity ?: run {
                          it.source.sendError(Text.translatable("permissions.requires.entity"));
                          return@executes 0
                      }
                      if (!entity.removeScoreboardTag("$ID:disabled/${effect.id}")) {
                          it.source.sendError(Text.translatable("command.permanent_status_effect.enable.failure", effect.name))
                          return@executes 0
                      }
                      it.source.sendFeedback(Suppliers.ofInstance(Text.translatable("command.permanent_status_effect.enable.succeed", effect.name)), false)
                      1
                  })
              )
            )
        }
        if (Fabric.isModLoaded(TravelersBackpack.MODID)) {
            STORAGE.register { entity, storage ->
                if (!CONFIG.instance().travelerBackpack || entity !is PlayerEntity) return@register
                ComponentUtils.getBackpackInv(entity)?.combinedInventory?.also {
                    for (view in InventoryStorage(it).nonEmptyViews()) {
                        storage += RemainderContainerItemContext(view, entity)
                    }
                }
                //INVENTORIES.register { entity, storages ->
                //    if (!CONFIG.instance().travelerBackpack || entity !is PlayerEntity) return@register
                //    ComponentUtils.getBackpackInv(entity)?.combinedInventory?.also { storages.put(TravelersBackpack.MODID, InventoryStorage(it)) }
                //}
//            ItemStorages.ITEM.fallback.register{item,context->
//                if (item is TravelersBackpackItem) {
//
//                }
//                null
//            }
            }
        }
        if (Fabric.isModLoaded("extraalchemy")) {
            EFFECTS.register { context, entity, stats ->
                if (context.itemVariant.isOf(ModItems.POTION_BAG)) {
                    addEffects(InventoryStorage(PotionBagItem.BagInventory(context.itemVariant.toStack(), null)), entity, stats)
                }
                false
            }
        }
    }
}
