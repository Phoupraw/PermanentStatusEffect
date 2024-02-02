package phoupraw.mcmod.permanent_status_effect

import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.controller.ControllerBuilder
import dev.isxander.yacl3.config.v2.api.ConfigField
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.autogen.*
import dev.isxander.yacl3.config.v2.api.autogen.Boolean
import dev.isxander.yacl3.config.v2.api.autogen.ListGroup.ControllerFactory
import dev.isxander.yacl3.config.v2.api.autogen.ListGroup.ValueFactory
import net.minecraft.registry.Registries

class PSEConfig {
    @AutoGen(category = Categories.COMMON)
    @SerialEntry
    @TickBox
    @JvmField
    var enable = true
    @AutoGen(category = Categories.COMMON)
    @SerialEntry
    @IntField(min = 1, max = 64, format = "%d")
    @GameRestart
    @JvmField
    var potionMaxStack = 1
    @AutoGen(category = Categories.COMMON)
    @SerialEntry
    @IntField(min = 1, max = 64, format = "%d")
    @GameRestart
    @JvmField
    var stewMaxStack = 1

    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @DoubleField(format = "%.1f")
    @JvmField
    var duration = 60.0
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var ambient = true
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var showParticles = false
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var showIcon = true
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var harmful = false
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var instant = false
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var mainInventory = true
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var enderChest = true
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @TickBox
    @JvmField
    var travelerBackpack = true
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @Boolean(formatter = Boolean.Formatter.CUSTOM)
    @JvmField
    var whitelist = false
    @AutoGen(category = Categories.SERVER)
    @SerialEntry
    @ListGroup(valueFactory = BlacklistFactory::class, controllerFactory = BlacklistFactory::class)
    @JvmField
    var blacklist = mutableListOf<String>()

    class BlacklistFactory : ValueFactory<String>, ControllerFactory<String> {
        override fun provideNewValue(): String = ""
        override fun createController(annotation: ListGroup, field: ConfigField<MutableList<String>>, storage: OptionAccess, option: Option<String>): ControllerBuilder<String> = RegistryValueController(option, Registries.ITEM).Builder()
    }
    object Categories {
        const val COMMON = "common"
        const val SERVER = "server"
    }
}