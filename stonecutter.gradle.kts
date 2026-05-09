plugins {
    id("dev.kikugie.stonecutter")
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.loom.obf) apply false
}

stonecutter active "1.21.10"

stonecutter parameters {
    swaps["mod_version"] = "\"" + property("mod.version") + "\""
    swaps["mod_id"] = "\"" + property("mod.id") + "\""
    swaps["mod_name"] = "\"" + property("mod.name") + "\""
    swaps["minecraft"] = "\"" + node.metadata.version + "\""

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        string(current.parsed >= "26.1") {
            replace("ClientCommandManager", "ClientCommands")
            replace("ClickType", "ContainerInput")
            replace("SpecialGuiElementRegistry", "PictureInPictureRendererRegistry")
        }

        string(current.parsed >= "26.1", "!graphics") {
            replace("GuiGraphics", "GuiGraphicsExtractor")
        }
    }
}